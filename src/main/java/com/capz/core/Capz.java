package com.capz.core;

import com.capz.core.eventbus.EventBus;
import com.capz.core.http.HttpServer;
import com.capz.core.impl.CapzFactory;
import com.capz.core.impl.Future;
import io.netty.channel.EventLoopGroup;


public interface Capz {

    Context getOrCreateContext();

    // TODO
    static Context currentContext() {
        return factory.context();
    }

    EventBus eventBus();


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler);


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);


    long setTimer(long delay, Handler<Long> handler);


    long setPeriodic(long delay, Handler<Long> handler);

    boolean cancelTimer(long id);


    void runOnContext(Handler<Void> action);

    void close();

    void close(Handler<AsyncResult<Void>> completionHandler);


    Handler<Throwable> exceptionHandler();

    CapzFactory factory = new CapzFactory();


    EventLoopGroup nettyEventLoopGroup();

    // 创建线程池
    WorkerExecutor createSharedWorkerExecutor(String name);

    WorkerExecutor createSharedWorkerExecutor(String name, int poolSize);

    WorkerExecutor createSharedWorkerExecutor(String name, int poolSize, long maxExecuteTime);


    HttpServer createHttpServer();
}
