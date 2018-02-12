package com.capz.core.eventbus;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.capz.core.utils.StringUtil;
import lombok.Getter;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;


@Getter
public class DeliveryOptions {

    public static final long DEFAULT_TIMEOUT = 30 * 1000;

    private long timeout = DEFAULT_TIMEOUT;
    private String codecName;
    private Map<String, HashSet<String>> headers;


    public DeliveryOptions() {
    }


    public DeliveryOptions(DeliveryOptions other) {
        this.timeout = other.getTimeout();
        this.codecName = other.getCodecName();
        this.headers = other.getHeaders();
    }

    // Create a delivery options from JSON
    public DeliveryOptions(JSONObject json) {
        Long timeout = json.getLong("timeout");
        if (timeout != null) {
            this.timeout = timeout;
        }
        this.codecName = json.getString("codecName");
        String hdrsStr = json.getString("headers");

        if (!StringUtil.isEmpty(hdrsStr)) {
            this.headers = JSON.parseObject(hdrsStr, Map.class); // TODO
        }
    }


    public DeliveryOptions setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public DeliveryOptions setCodecName(String codecName) {
        this.codecName = codecName;
        return this;
    }

    public DeliveryOptions setHeaders(Map headers) {
        this.headers = headers;
        return this;
    }

    public DeliveryOptions addHeader(String key, String value) {
        Objects.requireNonNull(key, "no null key accepted");
        Objects.requireNonNull(value, "no null value accepted");

        HashSet<String> header;
        if ((header = headers.get(key)) == null) {
            header = new HashSet<>();
            headers.put(key, header);
        }
        header.add(value);

        return this;
    }

}
