package com.capz.core.eventbus.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Capz;
import com.capz.core.CapzInternal;
import com.capz.core.Closeable;
import com.capz.core.Context;
import com.capz.core.Exception.ReplyException;
import com.capz.core.Handler;
import com.capz.core.eventbus.DeliveryOptions;
import com.capz.core.eventbus.EventBus;
import com.capz.core.eventbus.Message;
import com.capz.core.eventbus.MessageCodec;
import com.capz.core.eventbus.MessageConsumer;
import com.capz.core.eventbus.MessageProducer;
import com.capz.core.eventbus.ReplyFailure;
import com.capz.core.eventbus.SendContext;
import com.capz.core.impl.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Bao Qingping
 */
public class EventBusImpl implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBusImpl.class);

    private final List<Handler<SendContext>> interceptors = new CopyOnWriteArrayList<>();
    private final AtomicLong replySequence = new AtomicLong(0);

    protected final ConcurrentMap<String, Handlers> handlerMap = new ConcurrentHashMap<>();
    protected final CodecManager codecManager = new CodecManager();
    protected volatile boolean started;
    protected final CapzInternal capzInternal;

    public EventBusImpl(CapzInternal capzInternal) {
        this.capzInternal = capzInternal;
    }

    @Override
    public EventBus addInterceptor(Handler<SendContext> interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    @Override
    public EventBus removeInterceptor(Handler<SendContext> interceptor) {
        interceptors.remove(interceptor);
        return this;
    }

    public synchronized void start(Handler<AsyncResult<Void>> completionHandler) {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        started = true;
        completionHandler.handle(Future.succeededFuture());
    }

    @Override
    public EventBus send(String address, Object message) {
        return send(address, message, new DeliveryOptions(), null);
    }

    @Override
    public <T> EventBus send(String address, Object message, Handler<AsyncResult<Message<T>>> replyHandler) {
        return send(address, message, new DeliveryOptions(), replyHandler);
    }

    @Override
    public EventBus send(String address, Object message, DeliveryOptions options) {
        return send(address, message, options, null);
    }

    @Override
    public <T> EventBus send(String address, Object message, DeliveryOptions options,
                             Handler<AsyncResult<Message<T>>> replyHandler) {
        sendOrPubInternal(createMessage(true, address, options.getHeaders(), message, options.getCodecName()), options, replyHandler);
        return this;
    }

    @Override
    public <T> MessageProducer<T> sender(String address) {
        Objects.requireNonNull(address, "address");
        return new MessageProducerImpl<>(capzInternal, address, true, new DeliveryOptions());
    }

    @Override
    public <T> MessageProducer<T> sender(String address, DeliveryOptions options) {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(options, "options");
        return new MessageProducerImpl<>(capzInternal, address, true, options);
    }

    @Override
    public <T> MessageProducer<T> publisher(String address) {
        Objects.requireNonNull(address, "address");
        return new MessageProducerImpl<>(capzInternal, address, false, new DeliveryOptions());
    }

    @Override
    public <T> MessageProducer<T> publisher(String address, DeliveryOptions options) {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(options, "options");
        return new MessageProducerImpl<>(capzInternal, address, false, options);
    }

    @Override
    public EventBus publish(String address, Object message) {
        return publish(address, message, new DeliveryOptions());
    }

    @Override
    public EventBus publish(String address, Object message, DeliveryOptions options) {
        sendOrPubInternal(createMessage(false, address, options.getHeaders(), message, options.getCodecName()), options, null);
        return this;
    }

    @Override
    public <T> MessageConsumer<T> consumer(String address) {
        checkStarted();
        Objects.requireNonNull(address, "address");
        return new HandlerRegistration<>(capzInternal, this, address, null, false, null, -1);
    }

    @Override
    public <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler) {
        Objects.requireNonNull(handler, "handler");
        MessageConsumer<T> consumer = consumer(address);
        consumer.handler(handler);
        return consumer;
    }

    @Override
    public <T> MessageConsumer<T> localConsumer(String address) {
        checkStarted();
        Objects.requireNonNull(address, "address");
        return new HandlerRegistration<>(capzInternal, this, address, null, true, null, -1);
    }

    @Override
    public <T> MessageConsumer<T> localConsumer(String address, Handler<Message<T>> handler) {
        Objects.requireNonNull(handler, "handler");
        MessageConsumer<T> consumer = localConsumer(address);
        consumer.handler(handler);
        return consumer;
    }

    @Override
    public EventBus registerCodec(MessageCodec codec) {
        codecManager.registerCodec(codec);
        return this;
    }

    @Override
    public EventBus unregisterCodec(String name) {
        codecManager.unregisterCodec(name);
        return this;
    }

    @Override
    public <T> EventBus registerDefaultCodec(Class<T> clazz, MessageCodec<T, ?> codec) {
        codecManager.registerDefaultCodec(clazz, codec);
        return this;
    }

    @Override
    public EventBus unregisterDefaultCodec(Class clazz) {
        codecManager.unregisterDefaultCodec(clazz);
        return this;
    }

    @Override
    public void close(Handler<AsyncResult<Void>> completionHandler) {
        checkStarted();
        unregisterAll();

        if (completionHandler != null) {
            capzInternal.runOnContext(v -> completionHandler.handle(Future.succeededFuture()));
        }
    }


    protected MessageImpl createMessage(boolean send, String address, Map<String, HashSet<String>> headers, Object body, String codecName) {
        Objects.requireNonNull(address, "no null address accepted");
        MessageCodec codec = codecManager.lookupCodec(body, codecName);
        @SuppressWarnings("unchecked")
        MessageImpl msg = new MessageImpl(address, null, headers, body, codec, send, this);
        return msg;
    }

    protected <T> void addRegistration(String address, HandlerRegistration<T> registration,
                                       boolean replyHandler, boolean localOnly) {
        Objects.requireNonNull(registration.getHandler(), "handler");
        boolean newAddress = addLocalRegistration(address, registration, replyHandler, localOnly);
        addRegistration(newAddress, address, replyHandler, localOnly, registration::setResult);
    }

    protected <T> void addRegistration(boolean newAddress, String address,
                                       boolean replyHandler, boolean localOnly,
                                       Handler<AsyncResult<Void>> completionHandler) {
        completionHandler.handle(Future.succeededFuture());
    }

    protected <T> boolean addLocalRegistration(String address, HandlerRegistration<T> registration,
                                               boolean replyHandler, boolean localOnly) {
        Objects.requireNonNull(address, "address");

        Context context = Capz.currentContext();
        boolean hasContext = context != null;
        if (!hasContext) {
            // Embedded
            context = capzInternal.getOrCreateContext();
        }
        registration.setHandlerContext(context);

        boolean newAddress = false;

        HandlerHolder holder = new HandlerHolder<>(registration, replyHandler, localOnly, context);

        Handlers handlers = handlerMap.get(address);
        if (handlers == null) {
            handlers = new Handlers();
            Handlers prevHandlers = handlerMap.putIfAbsent(address, handlers);
            if (prevHandlers != null) {
                handlers = prevHandlers;
            }
            newAddress = true;
        }
        handlers.list.add(holder);

        if (hasContext) {
            HandlerEntry entry = new HandlerEntry<>(address, registration);
            //context.addCloseHook(entry);
        }

        return newAddress;
    }

    protected <T> void removeRegistration(String address, HandlerRegistration<T> handler, Handler<AsyncResult<Void>> completionHandler) {
        HandlerHolder holder = removeLocalRegistration(address, handler);
        removeRegistration(holder, address, completionHandler);
    }

    protected <T> void removeRegistration(HandlerHolder handlerHolder, String address,
                                          Handler<AsyncResult<Void>> completionHandler) {
        callCompletionHandlerAsync(completionHandler);
    }

    protected <T> HandlerHolder removeLocalRegistration(String address, HandlerRegistration<T> handler) {
        Handlers handlers = handlerMap.get(address);
        HandlerHolder lastHolder = null;
        if (handlers != null) {
            synchronized (handlers) {
                int size = handlers.list.size();
                // Requires a list traversal. This is tricky to optimise since we can't use a set since
                // we need fast ordered traversal for the round robin
                for (int i = 0; i < size; i++) {
                    HandlerHolder holder = handlers.list.get(i);
                    if (holder.getHandler() == handler) {
                        handlers.list.remove(i);
                        holder.setRemoved();
                        if (handlers.list.isEmpty()) {
                            handlerMap.remove(address);
                            lastHolder = holder;
                        }
                        //holder.getContext().removeCloseHook(new HandlerEntry<>(address, holder.getHandler()));
                        break;
                    }
                }
            }
        }
        return lastHolder;
    }

    protected <T> void sendReply(MessageImpl replyMessage, MessageImpl replierMessage, DeliveryOptions options,
                                 Handler<AsyncResult<Message<T>>> replyHandler) {
        if (replyMessage.address() == null) {
            throw new IllegalStateException("address not specified");
        } else {
            HandlerRegistration<T> replyHandlerRegistration = createReplyHandlerRegistration(replyMessage, options, replyHandler);
            new ReplySendContextImpl<>(replyMessage, options, replyHandlerRegistration, replierMessage).next();
        }
    }

    protected <T> void sendReply(SendContextImpl<T> sendContext, MessageImpl replierMessage) {
        sendOrPub(sendContext);
    }

    protected <T> void sendOrPub(SendContextImpl<T> sendContext) {
        MessageImpl message = sendContext.message;

        deliverMessageLocally(sendContext);
    }

    protected <T> Handler<Message<T>> convertHandler(Handler<AsyncResult<Message<T>>> handler) {
        return reply -> {
            Future<Message<T>> result;
            if (reply.body() instanceof ReplyException) {
                // This is kind of clunky - but hey-ho
                ReplyException exception = (ReplyException) reply.body();

                result = Future.failedFuture(exception);
            } else {
                result = Future.succeededFuture(reply);
            }
            handler.handle(result);
        };
    }

    protected void callCompletionHandlerAsync(Handler<AsyncResult<Void>> completionHandler) {
        if (completionHandler != null) {
            capzInternal.runOnContext(v -> {
                completionHandler.handle(Future.succeededFuture());
            });
        }
    }

    protected <T> void deliverMessageLocally(SendContextImpl<T> sendContext) {
        if (!deliverMessageLocally(sendContext.message)) {
            // no handlers

            if (sendContext.handlerRegistration != null) {
                sendContext.handlerRegistration.sendAsyncResultFailure(ReplyFailure.NO_HANDLERS,
                        "No handlers for address " + sendContext.message.address);
            }
        }
    }

    protected boolean isMessageLocal(MessageImpl msg) {
        return true;
    }

    protected <T> boolean deliverMessageLocally(MessageImpl msg) {
        msg.setBus(this);
        Handlers handlers = handlerMap.get(msg.address());
        if (handlers != null) {
            if (msg.isSend()) {
                //Choose one
                HandlerHolder holder = handlers.choose();

                if (holder != null) {
                    deliverToHandler(msg, holder);
                }
            } else {
                // Publish

                for (HandlerHolder holder : handlers.list) {
                    deliverToHandler(msg, holder);
                }
            }
            return true;
        } else {

            return false;
        }
    }

    protected void checkStarted() {
        if (!started) {
            throw new IllegalStateException("Event Bus is not started");
        }
    }

    protected String generateReplyAddress() {
        return Long.toString(replySequence.incrementAndGet());
    }

    private <T> HandlerRegistration<T> createReplyHandlerRegistration(MessageImpl message,
                                                                      DeliveryOptions options,
                                                                      Handler<AsyncResult<Message<T>>> replyHandler) {
        if (replyHandler != null) {
            long timeout = options.getTimeout();
            String replyAddress = generateReplyAddress();
            message.setReplyAddress(replyAddress);
            Handler<Message<T>> simpleReplyHandler = convertHandler(replyHandler);
            HandlerRegistration<T> registration =
                    new HandlerRegistration(capzInternal, this, replyAddress, message.address, true, replyHandler, timeout);
            registration.handler(simpleReplyHandler);
            return registration;
        } else {
            return null;
        }
    }

    private <T> void sendOrPubInternal(MessageImpl message, DeliveryOptions options,
                                       Handler<AsyncResult<Message<T>>> replyHandler) {
        checkStarted();
        HandlerRegistration<T> replyHandlerRegistration = createReplyHandlerRegistration(message, options, replyHandler);
        SendContextImpl<T> sendContext = new SendContextImpl<>(message, options, replyHandlerRegistration);
        sendContext.next();
    }

    protected class SendContextImpl<T> implements SendContext<T> {

        public final MessageImpl message;
        public final DeliveryOptions options;
        public final HandlerRegistration<T> handlerRegistration;
        public final Iterator<Handler<SendContext>> iter;

        public SendContextImpl(MessageImpl message, DeliveryOptions options, HandlerRegistration<T> handlerRegistration) {
            this.message = message;
            this.options = options;
            this.handlerRegistration = handlerRegistration;
            this.iter = interceptors.iterator();
        }

        @Override
        public Message<T> message() {
            return message;
        }

        @Override
        public void next() {
            if (iter.hasNext()) {
                Handler<SendContext> handler = iter.next();
                try {
                    handler.handle(this);
                } catch (Throwable t) {
                    log.error("Failure in interceptor", t);
                }
            } else {
                sendOrPub(this);
            }
        }

        @Override
        public boolean send() {
            return message.isSend();
        }

        @Override
        public Object sentBody() {
            return message.sentBody;
        }
    }

    protected class ReplySendContextImpl<T> extends SendContextImpl<T> {

        private final MessageImpl replierMessage;

        public ReplySendContextImpl(MessageImpl message, DeliveryOptions options, HandlerRegistration<T> handlerRegistration,
                                    MessageImpl replierMessage) {
            super(message, options, handlerRegistration);
            this.replierMessage = replierMessage;
        }

        @Override
        public void next() {
            if (iter.hasNext()) {
                Handler<SendContext> handler = iter.next();
                handler.handle(this);
            } else {
                sendReply(this, replierMessage);
            }
        }
    }


    private void unregisterAll() {
        // Unregister all handlers explicitly - don't rely on context hooks
        for (Handlers handlers : handlerMap.values()) {
            for (HandlerHolder holder : handlers.list) {
                holder.getHandler().unregister(true);
            }
        }
    }

    private <T> void deliverToHandler(MessageImpl msg, HandlerHolder<T> holder) {
        // Each handler gets a fresh copy
        @SuppressWarnings("unchecked")
        Message<T> copied = msg.copyBeforeReceive();

        holder.getContext().runOnContext((v) -> {
            // Need to check handler is still there - the handler might have been removed after the message were sent but
            // before it was received
            try {
                if (!holder.isRemoved()) {
                    holder.getHandler().handle(copied);
                }
            } finally {
                if (holder.isReplyHandler()) {
                    holder.getHandler().unregister();
                }
            }
        });
    }

    public class HandlerEntry<T> implements Closeable {
        final String address;
        final HandlerRegistration<T> handler;

        public HandlerEntry(String address, HandlerRegistration<T> handler) {
            this.address = address;
            this.handler = handler;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (getClass() != o.getClass()) return false;
            HandlerEntry entry = (HandlerEntry) o;
            if (!address.equals(entry.address)) return false;
            if (!handler.equals(entry.handler)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = address != null ? address.hashCode() : 0;
            result = 31 * result + (handler != null ? handler.hashCode() : 0);
            return result;
        }

        // Called by context on undeploy
        public void close(Handler<AsyncResult<Void>> completionHandler) {
            handler.unregister(completionHandler);
        }

    }

    @Override
    protected void finalize() throws Throwable {
        close(ar -> {
        });
        super.finalize();
    }

}
