package com.capz.core.eventbus;

import com.capz.core.AsyncResult;
import com.capz.core.Handler;
import com.capz.core.streams.ReadStream;

public interface MessageConsumer<T> {

    MessageConsumer<T> exceptionHandler(Handler<Throwable> handler);

    MessageConsumer<T> handler(Handler<Message<T>> handler);

    MessageConsumer<T> pause();

    MessageConsumer<T> resume();

    MessageConsumer<T> endHandler(Handler<Void> endHandler);

    /**
     * 返回代表信息的ReadStream
     */
    ReadStream<T> bodyStream();

    boolean isRegistered();

    String address();

    MessageConsumer<T> setMaxBufferedMessages(int maxBufferedMessages);


    int getMaxBufferedMessages();


    void completionHandler(Handler<AsyncResult<Void>> completionHandler);

    void unregister();

    void unregister(Handler<AsyncResult<Void>> completionHandler);
}