package com.capz.core.net.impl;

import com.capz.core.impl.AbstractContext;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class HandlerHolder<T> {

    public final AbstractContext context;
    public final T handler;

    public HandlerHolder(AbstractContext context, T handler) {
        this.context = context;
        this.handler = handler;
    }

}