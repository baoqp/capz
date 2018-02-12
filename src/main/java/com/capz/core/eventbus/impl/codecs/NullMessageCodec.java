package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;


public class NullMessageCodec implements MessageCodec<String, String> {

    @Override
    public void encodeToWire(Buffer buffer, String s) {
    }

    @Override
    public String decodeFromWire(int pos, Buffer buffer) {
        return null;
    }

    @Override
    public String transform(String s) {
        return null;
    }

    @Override
    public String name() {
        return "null";
    }

    @Override
    public byte systemCodecID() {
        return 0;
    }
}
