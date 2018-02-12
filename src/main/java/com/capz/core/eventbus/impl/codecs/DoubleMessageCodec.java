package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;


public class DoubleMessageCodec implements MessageCodec<Double, Double> {

    @Override
    public void encodeToWire(Buffer buffer, Double d) {
        buffer.appendDouble(d);
    }

    @Override
    public Double decodeFromWire(int pos, Buffer buffer) {
        return buffer.getDouble(pos);
    }

    @Override
    public Double transform(Double d) {
        // Doubles are immutable so just return it
        return d;
    }

    @Override
    public String name() {
        return "double";
    }

    @Override
    public byte systemCodecID() {
        return 8;
    }
}
