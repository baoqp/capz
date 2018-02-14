package com.capz.core.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Capz;
import com.capz.core.Handler;
import com.capz.core.WorkerExecutor;

import java.util.concurrent.ExecutorService;

public class WorkerExecutorImpl implements WorkerExecutor {

    private final Capz capz;

    private final ExecutorService executorService;

    private final boolean releaseOnClose;

    private volatile boolean closed;


    public WorkerExecutorImpl(Capz capz, ExecutorService executorService, boolean releaseOnClose) {
        this.capz = capz;
        this.executorService = executorService;
        this.releaseOnClose = releaseOnClose;
    }


    @Override
    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> asyncResultHandler) {
        if (closed) {
            throw new IllegalStateException("Worker executor closed");
        }
        AbstractContext context = (AbstractContext) capz.getOrCreateContext();
        context.executeBlocking(null, blockingCodeHandler, asyncResultHandler, executorService,
                ordered ? context.orderedTasks : null);
    }


    @Override
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    @Override
    public Capz capz() {
        return capz;
    }

    // TODO
    @Override
    public void close(Handler<AsyncResult<Void>> completionHandler) {

    }
}
