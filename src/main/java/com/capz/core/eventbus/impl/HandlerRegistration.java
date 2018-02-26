package com.capz.core.eventbus.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Capz;
import com.capz.core.Context;
import com.capz.core.Exception.ReplyException;
import com.capz.core.Handler;
import com.capz.core.eventbus.Message;
import com.capz.core.eventbus.MessageConsumer;
import com.capz.core.eventbus.ReplyFailure;
import com.capz.core.impl.Future;
import com.capz.core.streams.ReadStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;


// 在地址address上注册handler，在消费message的时候进行调用
public class HandlerRegistration<T> implements MessageConsumer<T>, Handler<Message<T>> {

    private static final Logger log = LoggerFactory.getLogger(HandlerRegistration.class);
    public static final int DEFAULT_MAX_BUFFERED_MESSAGES = 1000;

    private final Capz capz;
    private final EventBusImpl eventBus;
    private final String address;
    private final String repliedAddress;
    private final boolean localOnly;
    private final Handler<AsyncResult<Message<T>>> asyncResultHandler;
    private long timeoutID = -1;
    private boolean registered;
    private Handler<Message<T>> handler;
    private Context handlerContext;
    private AsyncResult<Void> result;
    private Handler<AsyncResult<Void>> completionHandler;
    private Handler<Void> endHandler;
    private Handler<Message<T>> discardHandler;
    private int maxBufferedMessages = DEFAULT_MAX_BUFFERED_MESSAGES;
    private final Queue<Message<T>> pending = new ArrayDeque<>(8);
    private boolean paused;
    private Object metric;

    public HandlerRegistration(Capz capz, EventBusImpl eventBus, String address,
                               String repliedAddress, boolean localOnly,
                               Handler<AsyncResult<Message<T>>> asyncResultHandler, long timeout) {
        this.capz = capz;
        this.eventBus = eventBus;
        this.address = address;
        this.repliedAddress = repliedAddress;
        this.localOnly = localOnly;
        this.asyncResultHandler = asyncResultHandler;
        if (timeout != -1) {
            timeoutID = capz.setTimer(timeout, tid -> {
                sendAsyncResultFailure(ReplyFailure.TIMEOUT,
                        "Timed out after waiting " + timeout + "(ms) for a reply. address: " + address + ", repliedAddress: " + repliedAddress);
            });
        }
    }

    @Override
    public synchronized MessageConsumer<T> setMaxBufferedMessages(int maxBufferedMessages) {
        assert maxBufferedMessages > 0;
        while (pending.size() > maxBufferedMessages) {
            pending.poll();
        }
        this.maxBufferedMessages = maxBufferedMessages;
        return this;
    }

    @Override
    public synchronized int getMaxBufferedMessages() {
        return maxBufferedMessages;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public synchronized void completionHandler(Handler<AsyncResult<Void>> completionHandler) {
        Objects.requireNonNull(completionHandler);
        if (result != null) {
            AsyncResult<Void> value = result;
            capz.runOnContext(v -> completionHandler.handle(value));
        } else {
            this.completionHandler = completionHandler;
        }
    }

    @Override
    public synchronized void unregister() {
        unregister(false);
    }

    @Override
    public synchronized void unregister(Handler<AsyncResult<Void>> completionHandler) {
        Objects.requireNonNull(completionHandler);
        doUnregister(completionHandler, false);
    }

    public void unregister(boolean callEndHandler) {
        doUnregister(null, callEndHandler);
    }

    public void sendAsyncResultFailure(ReplyFailure failure, String msg) {
        unregister();
        asyncResultHandler.handle(Future.failedFuture(new ReplyException(failure, msg)));
    }

    // TODO ???
    private void doUnregister(Handler<AsyncResult<Void>> completionHandler, boolean callEndHandler) {
        // 取消定时任务
        if (timeoutID != -1) {
            capz.cancelTimer(timeoutID);
        }

        if (endHandler != null && callEndHandler) {
            Handler<Void> theEndHandler = endHandler;
            Handler<AsyncResult<Void>> handler = completionHandler;
            completionHandler = ar -> {
                theEndHandler.handle(null);
                if (handler != null) {
                    handler.handle(ar);
                }
            };
        }
        if (registered) {
            registered = false;
            eventBus.removeRegistration(address, this, completionHandler);
        } else {
            callCompletionHandlerAsync(completionHandler);
        }
    }

    private void callCompletionHandlerAsync(Handler<AsyncResult<Void>> completionHandler) {
        if (completionHandler != null) {
            capz.runOnContext(v -> completionHandler.handle(Future.succeededFuture()));
        }
    }

    synchronized void setHandlerContext(Context context) {
        handlerContext = context;
    }

    public synchronized void setResult(AsyncResult<Void> result) {
        this.result = result;
        if (completionHandler != null) {
            Handler<AsyncResult<Void>> callback = completionHandler;
            capz.runOnContext(v -> callback.handle(result));
        } else if (result.failed()) {
            log.error("Failed to propagate registration for handler " + handler + " and address " + address);
        }
    }

    @Override
    public void handle(Message<T> message) {
        Handler<Message<T>> theHandler;
        synchronized (this) {
            if (paused) {
                if (pending.size() < maxBufferedMessages) {
                    pending.add(message);
                } else {
                    if (discardHandler != null) {
                        discardHandler.handle(message);
                    } else {
                        log.warn("Discarding message as more than " + maxBufferedMessages + " buffered in paused consumer. address: " + address);
                    }
                }
                return;
            } else {
                if (pending.size() > 0) {
                    pending.add(message);
                    message = pending.poll();
                }
                theHandler = handler;
            }
        }
        deliver(theHandler, message);
    }

    private void deliver(Handler<Message<T>> theHandler, Message<T> message) {

        checkNextTick();
        boolean local = true;

        /*String creditsAddress = message.headers().get(MessageProducerImpl.CREDIT_ADDRESS_HEADER_NAME);
        if (creditsAddress != null) {
            eventBus.send(creditsAddress, 1);
        }*/
        try {
            theHandler.handle(message);
        } catch (Exception e) {
            log.error("Failed to handleMessage. address: " + message.address(), e);
            throw e;
        }
    }

    private synchronized void checkNextTick() {
        // Check if there are more pending messages in the queue that can be processed next time around
        if (!pending.isEmpty()) {
            handlerContext.runOnContext(v -> {
                Message<T> message;
                Handler<Message<T>> theHandler;
                synchronized (HandlerRegistration.this) {
                    if (paused || (message = pending.poll()) == null) {
                        return;
                    }
                    theHandler = handler;
                }
                deliver(theHandler, message);
            });
        }
    }


    public synchronized void discardHandler(Handler<Message<T>> handler) {
        this.discardHandler = handler;
    }

    @Override
    public synchronized MessageConsumer<T> handler(Handler<Message<T>> handler) {
        this.handler = handler;
        if (this.handler != null && !registered) {
            registered = true;
            eventBus.addRegistration(address, this, repliedAddress != null, localOnly);
        } else if (this.handler == null && registered) {
            // This will set registered to false
            this.unregister();
        }
        return this;
    }

    @Override
    public ReadStream<T> bodyStream() {
        return null;
        //return new BodyReadStream<>(this);
    }

    @Override
    public synchronized boolean isRegistered() {
        return registered;
    }

    @Override
    public synchronized MessageConsumer<T> pause() {
        if (!paused) {
            paused = true;
        }
        return this;
    }

    @Override
    public synchronized MessageConsumer<T> resume() {
        if (paused) {
            paused = false;
            checkNextTick();
        }
        return this;
    }

    @Override
    public synchronized MessageConsumer<T> endHandler(Handler<Void> endHandler) {
        if (endHandler != null) {
            // We should use the HandlerHolder context to properly do this (needs small refactoring)
            Context endCtx = capz.getOrCreateContext();
            this.endHandler = v1 -> endCtx.runOnContext(v2 -> endHandler.handle(null));
        } else {
            this.endHandler = null;
        }
        return this;
    }

    @Override
    public synchronized MessageConsumer<T> exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    public Handler<Message<T>> getHandler() {
        return handler;
    }

    public Object getMetric() {
        return metric;
    }

}
