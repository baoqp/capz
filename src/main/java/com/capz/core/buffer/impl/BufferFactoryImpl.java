package com.capz.core.buffer.impl;

import com.capz.core.buffer.Buffer;
import com.capz.core.spi.BufferFactory;
import io.netty.buffer.ByteBuf;

public class BufferFactoryImpl implements BufferFactory {

    @Override
    public Buffer buffer(int initialSizeHint) {
        return new BufferImpl(initialSizeHint);
    }

    @Override
    public Buffer buffer() {
        return new BufferImpl();
    }

    @Override
    public Buffer buffer(String str) {
        return new BufferImpl(str);
    }

    @Override
    public Buffer buffer(String str, String enc) {
        return new BufferImpl(str, enc);
    }

    @Override
    public Buffer buffer(byte[] bytes) {
        return new BufferImpl(bytes);
    }

    @Override
    public Buffer buffer(ByteBuf byteBuffer) {
        return new BufferImpl(byteBuffer);
    }
}
