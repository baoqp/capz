package com.capz.core;

import com.capz.core.impl.Future;
import com.capz.core.impl.TaskQueue;
import io.netty.channel.EventLoop;



public interface ContextInternal extends Context {


    EventLoop nettyEventLoop();

    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, TaskQueue queue, Handler<AsyncResult<T>> resultHandler);

    // void executeFromIO(ContextTask task);
}
