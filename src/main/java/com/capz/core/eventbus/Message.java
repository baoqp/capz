package com.capz.core.eventbus;

import com.capz.core.AsyncResult;
import com.capz.core.Handler;

import java.util.HashSet;
import java.util.Map;

// 从eventbus中接收的信息
public interface Message<T> {

    String address();

    Map<String, HashSet<String>> headers();

    T body();


    String replyAddress();


    boolean isSend();


    void reply(Object message);


    <R> void reply(Object message, Handler<AsyncResult<Message<R>>> replyHandler);


    void reply(Object message, DeliveryOptions options);


    <R> void reply(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler);

    void fail(int failureCode, String message);

}