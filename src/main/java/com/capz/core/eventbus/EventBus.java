package com.capz.core.eventbus;

import com.capz.core.AsyncResult;
import com.capz.core.Handler;

/**
 * 事件总线
 */
public interface EventBus {

    // 发送信息
    EventBus send(String address, Object message);

    // 异步发送信息
    <T> EventBus send(String address, Object message, Handler<AsyncResult<Message<T>>> replyHandler);


    //EventBus send(String address, Object message, DeliveryOptions options);


    //<T> EventBus send(String address, Object message, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler);

    // 发布消息
    EventBus publish(String address, Object message);


    EventBus publish(String address, Object message, DeliveryOptions options);


    <T> MessageConsumer<T> consumer(String address);


    <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler);


    <T> MessageConsumer<T> localConsumer(String address);


    <T> MessageConsumer<T> localConsumer(String address, Handler<Message<T>> handler);

    // Create a message sender against the specified address.
    <T> MessageProducer<T> sender(String address);


    <T> MessageProducer<T> sender(String address, DeliveryOptions options);


    <T> MessageProducer<T> publisher(String address);


    <T> MessageProducer<T> publisher(String address, DeliveryOptions options);


    EventBus registerCodec(MessageCodec codec);


    EventBus unregisterCodec(String name);


    <T> EventBus registerDefaultCodec(Class<T> clazz, MessageCodec<T, ?> codec);


    EventBus unregisterDefaultCodec(Class clazz);


    //Start the event bus. This would not normally be called in user code
    void start(Handler<AsyncResult<Void>> completionHandler);


    //Close the event bus and release any resources held. This would not normally be called in user code
    void close(Handler<AsyncResult<Void>> completionHandler);


    // 拦截器
    EventBus addInterceptor(Handler<SendContext> interceptor);

    EventBus removeInterceptor(Handler<SendContext> interceptor);

}
