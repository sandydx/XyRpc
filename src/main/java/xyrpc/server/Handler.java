package xyrpc.server;

import java.util.concurrent.Callable;

/**
 * @author dingxin
 */
//TODO
public interface Handler extends Callable{
    public void addTask(Task task);
    //whether this handler has been submitted to a thread yet
    public boolean isRun();
    public void setRun(boolean run);
}
