package xyrpc.server;

/**
 * An interface to dispatch requests.
 * Implementation classes can specify which handler to use for different request types,
 * and how to run each handler(i.e. one thread per request, or maintain thread pool, etc.)
 *
 * @author dingxin
 */
public interface Dispatcher {
    /**
     * @param req - request to dispatch
     */
    public void dispatch(AbstractRequest req);

    /**
     * Get the processed reply for a task using task id.
     * This method is intended to be used by a {@link xyrpc.server.ReqListener} to fetch the result
     * for a task it previously dispatched to perform the write IO (to the client).
     *
     * @param id - Task id
     * @return The reply to the task
     */
    public Object getResult(int id);
}
