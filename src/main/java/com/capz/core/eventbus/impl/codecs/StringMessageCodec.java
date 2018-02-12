
package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;
import io.netty.util.CharsetUtil;

public class StringMessageCodec implements MessageCodec<String, String> {

    @Override
    public void encodeToWire(Buffer buffer, String s) {
        byte[] strBytes = s.getBytes(CharsetUtil.UTF_8);
        buffer.appendInt(strBytes.length);
        buffer.appendBytes(strBytes);
    }

    @Override
    public String decodeFromWire(int pos, Buffer buffer) {
        int length = buffer.getInt(pos);
        pos += 4;
        byte[] bytes = buffer.getBytes(pos, pos + length);
        return new String(bytes, CharsetUtil.UTF_8);
    }

    @Override
    public String transform(String s) {
        // Strings are immutable so just return it
        return s;
    }

    @Override
    public String name() {
        return "string";
    }

    @Override
    public byte systemCodecID() {
        return 9;
    }
}
