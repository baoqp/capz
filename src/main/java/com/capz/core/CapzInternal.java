package com.capz.core;

import com.capz.core.impl.AbstractContext;
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

    ExecutorService getWorkerPool();

    <T> void executeBlockingInternal(Action<T> action, Handler<AsyncResult<T>> resultHandler);
}
