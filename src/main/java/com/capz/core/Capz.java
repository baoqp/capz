package com.capz.core;

import com.capz.core.eventbus.EventBus;
import com.capz.core.impl.Future;
import io.netty.channel.EventLoopGroup;

/**
 * Capz -> capsule
 *
 * @author Bao Qingping
 */
public interface Capz {

    Context getOrCreateContext();

    // TODO
    static Context currentContext() {
        return null;
    }

    EventBus eventBus();


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler);


    <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);

    /**
     * Set a one-shot timer to fire after delay milliseconds, at which point handler will be called
     */
    long setTimer(long delay, Handler<Long> handler);

    /**
     * Set a periodic timer to fire every delay milliseconds, at which point handler will be called
     */
    long setPeriodic(long delay, Handler<Long> handler);

    boolean cancelTimer(long id);

    /**
     * Puts the handler on the event queue for the current context so it will be run asynchronously ASAP after all
     * preceeding events have been handled.
     */
    void runOnContext(Handler<Void> action);

    void close();

    void close(Handler<AsyncResult<Void>> completionHandler);


    EventLoopGroup nettyEventLoopGroup();

    Handler<Throwable> exceptionHandler();
}
