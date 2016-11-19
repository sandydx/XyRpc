package xyrpc.server;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dingxin
 */
public class MsgHandler implements Handler {
    //FIXME check whether to use blocking queue & whether to use bytebuffer
    //FIXME so the msg is queued at each handler, a good design??
    private BlockingDeque<ByteBuffer> msgQue;
    //TODO
    private Dispatcher dispatcher;

    //FIXME OK to just use atomic?
    private AtomicBoolean isRun = new AtomicBoolean(false);

    //TODO add dispatcher argument(for a new requireresponsehandler type)
    public MsgHandler() {
        msgQue = new LinkedBlockingDeque<ByteBuffer>();
    }

    //TODO remove?
    public MsgHandler(AbstractRequest req) {

    }

    /**
     *
     * @param task //FIXME
     */
    @Override
    public void addTask(Task task) {
        MsgRequest req = (MsgRequest)task.getReq();
        msgQue.add(req.getBuf());
    }

    @Override
    public boolean isRun() {
        return isRun.get();
    }

    @Override
    public void setRun(boolean run) {
        isRun.set(run);
    }

    //TODO add sleep when there is no work to do
    @Override
    public Object call() throws Exception {
        //get a task from the queue
        //parsing the task
        //call the right (registered service to process the message)
        //fill the task's result buf
        //register the write event through the dispatcher (which in turn through its Listener//TODO FIXME)
        return null;
    }
}
