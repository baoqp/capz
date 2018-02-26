package com.capz.core.net.impl;

import com.capz.core.AsyncResult;
import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.impl.AbstractContext;
import com.capz.core.impl.Future;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.handler.stream.ChunkedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

/**
 * Abstract base class for TCP connections.
 * <p>
 * This class is optimised for performance when used on the same event loop. However it can be used safely from other threads.
 * <p>
 * The internal state is protected using the synchronized keyword. If always used on the same event loop, then
 * we benefit from biased locking which makes the overhead of synchronized near zero.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class ConnectionBase {

    private static final Logger log = LoggerFactory.getLogger(ConnectionBase.class);

    protected final CapzInternal capz;
    protected final ChannelHandlerContext chctx;
    protected final AbstractContext context;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> closeHandler;
    private boolean read;
    private boolean needsFlush;
    private int writeInProgress;
    private Object metric;

    protected ConnectionBase(CapzInternal capz, ChannelHandlerContext chctx, AbstractContext context) {
        this.capz = capz;
        this.chctx = chctx;
        this.context = context;
    }

    /**
     * Encode to message before writing to the channel
     *
     * @param obj the object to encode
     * @return the encoded message
     */
    protected Object encode(Object obj) {
        return obj;
    }

    public synchronized final void startRead() {
        checkContext();
        read = true;
    }

    public synchronized final void endReadAndFlush() {
        if (read) {
            read = false;
            if (needsFlush && writeInProgress == 0) {
                needsFlush = false;
                chctx.flush();
            }
        }
    }

    private void write(Object msg, ChannelPromise promise) {
        msg = encode(msg);
        if (read || writeInProgress > 0) {
            needsFlush = true;
            chctx.write(msg, promise);
        } else {
            needsFlush = false;
            chctx.writeAndFlush(msg, promise);
        }
    }

    public synchronized void writeToChannel(Object msg, ChannelPromise promise) {
        // Make sure we serialize all the messages as this method can be called from various threads:
        // two "sequential" calls to writeToChannel (we can say that as it is synchronized) should preserve
        // the message order independently of the thread. To achieve this we need to reschedule messages
        // not on the event loop or if there are pending async message for the channel.
        if (chctx.executor().inEventLoop() && writeInProgress == 0) {
            write(msg, promise);
        } else {
            queueForWrite(msg, promise);
        }
    }

    private void queueForWrite(Object msg, ChannelPromise promise) {
        writeInProgress++;
        context.runOnContext(v -> {
            synchronized (ConnectionBase.this) {
                writeInProgress--;
                write(msg, promise);
            }
        });
    }

    public void writeToChannel(Object obj) {
        writeToChannel(obj, chctx.voidPromise());
    }

    // This is a volatile read inside the Netty channel implementation
    public boolean isNotWritable() {
        return !chctx.channel().isWritable();
    }

    /**
     * Close the connection
     */
    public void close() {
        // make sure everything is flushed out on close
        endReadAndFlush();
        chctx.channel().close();
    }

    public synchronized ConnectionBase closeHandler(Handler<Void> handler) {
        closeHandler = handler;
        return this;
    }

    public synchronized ConnectionBase exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    protected synchronized Handler<Throwable> exceptionHandler() {
        return exceptionHandler;
    }

    public void doPause() {
        chctx.channel().config().setAutoRead(false);
    }

    public void doResume() {
        chctx.channel().config().setAutoRead(true);
    }

    public void doSetWriteQueueMaxSize(int size) {
        ChannelConfig config = chctx.channel().config();
        config.setWriteBufferWaterMark(new WriteBufferWaterMark(size / 2, size));
    }

    protected void checkContext() {
        // Sanity check
        if (context != capz.getContext()) {
            throw new IllegalStateException("Wrong context!");
        }
    }

    /**
     * @return the Netty channel - for internal usage only
     */
    public Channel channel() {
        return chctx.channel();
    }

    public AbstractContext getContext() {
        return context;
    }

    public synchronized void metric(Object metric) {
        this.metric = metric;
    }

    public synchronized Object metric() {
        return metric;
    }


    public synchronized void handleException(Throwable t) {

        if (exceptionHandler != null) {
            exceptionHandler.handle(t);
        } else {
            log.error("Unhandled exception", t);
        }
    }

    public synchronized void handleClosed() {

        if (closeHandler != null) {
            capz.runOnContext(closeHandler);
        }
    }

    public abstract void handleInterestedOpsChanged();

    protected void addFuture(final Handler<AsyncResult<Void>> completionHandler, final ChannelFuture future) {
        if (future != null) {
            future.addListener(channelFuture -> context.executeFromIO(() -> {
                if (completionHandler != null) {
                    if (channelFuture.isSuccess()) {
                        completionHandler.handle(Future.succeededFuture());
                    } else {
                        completionHandler.handle(Future.failedFuture(channelFuture.cause()));
                    }
                } else if (!channelFuture.isSuccess()) {
                    handleException(channelFuture.cause());
                }
            }));
        }
    }


    protected ChannelFuture sendFile(RandomAccessFile raf, long offset, long length) throws IOException {
        // Write the content.
        ChannelPromise writeFuture = chctx.newPromise();

        writeToChannel(new ChunkedFile(raf, offset, length, 8192), writeFuture);

        if (writeFuture != null) {
            writeFuture.addListener(fut -> raf.close());
        } else {
            raf.close();
        }
        return writeFuture;
    }


    public ChannelPromise channelFuture() {
        return chctx.newPromise();
    }

    public String remoteName() {
        InetSocketAddress addr = (InetSocketAddress) chctx.channel().remoteAddress();
        if (addr == null) return null;
        // Use hostString that does not trigger a DNS resolution
        return addr.getHostString();
    }

}
