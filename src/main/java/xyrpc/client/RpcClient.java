package xyrpc.client;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * rpc client class.
 *
 * @author dingxin
 */
public final class RpcClient implements InvocationHandler {
    private InetSocketAddress cltAddr;
    private Logger logger;

    public RpcClient(int port) {
        cltAddr = new InetSocketAddress(port);
        logger = Logger.getLogger("RpcServer_" + cltAddr.toString());
    }

    /**
     * Returns a proxy instance of the interface class iClazz.
     *
     * @param iClazz an interface class
     * @return T the proxy instance of interface type iClazz
     * @throws java.lang.IllegalArgumentException if iClazz is not of interface type
     */
    public <T> T newProxy(Class<T> iClazz) {
        if (iClazz == null || !iClazz.isInterface())
            throw new IllegalArgumentException("passed in argument should be of interface type");
        return (T)Proxy.newProxyInstance(iClazz.getClassLoader(), new Class[] {iClazz}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    private boolean connectDst(InetSocketAddress dst) {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.bind(cltAddr);
            sc.connect(dst);
        } catch (IOException e) {
            Logger.getLogger("RpcClient").severe("can't connect to remote address" + dst.toString()
            + ", error: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        RpcClient client = new RpcClient(11013);
        client.connectDst(new InetSocketAddress(11021));
    }
}
