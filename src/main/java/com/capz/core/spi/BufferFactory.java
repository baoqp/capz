package com.capz.core.spi;

import com.capz.core.buffer.Buffer;
import io.netty.buffer.ByteBuf;

public interface BufferFactory {

    Buffer buffer(int initialSizeHint);

    Buffer buffer();

    Buffer buffer(String str);

    Buffer buffer(String str, String enc);

    Buffer buffer(byte[] bytes);

    Buffer buffer(ByteBuf byteBuffer);
}
