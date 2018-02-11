package com.capz.core.stream;

import com.capz.core.Handler;

// 写入流
public interface WriteStream<T> extends StreamBase {

    WriteStream<T> write(T data);

    // 结束写入
    void end();

    default void end(T t) {
        write(t);
        end();
    }

    WriteStream<T> setWriteQueueMaxSize(int maxSize);

    /**
     * This will return {@code true} if there are more bytes in the write queue than the value set using {@link
     * #setWriteQueueMaxSize}
     *
     * @return true if write queue is full
     */
    boolean writeQueueFull();

    /**
     * Set a drain handler on the stream. If the write queue is full, then the handler will be called when the write
     * queue is ready to accept buffers again.
     */
    WriteStream<T> drainHandler(Handler<Void> handler);

}