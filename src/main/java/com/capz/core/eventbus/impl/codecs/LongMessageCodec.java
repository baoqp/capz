package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;

public class LongMessageCodec implements MessageCodec<Long, Long> {

    @Override
    public void encodeToWire(Buffer buffer, Long l) {
        buffer.appendLong(l);
    }

    @Override
    public Long decodeFromWire(int pos, Buffer buffer) {
        return buffer.getLong(pos);
    }

    @Override
    public Long transform(Long l) {
        // Longs are immutable so just return it
        return l;
    }

    @Override
    public String name() {
        return "long";
    }

    @Override
    public byte systemCodecID() {
        return 6;
    }
}
