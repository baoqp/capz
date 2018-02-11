package com.capz.core;

public interface Closeable {

    void close(Handler<AsyncResult<Void>> completionHandler);
}
