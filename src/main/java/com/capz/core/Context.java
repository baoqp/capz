package com.capz.core;

import com.capz.core.impl.AbstractContext;
import com.capz.core.impl.Future;

import java.util.List;


// 上下文
public interface Context {


    static boolean isOnWorkerThread() {
        return AbstractContext.isOnWorkerThread();
    }


    static boolean isOnEventLoopThread() {
        return AbstractContext.isOnEventLoopThread();
    }


    static boolean isOnCapzThread() {
        return AbstractContext.isOnCapzThread();
    }


    void runOnContext(Handler<Void> action);


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler);


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);


    String deploymentID();


    //List<String> processArgs();


    boolean isEventLoopContext();


    boolean isWorkerContext();


    boolean isMultiThreadedWorkerContext();


    <T> T get(String key);


    void put(String key, Object value);


    boolean remove(String key);


    Capz owner();


    Context exceptionHandler(Handler<Throwable> handler);


/*    void addCloseHook(Closeable hook);


    void removeCloseHook(Closeable hook);*/

}