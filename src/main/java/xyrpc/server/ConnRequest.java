package xyrpc.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by dingxin on 10/15/16.
 */
public class ConnRequest extends AbstractRequest {
    private SocketChannel sc;

    public ConnRequest(SocketChannel sc) {
        super(ReqTypeEnum.Conn);
        this.sc = sc;
    }

    public SocketChannel getSockChann() {
        return sc;
    }

    @Override
    public boolean requireResponse() {
        return false;
    }

    @Override
    public SelectableChannel getClientChan() {
        return sc;
    }
}
