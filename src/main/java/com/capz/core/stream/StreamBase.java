package com.capz.core.stream;

import com.capz.core.Handler;

public interface StreamBase {

    StreamBase exceptionHandler(Handler<Throwable> handler);
}