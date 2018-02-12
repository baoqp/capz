package com.capz.core.impl;

import com.capz.core.CapzInternal;
import com.capz.core.Handler;

import java.util.concurrent.ExecutorService;

public class MultiThreadedWorkerContext extends WorkerContext {

    public MultiThreadedWorkerContext(CapzInternal vertx, ExecutorService internalBlockingPool, ExecutorService workerPool,
                                      String deploymentID, ClassLoader tccl) {
        super(vertx, internalBlockingPool, workerPool, deploymentID, tccl);
    }

    @Override
    public void executeAsync(Handler<Void> task) {
        workerPool.execute(wrapTask(null, task, false));
    }

    @Override
    public boolean isMultiThreadedWorkerContext() {
        return true;
    }
}