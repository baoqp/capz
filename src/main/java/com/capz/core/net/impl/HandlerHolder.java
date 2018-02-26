package com.capz.core.net.impl;

import com.capz.core.impl.AbstractContext;

public class HandlerHolder<T> {

    public final AbstractContext context;
    public final T handler;

    public HandlerHolder(AbstractContext context, T handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HandlerHolder that = (HandlerHolder) o;

        if (context != that.context) return false;
        if (handler != null ? !handler.equals(that.handler) : that.handler != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = context.hashCode();
        result = 31 * result + handler.hashCode();
        return result;
    }
}