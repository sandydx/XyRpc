package xyrpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * rpc server class
 *
 * @author dingxin
 */
public final class RpcServer {
    private InetSocketAddress srvAddr;
    private Logger logger;
    private ExecutorService pool;

    public RpcServer(int port) {
        srvAddr = new InetSocketAddress(port);
        pool = Executors.newFixedThreadPool(ServerConst.POOL_THRS_NUM);
        logger = Logger.getLogger("RpcServer_" + srvAddr.toString());
    }

    public void serveLoop() {
        ServerSocketChannel ssc = null;
        Selector selector = null;
        try {
            ssc = ServerSocketChannel.open();
            ssc.bind(srvAddr);
            selector = Selector.open();
            ssc.configureBlocking(false);
            SelectionKey acKey = ssc.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            logger.severe("Server listen failed: " + e.getMessage());
        }
        //select and dispatch
        while (selector != null) {
            int readyNum = 0;
            try {
                readyNum = selector.select();
            } catch (IOException e) {

            }
            if (readyNum != 0) {
                Set readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = readyKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey rkey = iter.next();

                    if (rkey.isAcceptable()) {  //new connection
                        try {
                            SocketChannel sc = ssc.accept();
                            logger.info("client " + sc.getRemoteAddress().toString() + "connected");
                            sc.register(selector, SelectionKey.OP_READ);
                        } catch (IOException e) {
                            logger.info("Accept failed");
                        }
                    } else if (rkey.isReadable()) { //new message

                    }
                    iter.remove();
                }
            }   //end of if(readyNum != 0)
        }   //end of while(selector != null)
    }   //end of serveLoop()

    public static void main(String[] args) {
        RpcServer server = new RpcServer(11021);
        server.serveLoop();
    }

}   //end of class

