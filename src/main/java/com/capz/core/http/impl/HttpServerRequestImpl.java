package com.capz.core.http.impl;

import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.http.HttpConnection;
import com.capz.core.http.HttpServerFileUpload;
import com.capz.core.http.HttpServerRequest;
import com.capz.core.http.HttpServerResponse;
import com.capz.core.net.SocketAddress;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is optimised for performance when used on the same event loop that is was passed to the handler with.
 * However it can be used safely from other threads.
 * <p>
 * The internal state is protected by using the connection as a lock. If always used on the same event loop, then
 * we benefit from biased locking which makes the overhead of synchronized near zero.
 * <p>
 * It's important we don't have different locks for connection and request/response to avoid deadlock conditions
 */
public class HttpServerRequestImpl implements HttpServerRequest {

    private static final Logger log = LoggerFactory.getLogger(HttpServerRequestImpl.class);

    private final Http1xServerConnection conn;
    private final HttpRequest request;
    private final HttpServerResponse response;


    private com.capz.core.http.HttpMethod method;
    private String rawMethod;
    private String uri;
    private String path;
    private String query;

    private Handler<Buffer> dataHandler;
    private Handler<Throwable> exceptionHandler;

    //Cache this for performance
    private Map<String, List<String>> params;
    private CapzHttpHeaders headers;
    private String absoluteURI;

    private Handler<HttpServerFileUpload> uploadHandler;
    private Handler<Void> endHandler;
    private Map<String, List<String>> attributes;
    private HttpPostRequestDecoder decoder;
    private boolean ended;


    HttpServerRequestImpl(Http1xServerConnection conn,
                          HttpRequest request,
                          HttpServerResponse response) {
        this.conn = conn;
        this.request = request;
        this.response = response;
    }


    @Override
    public com.capz.core.http.HttpMethod method() {
        if (method == null) {
            String sMethod = request.method().toString();
            try {
                method = com.capz.core.http.HttpMethod.valueOf(sMethod);
            } catch (IllegalArgumentException e) {
                method = com.capz.core.http.HttpMethod.OTHER;
            }
        }
        return method;
    }

    @Override
    public String rawMethod() {
        if (rawMethod == null) {
            rawMethod = request.method().toString();
        }
        return rawMethod;
    }

    @Override
    public String uri() {
        if (uri == null) {
            uri = request.uri();
        }
        return uri;
    }

    @Override
    public String path() {
        if (path == null) {
            path = HttpUtils.parsePath(uri());
        }
        return path;
    }

    @Override
    public String query() {
        if (query == null) {
            query = HttpUtils.parseQuery(uri());
        }
        return query;
    }

    @Override
    public String host() {
        return getHeader(HttpHeaderNames.HOST);
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    @Override
    public CapzHttpHeaders headers() {
        // TODO
        return headers;
    }

    @Override
    public String getHeader(String headerName) {
        return  headers().get(headerName);
    }

    @Override
    public String getHeader(CharSequence headerName) {
       return headers().get(headerName);
    }

    @Override
    public Map<String, List<String>> params() {
        if (params == null) {
            params = HttpUtils.params(uri());
        }
        return params;
    }

    @Override
    public String getParam(String paramName) {
         List<String> param =  params().get(paramName);
         return param == null || param.size() == 0 ? null : param.get(0);
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkEnded();
            }
            dataHandler = handler;
            return this;
        }
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        synchronized (conn) {
            this.exceptionHandler = handler;
            return this;
        }
    }

    @Override
    public HttpServerRequest pause() {
        synchronized (conn) {
            if (!ended) {
                conn.pause();
            }
            return this;
        }
    }

    @Override
    public HttpServerRequest resume() {
        synchronized (conn) {
            if (!ended) {
                conn.resume();
            }
            return this;
        }
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkEnded();
            }
            endHandler = handler;
            return this;
        }
    }

    @Override
    public String scheme() {
        return "http";
    }


    @Override
    public SocketAddress remoteAddress() {
        return conn.remoteAddress();
    }

    @Override
    public String absoluteURI() {
        if (absoluteURI == null) {
            try {
                absoluteURI = HttpUtils.absoluteURI(conn.getServerOrigin(), this);
            } catch (URISyntaxException e) {
                log.error("Failed to create abs uri", e);
            }
        }
        return absoluteURI;
    }


    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> handler) {
        synchronized (conn) {
            if (handler != null) {
                checkEnded();
            }
            uploadHandler = handler;
            return this;
        }
    }

    @Override
    public Map<String, List<String>> formAttributes() {
        return attributes();
    }

    @Override
    public String getFormAttribute(String attributeName) {
        List<String> thisAttribute = formAttributes().get(attributeName);
        return thisAttribute == null || thisAttribute.size() == 0 ? null : thisAttribute.get(0);
    }


    @Override
    public HttpServerRequest setExpectMultipart(boolean expect) {
        synchronized (conn) {
            checkEnded();
            if (expect) {
                if (decoder == null) {
                    String contentType = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
                    if (contentType != null) {
                        HttpMethod method = request.getMethod();
                        String lowerCaseContentType = contentType.toLowerCase();
                        boolean isURLEncoded = lowerCaseContentType.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
                        if ((lowerCaseContentType.startsWith(HttpHeaders.Values.MULTIPART_FORM_DATA) || isURLEncoded) &&
                                (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)
                                        || method.equals(HttpMethod.DELETE))) {
                            decoder = new HttpPostRequestDecoder(new NettyFileUploadDataFactory(conn.capz(), this, () -> uploadHandler), request);
                        }
                    }
                }
            } else {
                decoder = null;
            }
            return this;
        }
    }

    @Override
    public boolean isExpectMultipart() {
        synchronized (conn) {
            return decoder != null;
        }
    }

    @Override
    public SocketAddress localAddress() {
        return conn.localAddress();
    }

    @Override
    public boolean isEnded() {
        synchronized (conn) {
            return ended;
        }
    }


    @Override
    public HttpConnection connection() {
        return conn;
    }

    void handleData(Buffer data) {
        synchronized (conn) {
            if (decoder != null) {
                try {
                    decoder.offer(new DefaultHttpContent(data.getByteBuf()));
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
                    handleException(e);
                }
            }
            if (dataHandler != null) {
                dataHandler.handle(data);
            }
        }
    }

    void handleEnd() {
        synchronized (conn) {
            ended = true;
            if (decoder != null) {
                try {
                    decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);
                    while (decoder.hasNext()) {
                        InterfaceHttpData data = decoder.next();
                        if (data instanceof Attribute) {
                            Attribute attr = (Attribute) data;
                            try {

                                List<String> attrs = attributes().get(attr.getName());

                                if (attrs == null) {
                                    attrs = new ArrayList<>();
                                    attributes().put(attr.getName(), attrs);
                                }

                                attrs.add(attr.getValue());

                            } catch (Exception e) {
                                // Will never happen, anyway handle it somehow just in case
                                handleException(e);
                            }
                        }
                    }
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
                    handleException(e);
                } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
                    // ignore this as it is expected
                } finally {
                    decoder.destroy();
                }
            }
            // If there have been uploads then we let the last one call the end handler once any fileuploads are complete
            if (endHandler != null) {
                endHandler.handle(null);
            }
        }
    }

    void handleException(Throwable t) {
        synchronized (conn) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(t);
            }
        }
    }

    private void sendNotImplementedAndClose() {
        response().setStatusCode(501).end();
        response().close();
    }

    private void checkEnded() {
        if (ended) {
            throw new IllegalStateException("Request has already been read");
        }
    }


    private Map<String, List<String>> attributes() {
        // Create it lazily
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }


    private static String urlDecode(String str) {
        return QueryStringDecoder.decodeComponent(str, CharsetUtil.UTF_8);
    }

}
