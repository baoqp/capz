package com.capz.core.eventbus.impl.codecs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;
import io.netty.util.CharsetUtil;

/**
 * @author Bao Qingping
 */
public class JSONArrayMessageCodec  implements MessageCodec<JSONArray, JSONArray> {
    @Override
    public void encodeToWire(Buffer buffer, JSONArray jsonArray) {
        String jsonStr = jsonArray.toString();
        byte[] strBytes = jsonStr.getBytes(CharsetUtil.UTF_8);
        buffer.appendInt(strBytes.length);
        buffer.appendBytes(strBytes);
    }

    @Override
    public JSONArray decodeFromWire(int pos, Buffer buffer) {
        int length = buffer.getInt(pos);
        pos += 4;
        byte[] bytes = buffer.getBytes(pos, pos + length);
        String jsonStr =  new String(bytes, CharsetUtil.UTF_8);
        return JSON.parseArray(jsonStr);
    }

    @Override
    public JSONArray transform(JSONArray jsonArray) {
        return (JSONArray)jsonArray.clone();
    }

    @Override
    public String name() {
        return "jsonarray";
    }

    @Override
    public byte systemCodecID() {
        return 14;
    }
}
