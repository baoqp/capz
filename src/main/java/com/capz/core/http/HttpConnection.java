package com.capz.core.http;

import com.capz.core.Handler;
import com.capz.core.net.SocketAddress;


public interface HttpConnection {

    default int getWindowSize() {
        return -1;
    }


    default HttpConnection setWindowSize(int windowSize) {
        return this;
    }


    HttpConnection closeHandler(Handler<Void> handler);


    void close();


    HttpConnection exceptionHandler(Handler<Throwable> handler);


    /**
     * @return the remote address for this connection
     */
    SocketAddress remoteAddress();


    SocketAddress localAddress();

    String indicatedServerName();
}
