
package com.capz.core.http;


import com.capz.core.AsyncResult;
import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.http.impl.CapzHttpHeaders;
import com.capz.core.streams.WriteStream;

import java.util.List;
import java.util.Map;

/**
 * Represents a server-side HTTP response.
 * <p>
 * An instance of this is created and associated to every instance of
 * {@link HttpServerRequest} that.
 * <p>
 * It allows the developer to control the HTTP response that is sent back to the
 * client for a particular HTTP request.
 * <p>
 * It contains methods that allow HTTP headers and trailers to be set, and for a body to be written out to the response.
 * <p>
 * It also allows files to be streamed by the kernel directly from disk to the
 * outgoing HTTP connection, bypassing user space altogether (where supported by
 * the underlying operating system). This is a very efficient way of
 * serving files from the server since buffers do not have to be read one by one
 * from the file and written to the outgoing socket.
 */

public interface HttpServerResponse extends WriteStream<Buffer> {

    @Override
    HttpServerResponse exceptionHandler(Handler<Throwable> handler);

    @Override
    HttpServerResponse write(Buffer data);

    @Override
    HttpServerResponse setWriteQueueMaxSize(int maxSize);

    @Override
    HttpServerResponse drainHandler(Handler<Void> handler);


    int getStatusCode();


    HttpServerResponse setStatusCode(int statusCode);


    String getStatusMessage();


    HttpServerResponse setStatusMessage(String statusMessage);

    /**
     * If {@code chunked} is {@code true}, this response will use HTTP chunked encoding, and each call to write to the body
     * will correspond to a new HTTP chunk sent on the wire.
     * <p>
     * If chunked encoding is used the HTTP header {@code Transfer-Encoding} with a value of {@code Chunked} will be
     * automatically inserted in the response.
     * <p>
     * If {@code chunked} is {@code false}, this response will not use HTTP chunked encoding, and therefore the total size
     * of any data that is written in the respone body must be set in the {@code Content-Length} header <b>before</b> any
     * data is written out.
     * <p>
     * An HTTP chunked response is typically used when you do not know the total size of the request body up front.
     *
     * @return a reference to this, so the API can be used fluently
     */
    HttpServerResponse setChunked(boolean chunked);

    /**
     * @return is the response chunked?
     */
    boolean isChunked();


    CapzHttpHeaders headers();


    HttpServerResponse putHeader(String name, String value);

    HttpServerResponse putHeader(CharSequence name, CharSequence value);


    HttpServerResponse putHeader(String name, Iterable<String> values);

    HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values);


    Map<String, List<String>> trailers();


    HttpServerResponse putTrailer(String name, String value);

    HttpServerResponse putTrailer(String name, Iterable<String> values);

    HttpServerResponse closeHandler(Handler<Void> handler);

    HttpServerResponse endHandler(Handler<Void> handler);


    HttpServerResponse write(String chunk, String enc);

    HttpServerResponse write(String chunk);

    HttpServerResponse writeContinue();


    void end(String chunk);

    void end(String chunk, String enc);


    void end(Buffer chunk);


    void end();

    default HttpServerResponse sendFile(String filename) {
        return sendFile(filename, 0);
    }


    default HttpServerResponse sendFile(String filename, long offset) {
        return sendFile(filename, offset, Long.MAX_VALUE);
    }


    HttpServerResponse sendFile(String filename, long offset, long length);

    default HttpServerResponse sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
        return sendFile(filename, 0, resultHandler);
    }


    default HttpServerResponse sendFile(String filename, long offset, Handler<AsyncResult<Void>> resultHandler) {
        return sendFile(filename, offset, Long.MAX_VALUE, resultHandler);
    }

    HttpServerResponse sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler);


    void close();

    boolean ended();


    boolean closed();


    boolean headWritten();

    /**
     * Provide a handler that will be called just before the headers are written to the wire.<p>
     * This provides a hook allowing you to add any more headers or do any more operations before this occurs.
     *
     * @param handler the handler
     * @return a reference to this, so the API can be used fluently
     */
    HttpServerResponse headersEndHandler(Handler<Void> handler);

    /**
     * Provides a handler that will be called after the last part of the body is written to the wire.
     * The handler is called asynchronously of when the response has been received by the client.
     * This provides a hook allowing you to do more operations once the request has been sent over the wire
     * such as resource cleanup.
     *
     * @param handler the handler
     * @return a reference to this, so the API can be used fluently
     */
    HttpServerResponse bodyEndHandler(Handler<Void> handler);

    /**
     * @return the total number of bytes written for the body of the response.
     */
    long bytesWritten();

    /**
     * @return the id of the streams of this response, {@literal -1} for HTTP/1.x
     */
    int streamId();

}
