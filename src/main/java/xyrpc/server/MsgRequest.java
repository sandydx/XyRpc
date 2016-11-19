package xyrpc.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by dingxin on 10/15/16.
 */
public class MsgRequest extends AbstractRequest {
    private SocketChannel sc;
    private ByteBuffer buf;

    public MsgRequest(SocketChannel sc, ByteBuffer buf) {
        super(ReqTypeEnum.Msg);
        this.sc = sc;
        this.buf = buf;
    }

    public SocketChannel getSockChann() {
        return sc;
    }

    public ByteBuffer getBuf() {
        return buf;
    }

    @Override
    public boolean requireResponse() {
        return true;
    }

    @Override
    public SelectableChannel getClientChan() {
        return sc;
    }
}
