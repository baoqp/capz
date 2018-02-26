package com.capz.core.http.impl;

import com.capz.core.ContextTask;
import com.capz.core.Handler;
import com.capz.core.impl.AbstractContext;
import com.capz.core.net.impl.ConnectionBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;


public abstract class CapzHandler<C extends ConnectionBase> extends ChannelDuplexHandler {

    private C conn;
    private ContextTask endReadAndFlush;
    private Handler<C> addHandler;
    private Handler<C> removeHandler;

    /**
     * Set the connection, this is usually called by subclasses when the channel is added to the pipeline.
     */
    protected void setConnection(C connection) {
        conn = connection;
        endReadAndFlush = conn::endReadAndFlush;
        if (addHandler != null) {
            addHandler.handle(connection);
        }
    }

    /**
     * Set an handler to be called when the connection is set on this handler.
     */
    public CapzHandler<C> addHandler(Handler<C> handler) {
        this.addHandler = handler;
        return this;
    }

    /**
     * Set an handler to be called when the connection is unset from this handler.
     */
    public CapzHandler<C> removeHandler(Handler<C> handler) {
        this.removeHandler = handler;
        return this;
    }

    public C getConnection() {
        return conn;
    }

    public static ByteBuf safeBuffer(ByteBuf buf, ByteBufAllocator allocator) {
        if (buf == Unpooled.EMPTY_BUFFER) {
            return buf;
        }
        if (buf.isDirect() || buf instanceof CompositeByteBuf) {
            try {
                if (buf.isReadable()) {
                    ByteBuf buffer = allocator.heapBuffer(buf.readableBytes());
                    buffer.writeBytes(buf);
                    return buffer;
                } else {
                    return Unpooled.EMPTY_BUFFER;
                }
            } finally {
                buf.release();
            }
        }
        return buf;
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        C conn = getConnection();
        AbstractContext context = conn.getContext();
        context.executeFromIO(conn::handleInterestedOpsChanged);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext chctx, final Throwable t) throws Exception {
        Channel ch = chctx.channel();
        // Don't remove the connection at this point, or the handleClosed won't be called when channelInactive is called!
        C connection = getConnection();
        if (connection != null) {
            AbstractContext context = conn.getContext();
            context.executeFromIO(() -> {
                try {
                    if (ch.isOpen()) {
                        ch.close();
                    }
                } catch (Throwable ignore) {
                }
                connection.handleException(t);
            });
        } else {
            ch.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext chctx) throws Exception {
        if (removeHandler != null) {
            removeHandler.handle(conn);
        }
        AbstractContext context = conn.getContext();
        context.executeFromIO(conn::handleClosed);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        AbstractContext context = conn.getContext();
        context.executeFromIO(endReadAndFlush);
    }

    @Override
    public void channelRead(ChannelHandlerContext chctx, Object msg) throws Exception {
        Object message = decode(msg, chctx.alloc());
        AbstractContext context;
        context = conn.getContext();
        context.executeFromIO(() -> {
            conn.startRead();
            handleMessage(conn, context, chctx, message);
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent && ((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
            ctx.close();
        }
        ctx.fireUserEventTriggered(evt);
    }

    protected abstract void handleMessage(C connection, AbstractContext context, ChannelHandlerContext chctx, Object msg) throws Exception;


    protected abstract Object decode(Object msg, ByteBufAllocator allocator) throws Exception;
}
