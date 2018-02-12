package com.capz.core;

import io.netty.channel.EventLoopGroup;

import java.util.concurrent.ExecutorService;

/**
 * @author Bao Qingping
 */
public interface CapzInternal extends Capz {

    EventLoopGroup getEventLoopGroup();

    EventLoopGroup getAcceptorEventLoopGroup();

    ExecutorService getWorkerPool();

    <T> void executeBlockingInternal(Action<T> action, Handler<AsyncResult<T>> resultHandler);
}
