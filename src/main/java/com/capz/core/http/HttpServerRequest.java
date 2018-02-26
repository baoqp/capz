package com.capz.core.http;


import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.http.impl.CapzHttpHeaders;
import com.capz.core.net.SocketAddress;
import com.capz.core.stream.ReadStream;

import java.util.List;
import java.util.Map;

/**
 * Represents a server-side HTTP request.
 * Instances are created for each request and passed to the user via a handler.
 * Each instance of this class is associated with a corresponding {@link HttpServerResponse} instance via
 */
public interface HttpServerRequest extends ReadStream<Buffer> {

    @Override
    HttpServerRequest exceptionHandler(Handler<Throwable> handler);

    @Override
    HttpServerRequest handler(Handler<Buffer> handler);

    @Override
    HttpServerRequest pause();

    @Override
    HttpServerRequest resume();

    @Override
    HttpServerRequest endHandler(Handler<Void> endHandler);


    HttpMethod method();


    String rawMethod();


    String scheme();


    String uri();

    String path();

    /**
     * @return the query part of the uri. For example someparam=32&amp;someotherparam=x
     */
    String query();

    String host();

    HttpServerResponse response();

    CapzHttpHeaders headers();

    String getHeader(String headerName);

    String getHeader(CharSequence headerName);


    Map<String, List<String>> params();

    String getParam(String paramName);

    SocketAddress remoteAddress();

    SocketAddress localAddress();

    String absoluteURI();

    /**
     * Convenience method for receiving the entire request body in one piece.
     * <p>
     * This saves the user having to manually setting a data and end handler and append the chunks of the body until
     * the whole body received. Don't use this if your request body is large - you could potentially run out of RAM.
     *
     * @param bodyHandler This handler will be called after all the body has been received
     */

    default HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
        if (bodyHandler != null) {
            Buffer body = Buffer.buffer();
            handler(body::appendBuffer);
            endHandler(v -> bodyHandler.handle(body));
        }
        return this;
    }


    /**
     * Call this with true if you are expecting a multi-part body to be submitted in the request.
     * This must be called before the body of the request has been received
     *
     * @param expect true - if you are expecting a multi-part body
     * @return a reference to this, so the API can be used fluently
     */
    HttpServerRequest setExpectMultipart(boolean expect);

    /**
     * @return true if we are expecting a multi-part body for this request. See {@link #setExpectMultipart}.
     */
    boolean isExpectMultipart();

    /**
     * Set an upload handler. The handler will get notified once a new file upload was received to allow you to deal
     * with the file upload.
     *
     * @return a reference to this, so the API can be used fluently
     */
    HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler);

    /**
     * Returns a map of all form attributes in the request.
     * <p>
     * Be aware that the attributes will only be available after the whole body has been received, i.e. after
     * the request end handler has been called.
     * <p>
     * {@link #setExpectMultipart(boolean)} must be called first before trying to get the form attributes.
     *
     * @return the form attributes
     */
    Map<String, List<String>> formAttributes();

    /**
     * Return the first form attribute value with the specified name
     *
     * @param attributeName the attribute name
     * @return the attribute value
     */
    String getFormAttribute(String attributeName);


    /**
     * Has the request ended? I.e. has the entire request, including the body been read?
     *
     * @return true if ended
     */
    boolean isEnded();

    HttpConnection connection();

}
