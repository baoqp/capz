package com.capz.core.impl;

import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

public class EventLoopContext extends AbstractContext {

    private static final Logger log = LoggerFactory.getLogger(EventLoopContext.class);

    public EventLoopContext(CapzInternal capzInternal, ExecutorService internalBlockingPool, ExecutorService workerPool,
                            String deploymentID, ClassLoader tccl) {
        super(capzInternal, internalBlockingPool, workerPool, deploymentID, tccl);
    }

    public EventLoopContext(CapzInternal capzInternal, EventLoop eventLoop, ExecutorService internalBlockingPool,
                            ExecutorService workerPool, String deploymentID, ClassLoader tccl) {
        super(capzInternal, eventLoop, internalBlockingPool, workerPool, deploymentID, tccl);
    }

    public void executeAsync(Handler<Void> task) {
        // No metrics, we are on the event loop.
        nettyEventLoop().execute(wrapTask(null, task, true));
    }

    @Override
    public boolean isEventLoopContext() {
        return true;
    }

    @Override
    public boolean isMultiThreadedWorkerContext() {
        return false;
    }

    @Override
    protected void checkCorrectThread() {
        Thread current = Thread.currentThread();
        if (!(current instanceof CapzThread)) {
            throw new IllegalStateException("Expected to be on Vert.x thread, but actually on: " + current);
        } else if (contextThread != null && current != contextThread) {
            throw new IllegalStateException("Event delivered on unexpected thread " + current + " expected: " + contextThread);
        }
    }

}