package com.capz.core;

import java.util.List;

// 上下文
public interface Context {


    static boolean isOnWorkerThread() {
        return ContextImpl.isOnWorkerThread();
    }


    static boolean isOnEventLoopThread() {
        return ContextImpl.isOnEventLoopThread();
    }


    static boolean isOnCapzThread() {
        return ContextImpl.isOnVertxThread();
    }


    void runOnContext(Handler<Void> action);


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler);


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);


    String deploymentID();


    List<String> processArgs();


    boolean isEventLoopContext();


    boolean isWorkerContext();


    boolean isMultiThreadedWorkerContext();


    <T> T get(String key);


    void put(String key, Object value);


    boolean remove(String key);


    Capz owner();


    int getInstanceCount();


    Context exceptionHandler(Handler<Throwable> handler);


    void addCloseHook(Closeable hook);


    void removeCloseHook(Closeable hook);

}