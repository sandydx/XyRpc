package xyrpc.server;

import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by dingxin on 10/22/16.
 */
public class RequestHandlerPool {
    private HashMap<ReqTypeEnum, Handler[]> handlers;
    private Dispatcher dispatcher;  //dispatcher that dispatch tasks for the handlers
    private Logger logger = Logger.getLogger(this.getClass().getName());

    //FIXME whichHandler is mutable, what to do if it's changed during construction???
    //FIXME what
    RequestHandlerPool(Dispatcher dispatcher, HashMap<ReqTypeEnum, Class<? extends Handler>> whichHandler, int thrNum) {
        if (whichHandler == null) {
            handlers = new HashMap<ReqTypeEnum, Handler[]>();
            return;
        }
        createHandlers(whichHandler, thrNum);
        this.dispatcher = dispatcher;
    }

    RequestHandlerPool(Dispatcher dispatcher, HashMap<ReqTypeEnum, Class<? extends Handler>> whichHandler, int thrNum, EnumSet<ReqTypeEnum> types){
        if (whichHandler == null) {
            handlers = new HashMap<ReqTypeEnum, Handler[]>();
            return;
        }
        EnumSet<ReqTypeEnum> allTypes = EnumSet.allOf(ReqTypeEnum.class);
        if (allTypes.containsAll(types) == false) {
            logger.info("invalid type specified");
            throw new IllegalArgumentException("invalid types");
        }
        HashMap<ReqTypeEnum, Class<? extends Handler>> subSet = new HashMap<ReqTypeEnum, Class<? extends Handler>>();
        for(Map.Entry<ReqTypeEnum, Class<? extends Handler>> entry : whichHandler.entrySet()) {
            if (types.contains(entry.getKey())) {
                subSet.put(entry.getKey(), entry.getValue());
            }
        }
        createHandlers(subSet, thrNum);
        this.dispatcher = dispatcher;
    }

    private void createHandlers(HashMap<ReqTypeEnum, Class<? extends Handler>> whichHandler, int thrNum) {
        int types = whichHandler.size();
        if (thrNum < types) {
            logger.info("Too few threads, changed to " + types);
            thrNum = types;
        }

        handlers = new HashMap<ReqTypeEnum, Handler[]>();
        int handlersNow = 0;
        Iterator<Map.Entry<ReqTypeEnum, Class<? extends Handler>>> iter = whichHandler.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<ReqTypeEnum, Class<? extends Handler>> entry = iter.next();
            int num = thrNum / types;
            if (iter.hasNext() == false) {
                num = thrNum - handlersNow;
            }
            Handler[] handlersThisType = (Handler[])Array.newInstance(entry.getValue(), num);
            try {
                Arrays.fill(handlersThisType, entry.getValue().getConstructor().newInstance());
            } catch (Exception e) {
                //FIXME
                Arrays.fill(handlersThisType, null);
            }
            handlers.put(entry.getKey(), handlersThisType);
            handlersNow += num;
        }
    }

    public Handler getHandler(ReqTypeEnum type, int id) {
        Handler[] handlersThisType = handlers.get(type);
        if (handlersThisType == null) {
            return null;
        }
        return handlersThisType[id % handlersThisType.length];
    }
}
