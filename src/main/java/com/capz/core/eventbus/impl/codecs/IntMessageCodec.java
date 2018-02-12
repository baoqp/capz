package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;


public class IntMessageCodec implements MessageCodec<Integer, Integer> {

    @Override
    public void encodeToWire(Buffer buffer, Integer i) {
        buffer.appendInt(i);
    }

    @Override
    public Integer decodeFromWire(int pos, Buffer buffer) {
        return buffer.getInt(pos);
    }

    @Override
    public Integer transform(Integer i) {
        // Integers are immutable so just return it
        return i;
    }

    @Override
    public String name() {
        return "int";
    }

    @Override
    public byte systemCodecID() {
        return 5;
    }
}
