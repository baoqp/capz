package com.capz.core.http.impl;

import com.capz.core.net.impl.ConnectionBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;


public abstract class CapzHttpHandler<C extends ConnectionBase> extends CapzHandler<C> {

    private static ByteBuf safeBuffer(ByteBufHolder holder, ByteBufAllocator allocator) {
        return safeBuffer(holder.content(), allocator);
    }

    @Override
    protected Object decode(Object msg, ByteBufAllocator allocator) {
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf buf = content.content();
            if (buf != Unpooled.EMPTY_BUFFER && buf.isDirect()) {
                ByteBuf newBuf = safeBuffer(content, allocator);
                if (msg instanceof LastHttpContent) {
                    LastHttpContent last = (LastHttpContent) msg;
                    return new AssembledLastHttpContent(newBuf, last.trailingHeaders(), last.getDecoderResult());
                } else {
                    return new DefaultHttpContent(newBuf);
                }
            }
        }
        return msg;
    }

}
