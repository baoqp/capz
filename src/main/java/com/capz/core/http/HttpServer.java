package com.capz.core.http;

import com.capz.core.AsyncResult;
import com.capz.core.Handler;
import com.capz.core.stream.ReadStream;

/**
 * @author Bao Qingping
 */
public interface HttpServer {

    ReadStream<HttpServerRequest> requestStream();

    HttpServer requestHandler(Handler<HttpServerRequest> handler);

    Handler<HttpServerRequest> requestHandler();


    HttpServer connectionHandler(Handler<HttpConnection> handler);


    HttpServer exceptionHandler(Handler<Throwable> handler);


    HttpServer listen();

    HttpServer listen(int port, String host);

    HttpServer listen(int port, String host, Handler<AsyncResult<HttpServer>> listenHandler);

    HttpServer listen(int port);

    HttpServer listen(int port, Handler<AsyncResult<HttpServer>> listenHandler);

    HttpServer listen(Handler<AsyncResult<HttpServer>> listenHandler);

    void close();

    void close(Handler<AsyncResult<Void>> completionHandler);

    int actualPort();

}
