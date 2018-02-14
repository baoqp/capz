package com.capz.core.impl;

import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.WorkerExecutor;

import java.util.concurrent.ExecutorService;

public class MultiThreadedWorkerContext extends WorkerContext {

    public MultiThreadedWorkerContext(CapzInternal vertx, WorkerExecutor internalBlockingPool, WorkerExecutor workerPool,
                                      String deploymentID, ClassLoader tccl) {
        super(vertx, internalBlockingPool, workerPool, deploymentID, tccl);
    }

    @Override
    public void executeAsync(Handler<Void> task) {
        workerPool.getExecutorService().execute(wrapTask(null, task, false));
    }

    @Override
    public boolean isMultiThreadedWorkerContext() {
        return true;
    }
}