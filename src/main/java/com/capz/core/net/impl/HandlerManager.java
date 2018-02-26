package com.capz.core.net.impl;

import com.capz.core.impl.AbstractContext;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class HandlerManager<T> {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(HandlerManager.class);

    private final CapzEventLoopGroup availableWorkers;
    private final ConcurrentMap<EventLoop, Handlers<T>> handlerMap = new ConcurrentHashMap<>();

    // We maintain a separate hasHandlers variable so we can implement hasHandlers() efficiently
    // As it is called for every HTTP message received
    private volatile boolean hasHandlers;

    public HandlerManager(CapzEventLoopGroup availableWorkers) {
        this.availableWorkers = availableWorkers;
    }

    public boolean hasHandlers() {
        return hasHandlers;
    }

    public HandlerHolder<T> chooseHandler(EventLoop worker) {
        Handlers<T> handlers = handlerMap.get(worker);
        return handlers == null ? null : handlers.chooseHandler();
    }

    public synchronized void addHandler(T handler, AbstractContext context) {
        EventLoop worker = context.nettyEventLoop();
        availableWorkers.addWorker(worker);
        Handlers<T> handlers = new Handlers<>();
        Handlers<T> prev = handlerMap.putIfAbsent(worker, handlers);
        if (prev != null) {
            handlers = prev;
        }
        handlers.addHandler(new HandlerHolder<>(context, handler));
        hasHandlers = true;
    }

    public synchronized void removeHandler(T handler, AbstractContext context) {
        EventLoop worker = context.nettyEventLoop();
        Handlers<T> handlers = handlerMap.get(worker);
        if (!handlers.removeHandler(new HandlerHolder(context, handler))) {
            throw new IllegalStateException("Can't find handler");
        }
        if (handlers.isEmpty()) {
            handlerMap.remove(worker);
        }
        if (handlerMap.isEmpty()) {
            hasHandlers = false;
        }
        //Available workers does it's own reference counting -since workers can be shared across different Handlers
        availableWorkers.removeWorker(worker);
    }

    private static final class Handlers<T> {
        private int pos;
        private final List<HandlerHolder<T>> list = new CopyOnWriteArrayList<>();

        HandlerHolder<T> chooseHandler() {
            HandlerHolder<T> handler = list.get(pos);
            pos++;
            checkPos();
            return handler;
        }

        void addHandler(HandlerHolder<T> handler) {
            list.add(handler);
        }

        boolean removeHandler(HandlerHolder<T> handler) {
            if (list.remove(handler)) {
                checkPos();
                return true;
            } else {
                return false;
            }
        }

        boolean isEmpty() {
            return list.isEmpty();
        }

        void checkPos() {
            if (pos == list.size()) {
                pos = 0;
            }
        }
    }

}
