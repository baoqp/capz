package com.capz.core.http.impl;

import com.capz.core.AsyncResult;
import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.http.HttpConnection;
import com.capz.core.http.HttpServer;
import com.capz.core.http.HttpServerOptions;
import com.capz.core.http.HttpServerRequest;
import com.capz.core.impl.AbstractContext;
import com.capz.core.impl.Future;
import com.capz.core.net.SocketAddress;
import com.capz.core.net.impl.AsyncResolveConnectHelper;
import com.capz.core.net.impl.CapzEventLoopGroup;
import com.capz.core.net.impl.HandlerHolder;
import com.capz.core.net.impl.HandlerManager;
import com.capz.core.net.impl.ServerID;
import com.capz.core.stream.ReadStream;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bao Qingping
 */
public class HttpServerImpl implements HttpServer {

    private static final Logger log = LoggerFactory.getLogger(HttpServerImpl.class);
    private volatile boolean listening;
    private volatile int actualPort;

    private ChannelGroup serverChannelGroup;

    private final CapzEventLoopGroup availableWorkers = new CapzEventLoopGroup();
    private final HandlerManager<HttpHandlers> httpHandlerMgr = new HandlerManager<>(availableWorkers);

    private static final Handler<Throwable> DEFAULT_EXCEPTION_HANDLER = t -> log.trace("Connection failure", t);

    private CapzInternal capz;

    private AbstractContext listenContext;

    private AbstractContext creatingContext;

    private boolean logEnabled;

    private String serverOrigin;

    private HttpServerOptions options;

    private ServerID id;

    private final Map<Channel, Http1xServerConnection> connectionMap = new ConcurrentHashMap<>();

    private AsyncResolveConnectHelper bindFuture;

    private HttpServerImpl actualServer;


    private final HttpStreamHandler<HttpServerRequest> requestStream = new HttpStreamHandler<>();
    private Handler<HttpConnection> connectionHandler;

    private Handler<Throwable> exceptionHandler;

    public synchronized HttpServer listen(int port, String host, Handler<AsyncResult<HttpServer>> listenHandler) {

        /*if (requestStream.handler() == null && wsStream.handler() == null) {
            throw new IllegalStateException("Set request or websocket handler first");
        }*/

        if (listening) {
            throw new IllegalStateException("Already listening");
        }

        listenContext = capz.getOrCreateContext();
        listening = true;

        synchronized (capz.sharedHttpServers()) {
            this.actualPort = port; // Will be updated on bind for a wildcard port
            id = new ServerID(port, host);
            HttpServerImpl shared = capz.sharedHttpServers().get(id);
            if (shared == null || port == 0) {
                serverChannelGroup = new DefaultChannelGroup("capz-acceptor-channels", GlobalEventExecutor.INSTANCE);
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(capz.getAcceptorEventLoopGroup(), availableWorkers);
                //applyConnectionOptions(bootstrap);

                bootstrap.childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (requestStream.isPaused()) {
                            ch.close();
                            return;
                        }
                        ChannelPipeline pipeline = ch.pipeline();

                        // 只支持http1
                        handleHttp1(ch);

                    }
                });

                addHandlers(this, listenContext);

                try {
                    bindFuture = AsyncResolveConnectHelper.doBind(capz, SocketAddress.inetSocketAddress(port, host), bootstrap);
                    bindFuture.addListener(res -> {
                        if (res.failed()) {
                            capz.sharedHttpServers().remove(id);
                        } else {
                            Channel serverChannel = res.result();
                            HttpServerImpl.this.actualPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
                            serverChannelGroup.add(serverChannel);


                        }
                    });
                } catch (final Throwable t) {
                    // Make sure we send the exception back through the handler (if any)
                    if (listenHandler != null) {
                        capz.runOnContext(v -> listenHandler.handle(Future.failedFuture(t)));
                    } else {
                        // No handler - log so user can see failure
                        log.error(t.toString());
                    }
                    listening = false;
                    return this;
                }
                capz.sharedHttpServers().put(id, this);
                actualServer = this;
            } else {
                // Server already exists with that host/port - we will use that
                actualServer = shared;
                this.actualPort = shared.actualPort;
                addHandlers(actualServer, listenContext);

            }
            actualServer.bindFuture.addListener(future -> {
                if (listenHandler != null) {
                    final AsyncResult<HttpServer> res;
                    if (future.succeeded()) {
                        res = Future.succeededFuture(HttpServerImpl.this);
                    } else {
                        res = Future.failedFuture(future.cause());
                        listening = false;
                    }
                    listenContext.runOnContext((v) -> listenHandler.handle(res));
                } else if (future.failed()) {
                    listening = false;
                    // No handler - log so user can see failure
                    log.error(future.cause().toString());
                }
            });
        }
        return this;
    }


    @Override
    public synchronized HttpServer requestHandler(Handler<HttpServerRequest> handler) {
        requestStream.handler(handler);
        return this;
    }

    @Override
    public ReadStream<HttpServerRequest> requestStream() {
        return requestStream;
    }


    @Override
    public Handler<HttpServerRequest> requestHandler() {
        return requestStream.handler();
    }


    @Override
    public synchronized HttpServer connectionHandler(Handler<HttpConnection> handler) {
        if (listening) {
            throw new IllegalStateException("Please set handler before server is listening");
        }
        connectionHandler = handler;
        return this;
    }


    @Override
    public synchronized HttpServer exceptionHandler(Handler<Throwable> handler) {
        if (listening) {
            throw new IllegalStateException("Please set handler before server is listening");
        }
        exceptionHandler = handler;
        return this;
    }


    @Override
    public HttpServer listen() {
        return listen(options.getPort(), options.getHost(), null);

    }

    @Override
    public HttpServer listen(Handler<AsyncResult<HttpServer>> listenHandler) {
        return listen(options.getPort(), options.getHost(), listenHandler);
    }

    @Override
    public HttpServer listen(int port) {
        return listen(port, "0.0.0.0", null);
    }

    @Override
    public HttpServer listen(int port, String host) {
        return listen(port, host, null);
    }


    @Override
    public HttpServer listen(int port, Handler<AsyncResult<HttpServer>> listenHandler) {
        return listen(port, "0.0.0.0", listenHandler);
    }


    @Override
    public void close() {
        close(null);
    }

    @Override
    public synchronized void close(Handler<AsyncResult<Void>> done) {
        if (requestStream.endHandler() != null) {
            Handler<Void> requestEndHandler = requestStream.endHandler();
            requestStream.endHandler(null);
            Handler<AsyncResult<Void>> next = done;
            done = event -> {
                if (event.succeeded()) {

                    if (requestEndHandler != null) {
                        requestEndHandler.handle(event.result());
                    }
                }
                if (next != null) {
                    next.handle(event);
                }
            };
        }

        AbstractContext context = capz.getOrCreateContext();
        if (!listening) {
            executeCloseDone(context, done, null);
            return;
        }
        listening = false;

        synchronized (capz.sharedHttpServers()) {

            if (actualServer != null) {

                actualServer.httpHandlerMgr.removeHandler(
                        new HttpHandlers(
                                requestStream.handler(),

                                connectionHandler,
                                exceptionHandler == null ? DEFAULT_EXCEPTION_HANDLER : exceptionHandler)
                        , listenContext);

                if (actualServer.httpHandlerMgr.hasHandlers()) {
                    // The actual server still has handlers so we don't actually close it
                    if (done != null) {
                        executeCloseDone(context, done, null);
                    }
                } else {
                    // No Handlers left so close the actual server
                    // The done handler needs to be executed on the context that calls close, NOT the context
                    // of the actual server
                    actualServer.actualClose(context, done);
                }
            }
        }
        if (creatingContext != null) {
            //creatingContext.removeCloseHook(this);
        }
    }


    @Override
    public int actualPort() {
        return actualPort;
    }

    private void executeCloseDone(final AbstractContext closeContext, final Handler<AsyncResult<Void>> done, final Exception e) {
        if (done != null) {
            Future<Void> fut = e != null ? Future.failedFuture(e) : Future.succeededFuture();
            closeContext.runOnContext((v) -> done.handle(fut));
        }
    }


    private void actualClose(final AbstractContext closeContext, final Handler<AsyncResult<Void>> done) {
        if (id != null) {
            capz.sharedHttpServers().remove(id);
        }

        AbstractContext currCon = capz.getContext();

        for (Http1xServerConnection conn : connectionMap.values()) {
            conn.close();
        }


        // Sanity check
        if (capz.getContext() != currCon) {
            throw new IllegalStateException("Context was changed");
        }


        ChannelGroupFuture fut = serverChannelGroup.close();
        fut.addListener(cgf -> executeCloseDone(closeContext, done, fut.cause()));
    }

    private void addHandlers(HttpServerImpl server, AbstractContext context) {
        server.httpHandlerMgr.addHandler(
                new HttpHandlers(
                        requestStream.handler(),
                        connectionHandler,
                        exceptionHandler == null ? DEFAULT_EXCEPTION_HANDLER : exceptionHandler)
                , context);
    }

    private void handleHttp1(Channel ch) {
        HandlerHolder<HttpHandlers> holder = httpHandlerMgr.chooseHandler(ch.eventLoop());
        if (holder == null) {
            sendServiceUnavailable(ch);
            return;
        }
        configureHttp1(ch.pipeline(), holder);
    }

    // 配置netty pipeline上的handler
    private void configureHttp1(ChannelPipeline pipeline, HandlerHolder<HttpHandlers> holder) {
        if (logEnabled) {
            pipeline.addLast("logging", new LoggingHandler());
        }

        pipeline.addLast("httpDecoder", new HttpRequestDecoder(options.getMaxInitialLineLength(),
                options.getMaxHeaderSize(), options.getMaxChunkSize(), false, options.getDecoderInitialBufferSize()));
        pipeline.addLast("httpEncoder", new HttpResponseEncoder());

        if (options.isDecompressionSupported()) {
            pipeline.addLast("inflater", new HttpContentDecompressor(true));
        }
        if (options.isCompressionSupported()) {
            pipeline.addLast("deflater", new HttpContentCompressor(options.getCompressionLevel()));
        }

        Http1xServerHandler handler;

        handler = new Http1xServerHandler(options, serverOrigin, holder);

        handler.addHandler(conn -> {
            connectionMap.put(pipeline.channel(), conn);
        });
        handler.removeHandler(conn -> {
            connectionMap.remove(pipeline.channel());
        });
        pipeline.addLast("handler", handler);
    }


    private void sendServiceUnavailable(Channel ch) {
        ch.writeAndFlush(
                Unpooled.copiedBuffer("HTTP/1.1 503 Service Unavailable\r\n" +
                        "Content-Length:0\r\n" +
                        "\r\n", StandardCharsets.ISO_8859_1))
                .addListener(ChannelFutureListener.CLOSE);
    }


    private class HttpStreamHandler<C extends ReadStream<Buffer>> implements ReadStream<C> {

        private Handler<C> handler;
        private boolean paused;
        private Handler<Void> endHandler;

        Handler<C> handler() {
            synchronized (HttpServerImpl.this) {
                return handler;
            }
        }

        boolean isPaused() {
            synchronized (HttpServerImpl.this) {
                return paused;
            }
        }

        Handler<Void> endHandler() {
            synchronized (HttpServerImpl.this) {
                return endHandler;
            }
        }

        @Override
        public ReadStream handler(Handler<C> handler) {
            synchronized (HttpServerImpl.this) {
                if (listening) {
                    throw new IllegalStateException("Please set handler before server is listening");
                }
                this.handler = handler;
                return this;
            }
        }

        @Override
        public ReadStream pause() {
            synchronized (HttpServerImpl.this) {
                if (!paused) {
                    paused = true;
                }
                return this;
            }
        }

        @Override
        public ReadStream resume() {
            synchronized (HttpServerImpl.this) {
                if (paused) {
                    paused = false;
                }
                return this;
            }
        }

        @Override
        public ReadStream endHandler(Handler<Void> endHandler) {
            synchronized (HttpServerImpl.this) {
                this.endHandler = endHandler;
                return this;
            }
        }

        @Override
        public ReadStream exceptionHandler(Handler<Throwable> handler) {
            // Should we use it in the server close exception handler ?
            return this;
        }
    }
}
