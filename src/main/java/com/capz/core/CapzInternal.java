package com.capz.core;

import com.capz.core.http.impl.HttpServerImpl;
import com.capz.core.impl.AbstractContext;
import com.capz.core.impl.EventLoopContext;
import com.capz.core.net.impl.ServerID;
import io.netty.channel.EventLoopGroup;

import java.util.Map;

/**
 * @author Bao Qingping
 */
public interface CapzInternal extends Capz {

    @Override
    AbstractContext getOrCreateContext();

    AbstractContext getContext();

    EventLoopGroup getEventLoopGroup();

    EventLoopGroup getAcceptorEventLoopGroup();

    WorkerExecutor getWorkerPool();

    <T> void executeBlockingInternal(Action<T> action, Handler<AsyncResult<T>> resultHandler);

    AbstractContext createWorkerContext(boolean multiThreaded, String deploymentID,
                                        WorkerExecutor pool, ClassLoader tccl);

    EventLoopContext createEventLoopContext(String deploymentID, WorkerExecutor workerPool,
                                            ClassLoader tccl);


    Map<ServerID, HttpServerImpl> sharedHttpServers();

}
