package xyrpc.server;

import java.nio.channels.SelectableChannel;

/**
 * Created by dingxin on 10/15/16.
 */
public abstract class AbstractRequest {
    private ReqTypeEnum type;

    public AbstractRequest(ReqTypeEnum type) {
        this.type = type;
    }

    public ReqTypeEnum getType() {
        return type;
    }

    public abstract boolean requireResponse();

    //FIXME add a new subclass requestrequiresresponse and move this method
    public abstract SelectableChannel getClientChan();
}
