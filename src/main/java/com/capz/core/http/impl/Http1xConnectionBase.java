package com.capz.core.http.impl;

import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.impl.AbstractContext;
import com.capz.core.net.impl.ConnectionBase;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

abstract class Http1xConnectionBase extends ConnectionBase implements com.capz.core.http.HttpConnection {

    Http1xConnectionBase(CapzInternal capz, ChannelHandlerContext chctx, AbstractContext context) {
        super(capz, chctx, context);
    }

    abstract public void closeWithPayload(ByteBuf byteBuf);

    @Override
    public Http1xConnectionBase closeHandler(Handler<Void> handler) {
        return (Http1xConnectionBase) super.closeHandler(handler);
    }

    @Override
    public Http1xConnectionBase exceptionHandler(Handler<Throwable> handler) {
        return (Http1xConnectionBase) super.exceptionHandler(handler);
    }

}
