package com.capz.core.streams;

import com.capz.core.Handler;

public interface StreamBase {

    StreamBase exceptionHandler(Handler<Throwable> handler);
}