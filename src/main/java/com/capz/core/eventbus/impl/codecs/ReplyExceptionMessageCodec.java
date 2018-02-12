package com.capz.core.eventbus.impl.codecs;

import com.capz.core.Exception.ReplyException;
import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;
import com.capz.core.eventbus.ReplyFailure;
import io.netty.util.CharsetUtil;


public class ReplyExceptionMessageCodec implements MessageCodec<ReplyException, ReplyException> {

    @Override
    public void encodeToWire(Buffer buffer, ReplyException body) {
        buffer.appendByte((byte) body.failureType().toInt());
        buffer.appendInt(body.failureCode());
        if (body.getMessage() == null) {
            buffer.appendByte((byte) 0);
        } else {
            buffer.appendByte((byte) 1);
            byte[] encoded = body.getMessage().getBytes(CharsetUtil.UTF_8);
            buffer.appendInt(encoded.length);
            buffer.appendBytes(encoded);
        }
    }

    @Override
    public ReplyException decodeFromWire(int pos, Buffer buffer) {
        int i = (int) buffer.getByte(pos);
        ReplyFailure rf = ReplyFailure.fromInt(i);
        pos++;
        int failureCode = buffer.getInt(pos);
        pos += 4;
        boolean isNull = buffer.getByte(pos) == (byte) 0;
        String message;
        if (!isNull) {
            pos++;
            int strLength = buffer.getInt(pos);
            pos += 4;
            byte[] bytes = buffer.getBytes(pos, pos + strLength);
            message = new String(bytes, CharsetUtil.UTF_8);
        } else {
            message = null;
        }
        return new ReplyException(rf, failureCode, message);
    }

    @Override
    public ReplyException transform(ReplyException exception) {
        return exception;
    }

    @Override
    public String name() {
        return "replyexception";
    }

    @Override
    public byte systemCodecID() {
        return 15;
    }
}
