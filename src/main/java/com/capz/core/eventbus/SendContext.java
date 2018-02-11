package com.capz.core.eventbus;


public interface SendContext<T> {

    Message<T> message();

    void next();

    /**
     * true if the message is being sent (point to point) or False if the message is being published
     */
    boolean send();

    Object sentBody();
}
