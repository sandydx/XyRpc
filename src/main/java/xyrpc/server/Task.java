package xyrpc.server;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * Created by dingxin on 10/15/16.
 */
public class Task {
    private AbstractRequest req;
    private int id;
    private ByteBuffer result;

    public Task(int id, AbstractRequest req) {
        this.req = req;
        this.id = id;
        result = null;
    }

    public int getId() {
        return id;
    }

    public AbstractRequest getReq() {
        return req;
    }

    public void setResultBuf(ByteBuffer res) {
        if (req.requireResponse() == true)
            result = res;
    }

    public ByteBuffer getResultBuf() {
        return result;
    }
}
