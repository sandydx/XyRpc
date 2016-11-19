package xyrpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.EnumSet;
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
    private ReqListener listener;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public RpcServer(int port) {
        srvAddr = new InetSocketAddress(port);
        try {
            listener = new ReqListener(srvAddr, ServerConst.POOL_THRS_NUM / 2, EnumSet.of(ReqTypeEnum.Conn));
        } catch (IOException e) {
            logger.severe("Create RpcServer failed");
        }
    }

    public void startServer() {
        listener.listenLoop();
    }

    public static void main(String[] args) {
        RpcServer server = new RpcServer(11021);
        server.startServer();
    }

}   //end of class

