package com.capz.core.eventbus.impl.codecs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.capz.core.buffer.Buffer;
import com.capz.core.eventbus.MessageCodec;
import io.netty.util.CharsetUtil;

/**
 * @author Bao Qingping
 */
public class JSONObjectMessageCodec implements MessageCodec<JSONObject, JSONObject> {

    // 先转json字符串，再序列化
    @Override
    public void encodeToWire(Buffer buffer, JSONObject jsonObject) {
        String jsonStr = jsonObject.toString();
        byte[] strBytes = jsonStr.getBytes(CharsetUtil.UTF_8);
        buffer.appendInt(strBytes.length);
        buffer.appendBytes(strBytes);
    }

    @Override
    public JSONObject decodeFromWire(int pos, Buffer buffer) {
        int length = buffer.getInt(pos);
        pos += 4;
        byte[] bytes = buffer.getBytes(pos, pos + length);
        String jsonStr =  new String(bytes, CharsetUtil.UTF_8);
        return JSON.parseObject(jsonStr);
    }

    @Override
    public JSONObject transform(JSONObject jsonObject) {
        return (JSONObject)jsonObject.clone();
    }

    @Override
    public String name() {
        return "jsonobject";
    }

    @Override
    public byte systemCodecID() {
        return 13;
    }
}
