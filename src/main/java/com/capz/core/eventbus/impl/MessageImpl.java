package com.capz.core.eventbus.impl;

import com.capz.core.AsyncResult;
import com.capz.core.eventbus.Message;
import com.capz.core.eventbus.MessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;

public class MessageImpl<U, V> implements Message<V> {

    private static final Logger log = LoggerFactory.getLogger(MessageImpl.class);

    protected MessageCodec<U, V> messageCodec;
    protected EventBusImpl bus;
    protected String address;
    protected String replyAddress;
    protected Map<String, HashSet<String>> headers;
    protected U sentBody;
    protected V receivedBody;
    protected boolean send;

    public MessageImpl() {
    }

    public MessageImpl(String address, String replyAddress, Map headers, U sentBody,
                       MessageCodec<U, V> messageCodec,
                       boolean send, EventBusImpl bus) {
        this.messageCodec = messageCodec;
        this.address = address;
        this.replyAddress = replyAddress;
        this.headers = headers;
        this.sentBody = sentBody;
        this.send = send;
        this.bus = bus;
    }

    protected MessageImpl(MessageImpl<U, V> other) {
        this.bus = other.bus;
        this.address = other.address;
        this.replyAddress = other.replyAddress;
        this.messageCodec = other.messageCodec;
        this.headers = other.headers;
        if (other.sentBody != null) {
            this.sentBody = other.sentBody;
            this.receivedBody = messageCodec.transform(other.sentBody);
        }
        this.send = other.send;
    }

    public MessageImpl<U, V> copyBeforeReceive() {
        return new MessageImpl<>(this);
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public Map headers() {
        return headers;
    }

    @Override
    public V body() {
        if (receivedBody == null && sentBody != null) {
            receivedBody = messageCodec.transform(sentBody);
        }
        return receivedBody;
    }

    @Override
    public String replyAddress() {
        return replyAddress;
    }

    @Override
    public void fail(int failureCode, String message) {
        if (replyAddress != null) {
            sendReply(bus.createMessage(true, replyAddress, null,
                    new ReplyException(ReplyFailure.RECIPIENT_FAILURE, failureCode, message), null), null, null);
        }
    }

    @Override
    public void reply(Object message) {
        reply(message, new DeliveryOptions(), null);
    }

    @Override
    public <R> void reply(Object message, Handler<AsyncResult<Message<R>>> replyHandler) {
        reply(message, new DeliveryOptions(), replyHandler);
    }

    @Override
    public void reply(Object message, DeliveryOptions options) {
        reply(message, options, null);
    }

    @Override
    public <R> void reply(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
        if (replyAddress != null) {
            sendReply(bus.createMessage(true, replyAddress, options.getHeaders(), message, options.getCodecName()), options, replyHandler);
        }
    }

    @Override
    public boolean isSend() {
        return send;
    }

    public void setReplyAddress(String replyAddress) {
        this.replyAddress = replyAddress;
    }

    public MessageCodec<U, V> codec() {
        return messageCodec;
    }

    public void setBus(EventBusImpl bus) {
        this.bus = bus;
    }

    protected <R> void sendReply(MessageImpl msg, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
        if (bus != null) {
            bus.sendReply(msg, this, options, replyHandler);
        }
    }

    protected boolean isLocal() {
        return true;
    }
}