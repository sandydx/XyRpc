package xyrpc.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author dingxin
 */
public class ConnHandler implements Handler{
    //FIXME
    private ReqListener listener;
    private boolean isRun = false;

    //TODO
    public ConnHandler(SelectableChannel chan) {

    }

    //TODO remove?
    public ConnHandler(AbstractRequest req) {

    }

    public ConnHandler() {

    }
    /**
     *
     * @param task
     */
    @Override
    public void addTask(Task task) {
        SocketChannel sc = ((ConnRequest)task.getReq()).getSockChann();
        //FIXME
        int op = SelectionKey.OP_READ;
        listener.addEvent(sc, op);
    }

    @Override
    public boolean isRun() {
        return isRun;
    }

    @Override
    public void setRun(boolean run) {
        isRun = true;
    }

    //TODO add sleep when there is no work to do
    @Override
    public Object call() throws Exception {
        listener.listenLoop();
        return null;
    }
}
