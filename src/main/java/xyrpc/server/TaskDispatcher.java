package xyrpc.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * An implementation of {@link xyrpc.server.Dispatcher} using Executor service.
 * This class maintains a ExecutorService thread pool of handlers and evenly distributes
 * all requests to matching handlers.
 * Since {@link xyrpc.server.ConnHandler} is long running(not able to create a new instance
 * for each request), all the handler instances are pre-created. And since each instance of
 * this class actually deals with just one request type(refer to {@link xyrpc.server.ReqListener}
 * doc), this class requires a "types" param (so as to create matching handlers).
 *
 * @author dingxin
 */
public class TaskDispatcher implements Dispatcher{
    private ReqListener listener;
    private ExecutorService pool;
    private RequestHandlerPool handlers;
    private HashMap<Integer, ByteBuffer> replies;
    private int idSeed = 0;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final HashMap<ReqTypeEnum, Class<? extends Handler>> whichHandler = new HashMap<ReqTypeEnum, Class<? extends Handler>>();
    static {
        whichHandler.put(ReqTypeEnum.Conn, ConnHandler.class);
        whichHandler.put(ReqTypeEnum.Msg, MsgHandler.class);
    }

    /**
     * @param listener - {@link xyrpc.server.ReqListener} that forwards tasks to this dispatcher
     * @param thrNum - Number of threads for the handler thread pool.
     *               Will be adjusted if it's too small(<=1).
     * @param types - Request types that this instance dispatches
     */
    //FIXME need to check whether this listener
    public TaskDispatcher(ReqListener listener, int thrNum, EnumSet<ReqTypeEnum> types) {
        this.listener = listener;
        int handlerTypeNum = whichHandler.size();

        if (thrNum <= 1) {
            logger.warning("specified thread number " + thrNum + "is too small");
            thrNum = handlerTypeNum;
        }
        pool = Executors.newFixedThreadPool(thrNum);
        handlers = new RequestHandlerPool(this, whichHandler, thrNum, types);

        replies = new HashMap<Integer, ByteBuffer>();
    }

    /**
     *
     * @param req
     */
    @Override
    public void dispatch(AbstractRequest req) {
        //TODO think about maintain connections array , cause the sc in req might be broken. but where to maintain???
        ReqTypeEnum type = req.getType();
        int id = generateId();
        Task task = new Task(id, req);
        Handler handler = handlers.getHandler(type, id);
        if (handler != null) {
            if (task.getReq().requireResponse()) {
                //FIXME change reply type to chain very large response
                ByteBuffer buf = ByteBuffer.allocate(ServerConst.WR_BUF_SZ);
                replies.put(id, buf);
                task.setResultBuf(buf);
            }
            handler.addTask(task);
            if (handler.isRun() == false) {
                pool.submit(handler);
                handler.setRun(true);
            }
        } else {
            logger.info("Ignoring " + type + " request(no matching handler/create handler failed)");
        }
    }

    @Override
    public Object getResult(int id) {
        return replies.get(id);
    }

    public void registerResultWrite(Task task) {
        listener.addEvent(task.getReq().getClientChan(), SelectionKey.OP_WRITE, task.getId());
    }


    //id only needs to be unique within this object instance
    private int generateId() {
        if (idSeed == Integer.MAX_VALUE){
            idSeed = 0;
        }
        return idSeed++;
    }

}
