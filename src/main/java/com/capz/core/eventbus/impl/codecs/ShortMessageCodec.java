package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;


public class ShortMessageCodec implements MessageCodec<Short, Short> {

    @Override
    public void encodeToWire(Buffer buffer, Short s) {
        buffer.appendShort(s);
    }

    @Override
    public Short decodeFromWire(int pos, Buffer buffer) {
        return buffer.getShort(pos);
    }

    @Override
    public Short transform(Short s) {
        // Shorts are immutable so just return it
        return s;
    }

    @Override
    public String name() {
        return "short";
    }

    @Override
    public byte systemCodecID() {
        return 4;
    }
}
