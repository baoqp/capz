
package com.capz.core.http.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Capz;
import com.capz.core.CapzInternal;
import com.capz.core.Exception.CapzException;
import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.http.HttpConnection;
import com.capz.core.http.HttpServerOptions;
import com.capz.core.http.HttpServerRequest;
import com.capz.core.impl.AbstractContext;
import com.capz.core.net.SocketAddress;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.Deque;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * This class is optimised for performance when used on the same event loop. However it can be used safely from
 * other threads. The internal state is protected using the synchronized keyword. If always used on the same event
 * loop, then we benefit from biased locking which makes the overhead of synchronized near zero.
 * 服务端连接封装
 */
public class Http1xServerConnection extends Http1xConnectionBase implements HttpConnection {

    private static final Logger log = LoggerFactory.getLogger(Http1xServerConnection.class);

    private static final Handler<HttpServerRequest> NULL_REQUEST_HANDLER = req -> {};

    private static final int CHANNEL_PAUSE_QUEUE_SIZE = 5;

    private final Deque  pending = new ArrayDeque (8);

    private final String serverOrigin;

    private Handler<HttpServerRequest> requestHandler = NULL_REQUEST_HANDLER;

    private HttpServerRequestImpl currentRequest;
    private HttpServerResponseImpl pendingResponse;

    private boolean channelPaused;
    private boolean paused;
    private boolean sentCheck;

    private boolean queueing;

    private HttpServerOptions options;

    public Http1xServerConnection(CapzInternal capz,
                                  HttpServerOptions options,
                                  ChannelHandlerContext channel,
                                  AbstractContext context,
                                  String serverOrigin) {
        super(capz, channel, context);
        this.serverOrigin = serverOrigin;
        this.options = options;

    }


    synchronized void pause() {
        if (!paused) {
            paused = true;
            queueing = true;
        }
    }

    synchronized void resume() {
        if (paused) {
            paused = false;
            checkNextTick();
        }
    }

    synchronized void handleMessage(Object msg) {
        if (queueing) {
            enqueue(msg);
        } else {
            if (processMessage(msg)) {
                checkNextTick();
            } else {
                enqueue(msg);
            }
        }
    }

    private void enqueue(Object msg) {
        //We queue requests if paused or a request is in progress to prevent responses being written in the wrong order
        queueing = true;
        pending.add(msg);
        if (pending.size() == CHANNEL_PAUSE_QUEUE_SIZE) {
            //We pause the channel too, to prevent the queue growing too large, but we don't do this
            //until the queue reaches a certain size, to avoid pausing it too often
            super.doPause();
            channelPaused = true;
        }
    }

    synchronized void responseComplete() {

        pendingResponse = null;
        checkNextTick();
    }

    synchronized void requestHandler(Handler<HttpServerRequest> handler) {
        this.requestHandler = handler;
    }


    String getServerOrigin() {
        return serverOrigin;
    }

    CapzInternal capz() {
        return capz;
    }


    private void handleChunk(Buffer chunk) {
        currentRequest.handleData(chunk);
    }

    @Override
    public synchronized void handleInterestedOpsChanged() {
        if (!isNotWritable()) {
            if (pendingResponse != null) {
                pendingResponse.handleDrained();
            }
        }
    }


    @Override
    public void closeWithPayload(ByteBuf byteBuf) {

    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public String indicatedServerName() {
        return null;
    }

    void write100Continue() {
        chctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
    }

    synchronized public void handleClosed() {

        super.handleClosed();

        if (currentRequest != null) {
            currentRequest.handleException(new CapzException("Connection was closed"));
        }
        if (pendingResponse != null) {

            pendingResponse.handleClosed();
        }
    }

    public AbstractContext getContext() {
        return super.getContext();
    }

    @Override
    public synchronized void handleException(Throwable t) {
        super.handleException(t);

        if (currentRequest != null) {
            currentRequest.handleException(t);
        }
        if (pendingResponse != null) {
            pendingResponse.handleException(t);
        }

    }

    protected void addFuture(Handler<AsyncResult<Void>> completionHandler, ChannelFuture future) {
        super.addFuture(completionHandler, future);
    }


    protected ChannelFuture sendFile(RandomAccessFile file, long offset, long length) throws IOException {
        return super.sendFile(file, offset, length);
    }

    private void handleError(HttpObject obj) {
        DecoderResult result = obj.decoderResult();
        Throwable cause = result.cause();
        if (cause instanceof TooLongFrameException) {
            String causeMsg = cause.getMessage();
            HttpVersion version = HttpVersion.HTTP_1_1;

            HttpResponseStatus status = causeMsg.startsWith("An HTTP line is larger than") ? HttpResponseStatus.REQUEST_URI_TOO_LONG : HttpResponseStatus.BAD_REQUEST;
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(version, status);
            ChannelPromise fut = chctx.newPromise();
            writeToChannel(resp, fut);
            fut.addListener(res -> {
                if (res.isSuccess()) {
                    // That will close the connection as it is considered as unusable
                    chctx.pipeline().fireExceptionCaught(result.cause());
                }
            });
        } else {
            // That will close the connection as it is considered as unusable
            chctx.pipeline().fireExceptionCaught(result.cause());
        }
    }

    private boolean processMessage(Object msg) {
        if (msg instanceof HttpRequest) {
            if (pendingResponse != null) {
                return false;
            }
            HttpRequest request = (HttpRequest) msg;
            if (request.decoderResult().isFailure()) {
                handleError(request);
                return false;
            }
            if (options.isHandle100ContinueAutomatically() && HttpUtil.is100ContinueExpected(request)) {
                write100Continue();
            }
            HttpServerResponseImpl resp = new HttpServerResponseImpl(capz, this, request);
            HttpServerRequestImpl req = new HttpServerRequestImpl(this, request, resp);
            currentRequest = req;
            pendingResponse = resp;

            requestHandler.handle(req);
        } else if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
            handleLastHttpContent();
        } else if (msg instanceof HttpContent) {
            handleContent(msg);
        } else {
            handleOther(msg);
        }
        return true;
    }

    private void handleContent(Object msg) {
        HttpContent content = (HttpContent) msg;
        if (content.decoderResult().isFailure()) {
            handleError(content);
            return;
        }
        ByteBuf chunk = content.content();
        if (chunk.isReadable()) {
            Buffer buff = Buffer.buffer(chunk);
            handleChunk(buff);
        }
        //TODO chunk trailers
        if (content instanceof LastHttpContent) {
            handleLastHttpContent();
        }
    }

    private void handleOther(Object msg) {

    }

    private void handleLastHttpContent() {
        currentRequest.handleEnd();
        currentRequest = null;
    }


    private void checkNextTick() {
        // Check if there are more pending messages in the queue that can be processed next time around
        if (!paused && !sentCheck) {
            sentCheck = true;
            capz.runOnContext(v -> {
                synchronized (Http1xServerConnection.this) {
                    sentCheck = false;
                    if (!paused) {
                        Object msg = pending.poll();
                        if (msg != null) {
                            if (processMessage(msg)) {
                                checkNextTick();
                            } else {
                                pending.addFirst(msg);
                            }
                        }
                        if (pending.isEmpty()) {
                            queueing = false;
                            if (channelPaused) {
                                // Resume the actual channel
                                channelPaused = false;
                                Http1xServerConnection.super.doResume();
                            }
                        } else {
                            queueing = true;
                            checkNextTick();
                        }
                    }
                }
            });
        }
    }

    private long getBytes(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Buffer) {
            return ((Buffer) obj).length();
        } else if (obj instanceof ByteBuf) {
            return ((ByteBuf) obj).readableBytes();
        } else if (obj instanceof HttpContent) {
            return ((HttpContent) obj).content().readableBytes();
        } else if (obj instanceof FileRegion) {
            return ((FileRegion) obj).count();
        } else if (obj instanceof ChunkedFile) {
            ChunkedFile file = (ChunkedFile) obj;
            return file.endOffset() - file.startOffset();
        } else {
            return -1;
        }
    }
}
