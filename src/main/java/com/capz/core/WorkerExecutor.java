package com.capz.core;

import com.capz.core.impl.Future;

import java.util.concurrent.ExecutorService;

public interface WorkerExecutor extends Closeable {

    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered,
                             Handler<AsyncResult<T>> resultHandler);


    default <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler,
                                     Handler<AsyncResult<T>> resultHandler) {
        executeBlocking(blockingCodeHandler, true, resultHandler);
    }


    Capz capz();

    ExecutorService getExecutorService();


}
