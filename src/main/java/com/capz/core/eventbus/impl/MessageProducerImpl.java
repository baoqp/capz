package com.capz.core.eventbus.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Capz;
import com.capz.core.Handler;
import com.capz.core.eventbus.DeliveryOptions;
import com.capz.core.eventbus.EventBus;
import com.capz.core.eventbus.Message;
import com.capz.core.eventbus.MessageConsumer;
import com.capz.core.eventbus.MessageProducer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

public class MessageProducerImpl<T> implements MessageProducer<T> {

    public static final String CREDIT_ADDRESS_HEADER_NAME = "__Capz.credit";

    private final Capz Capz;
    private final EventBus bus;
    private final boolean send;
    private final String address;
    private final Queue<T> pending = new ArrayDeque<>();
    private final MessageConsumer<Integer> creditConsumer;
    private DeliveryOptions options;
    private int maxSize = DEFAULT_WRITE_QUEUE_MAX_SIZE;
    private int credits = DEFAULT_WRITE_QUEUE_MAX_SIZE;
    private Handler<Void> drainHandler;

    public MessageProducerImpl(Capz Capz, String address, boolean send, DeliveryOptions options) {
        this.Capz = Capz;
        this.bus = Capz.eventBus();
        this.address = address;
        this.send = send;
        this.options = options;
        if (send) {
            String creditAddress = UUID.randomUUID().toString() + "-credit";
            creditConsumer = bus.consumer(creditAddress, msg -> {
                doReceiveCredit(msg.body());
            });
            options.addHeader(CREDIT_ADDRESS_HEADER_NAME, creditAddress);
        } else {
            creditConsumer = null;
        }
    }

    @Override
    public synchronized MessageProducer<T> deliveryOptions(DeliveryOptions options) {
        if (creditConsumer != null) {
            options = new DeliveryOptions(options);
            // options.addHeader(CREDIT_ADDRESS_HEADER_NAME, this.options.getHeaders().get(CREDIT_ADDRESS_HEADER_NAME));
        }
        this.options = options;
        return this;
    }

    @Override
    public MessageProducer<T> send(T message) {
        doSend(message, null);
        return this;
    }

    @Override
    public <R> MessageProducer<T> send(T message, Handler<AsyncResult<Message<R>>> replyHandler) {
        doSend(message, replyHandler);
        return this;
    }

    @Override
    public MessageProducer<T> exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    @Override
    public synchronized MessageProducer<T> setWriteQueueMaxSize(int s) {
        int delta = s - maxSize;
        maxSize = s;
        credits += delta;
        return this;
    }

    @Override
    public synchronized MessageProducer<T> write(T data) {
        if (send) {
            doSend(data, null);
        } else {
            bus.publish(address, data, options);
        }
        return this;
    }

    @Override
    public synchronized boolean writeQueueFull() {
        return credits == 0;
    }

    @Override
    public synchronized MessageProducer<T> drainHandler(Handler<Void> handler) {
        this.drainHandler = handler;
        return this;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public void end() {
        close();
    }

    @Override
    public void close() {
        if (creditConsumer != null) {
            creditConsumer.unregister();
        }
    }

    // Just in case user forget to call close()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private synchronized <R> void doSend(T data, Handler<AsyncResult<Message<R>>> replyHandler) {
        if (credits > 0) {
            credits--;
            if (replyHandler == null) {
                bus.send(address, data, options);
            } else {
                bus.send(address, data, options, replyHandler);
            }
        } else {
            pending.add(data);
        }
    }

    private synchronized void doReceiveCredit(int credit) {
        credits += credit;
        while (credits > 0) {
            T data = pending.poll();
            if (data == null) {
                break;
            } else {
                credits--;
                bus.send(address, data, options);
            }
        }
        final Handler<Void> theDrainHandler = drainHandler;
        if (theDrainHandler != null && credits >= maxSize / 2) {
            this.drainHandler = null;
            Capz.runOnContext(v -> theDrainHandler.handle(null));
        }
    }
}
