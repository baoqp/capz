package com.capz.core.eventbus;

import com.capz.core.AsyncResult;
import com.capz.core.Handler;
import com.capz.core.streams.WriteStream;

public interface MessageProducer<T> extends WriteStream<T> {

    int DEFAULT_WRITE_QUEUE_MAX_SIZE = 1000;

    MessageProducer<T> send(T message);

    <R> MessageProducer<T> send(T message, Handler<AsyncResult<Message<R>>> replyHandler);

     MessageProducer<T> deliveryOptions(DeliveryOptions options);

    String address();


    void close();
}
