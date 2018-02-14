package com.capz.core;

import com.capz.core.impl.AbstractContext;
import com.capz.core.impl.EventLoopContext;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.ExecutorService;

/**
 * @author Bao Qingping
 */
public interface CapzInternal extends Capz {

    @Override
    AbstractContext getOrCreateContext();

    EventLoopGroup getEventLoopGroup();

    EventLoopGroup getAcceptorEventLoopGroup();

    WorkerExecutor getWorkerPool();

    <T> void executeBlockingInternal(Action<T> action, Handler<AsyncResult<T>> resultHandler);

    AbstractContext createWorkerContext(boolean multiThreaded, String deploymentID,
                                        WorkerExecutor pool, ClassLoader tccl);

    EventLoopContext createEventLoopContext(String deploymentID, WorkerExecutor workerPool,
                                            ClassLoader tccl);

}
