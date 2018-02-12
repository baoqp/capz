package com.capz.core.eventbus.impl;

import com.capz.core.Context;

public class HandlerHolder<T> {

    private final Context context;
    private final HandlerRegistration<T> handler;
    private final boolean replyHandler;
    private final boolean localOnly;
    private boolean removed;

    public HandlerHolder(HandlerRegistration<T> handler, boolean replyHandler, boolean localOnly,
                         Context context) {
        this.context = context;
        this.handler = handler;
        this.replyHandler = replyHandler;
        this.localOnly = localOnly;
    }

    public void setRemoved() {
        synchronized (this) {
            if (!removed) {
                removed = true;
            }
        }
    }

    // Because of biased locks the overhead of the synchronized lock should be very low as it's almost always
    // called by the same event loop
    public synchronized boolean isRemoved() {
        return removed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandlerHolder that = (HandlerHolder) o;
        if (handler != null ? !handler.equals(that.handler) : that.handler != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return handler != null ? handler.hashCode() : 0;
    }

    public Context getContext() {
        return context;
    }

    public HandlerRegistration<T> getHandler() {
        return handler;
    }

    public boolean isReplyHandler() {
        return replyHandler;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    ;
}
