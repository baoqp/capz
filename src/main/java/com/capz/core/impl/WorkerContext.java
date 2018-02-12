package com.capz.core.impl;

import com.capz.core.CapzInternal;
import com.capz.core.ContextTask;
import com.capz.core.Handler;

import java.util.concurrent.ExecutorService;

public class WorkerContext extends AbstractContext {

    public WorkerContext(CapzInternal capzInternal, ExecutorService internalBlockingPool, ExecutorService workerPool, String deploymentID,
                         ClassLoader tccl) {
        super(capzInternal, internalBlockingPool, workerPool, deploymentID, tccl);
    }

    @Override
    public void executeAsync(Handler<Void> task) {
        orderedTasks.execute(wrapTask(null, task, true), workerPool);
    }

    @Override
    public boolean isEventLoopContext() {
        return false;
    }

    @Override
    public boolean isMultiThreadedWorkerContext() {
        return false;
    }

    @Override
    protected void checkCorrectThread() {
        // NOOP
    }

    // In the case of a worker context, the IO will always be provided on an event loop thread, not a worker thread
    // so we need to execute it on the worker thread
    @Override
    public void executeFromIO(ContextTask task) {
        orderedTasks.execute(wrapTask(task, null, true), workerPool);
    }

}