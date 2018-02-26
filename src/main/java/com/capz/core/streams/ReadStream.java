package com.capz.core.streams;

import com.capz.core.Handler;

// 读取流
public interface ReadStream<T> extends StreamBase {

    // Set a data handler. As data is read, the handler will be called with the data.
    ReadStream<T> handler(Handler<T> handler);


    ReadStream<T> pause();


    ReadStream<T> resume();

    // Set an end handler. Once the streams has ended, and there is no more data to be read, this handler will be called.
    ReadStream<T> endHandler(Handler<Void> endHandler);

}