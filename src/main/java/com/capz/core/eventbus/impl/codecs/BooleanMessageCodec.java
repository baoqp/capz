
package com.capz.core.eventbus.impl.codecs;

import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;

public class BooleanMessageCodec implements MessageCodec<Boolean, Boolean> {

    @Override
    public void encodeToWire(Buffer buffer, Boolean b) {
        buffer.appendByte((byte) (b ? 0 : 1));
    }

    @Override
    public Boolean decodeFromWire(int pos, Buffer buffer) {
        return buffer.getByte(pos) == 0;
    }

    @Override
    public Boolean transform(Boolean b) {
        // Booleans are immutable so just return it
        return b;
    }

    @Override
    public String name() {
        return "boolean";
    }

    @Override
    public byte systemCodecID() {
        return 3;
    }
}
