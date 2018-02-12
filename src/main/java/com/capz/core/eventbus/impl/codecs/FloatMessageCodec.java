package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;


public class FloatMessageCodec implements MessageCodec<Float, Float> {

    @Override
    public void encodeToWire(Buffer buffer, Float f) {
        buffer.appendFloat(f);
    }

    @Override
    public Float decodeFromWire(int pos, Buffer buffer) {
        return buffer.getFloat(pos);
    }

    @Override
    public Float transform(Float f) {
        // Floats are immutable so just return it
        return f;
    }

    @Override
    public String name() {
        return "float";
    }

    @Override
    public byte systemCodecID() {
        return 7;
    }
}
