package xyrpc.server;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Listener class that accepts client connection and message(rpc) requests and do network IOs.
 *
 * This class maintains a {@link xyrpc.server.TaskDispatcher} and forwards each requests it accepted
 * to the dispatcher, which will assign the task to a proper {@link xyrpc.server.Handler} and run the
 * handler in the thread pool it(dispatcher) maintained.
 * The (thread) work pattern of xyrpc server is as follows:
 *                                                                                      rep1              rep1
 *                                                                                  | <-----  Worker     <-----   Msg
 *                                                                                  | -----> Dispatcher1 -----> Handler1
 *                                                                                  |   msg1              msg1
 *                                                                                  |
 *                                                                                  |
 *                                                                                  |         rep1(WR)
 * Client1 |                                              |   Conn   |        |  Worker   | ----------> |
 *         |                                              | Handler1 |        | Listener1 | <---------- | Client1
 *         |  conn1    Boss     sc1(AC)     Boss     sc1  |          |  sc1   |           |   msg1(RD)  |
 *         | -------> Listener --------> Dispatcher ----> |          | -----> |           |
 *         |  conn2             sc2(AC)              sc2  |          |  sc2   |           |   msg2(RD)  |
 *         |                                              |   Conn   |        |  Worker   | <---------- | Client2
 * Client2 |                                              | Handler2 |        | Listener2 | ----------> |
 *                                                                                  |         rep2(WR)
 *                                                                                  |
 *                                                                                  |
 *                                                                                  |  msg2               msg2
 *                                                                                  | ----->  Worker     ----->   Msg
 *                                                                                  | <----- Dispatcher2 <----- Handler2
 *                                                                                     rep2               rep2
 *
 * One boss listener(ReqListener unless specified) thread listens on the ServerSocketChannel for all
 * client connections and perform accept() IO. All the connections(SocketChannels) will be evenly split
 * (by boss dispatcher maintained by the boss listener) among the ConnHandlers pool. Each ConnHandler
 * maintains also a worker listener which listens on all the SocketChannels for messages and perform
 * the read() IO. All the message requests will be evenly split (by the worker dispatcher maintained
 * by worker listener) among the MsgHandler pool. When MsgHandler finishes its processing, it will
 * (usually) produce a result. It will register the result to its dispatcher, which will in turn register
 * it to its worker ReqListener to do the write() IO.
 * So although ReqListener is designed to listen for both connections and messages, actually each instance
 * of it just deals with one kind: boss listener with connections and worker listener(maintained by
 * ConnHandlers) with messages. And so is TaskDispatcher since it's contained by ReqListeners.
 *
 * //TODO maintain long connections (when to close to prevent malicious clients ??)
 *
 * @author dingxin
 */
public final class ReqListener {

    private Selector selector;
    private Dispatcher dispatcher;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     *
     * @param thrNum - number of threads to process requests accepted by this listener.
     * @param types - request types that this listener accept.
     * @throws IOException - if listen on the specified address failed.
     */
    public ReqListener(InetSocketAddress addr, int thrNum, EnumSet types) throws IOException {
        try {
            selector = Selector.open();
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(addr);
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new IOException("Create ReqListener failed");
        }
        logger.info("start listening on address " + addr.toString());
        dispatcher = new TaskDispatcher(this, thrNum, types);
    }

    /**
     *
     * @param chan
     * @param op
     * @return
     */
    public boolean addEvent(SelectableChannel chan, int op) {
        try {
            chan.register(selector, op);
        } catch (IOException e) {
            return  false;
        }
        return true;
    }

    /**
     * Add a event on some channel to listen for this listener.
     * Each (client) socket channel will register multiple events:
     * OP_READ(RD for short) for messages and OP_WRITE(WR for short)
     * for writing responses. Xyrpc maintains long connections, so
     * RD is registered once and effective all the time(never cancelled
     * and re-registered). But WR may be added many times since xyrpc
     * support async calls(second response can be ready before the first
     * one is written out). We use ArrayList to accumulate all the WR
     * attachments so that previous ids won't be overridden and forgotten.
     *
     * @param channel
     * @param op - event type, can be SelectionKey.OP_XXX
     * @param attach - attachment, can be null
     * @return A boolean that indicates add successful or not
     * //FIXME change op to ops (an int can indicate multiple ops)
     */
    public boolean addEvent(SelectableChannel channel, int op, Object attach) {
        if (channel == null)
            return false;

        ArrayList toAttach = null;
        if (attach != null) {
            assert((op & SelectionKey.OP_WRITE) != 0);
            toAttach = new ArrayList();
            toAttach.add(attach);
        }

        SelectionKey key = channel.keyFor(selector);
        if (key != null) {  //channel already registered in selector
            ArrayList attached = (ArrayList)key.attachment();
            if (attached != null) {     //already have attachment
                toAttach.addAll(attached);  //should be ok since attached shouldn't be big
            }
        }
        try {
            channel.register(selector, op, toAttach);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Start the main listening loop.
     * Listen for registered events, perform the IO and then pass the requests if necessary.
     */
    public void listenLoop() {
        while (selector != null) {
            int readyNum = 0;
            try {
                readyNum = selector.select();
            } catch (IOException e) {
                logger.severe("Select failed, msg: " + e.getMessage());
            }
            if (readyNum != 0) {
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = readyKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    SelectableChannel chan = key.channel();
                    try {
                        if (key.isAcceptable()) {
                            SocketChannel sock = ((ServerSocketChannel)chan).accept();
                            sock.configureBlocking(false);  //don't forget!!
                            dispatcher.dispatch(new ConnRequest(sock));
                        } else if (key.isReadable()) {
                            ByteBuffer buf = ByteBuffer.allocate(ServerConst.RD_BUF_SZ);
                            SocketChannel sock = (SocketChannel)chan;
                            sock.read(buf);
                            dispatcher.dispatch(new MsgRequest(sock, buf));
                        } else if (key.isWritable()) {
                            SocketChannel sock = (SocketChannel)chan;
                            //TODO attachment should be synchronzied
                            ArrayList<Integer> readyIds = (ArrayList<Integer>)key.attachment();

                            for (int id : readyIds) {
                                ByteBuffer buf = (ByteBuffer)dispatcher.getResult(id);
                                assert(buf != null);
                                //FIXME deals with write exception, in which case the id shouldn't be removed
                                sock.write(buf);
                            }
                            readyIds.clear();
                            int ops = key.interestOps() & ~SelectionKey.OP_WRITE;
                            key.interestOps(ops);
                        }
                        iter.remove();
                    } catch (IOException e) {
                        logger.warning("Client IO failed, msg: " + e.getMessage());
                    }

                }
            }
        }
    }
}
