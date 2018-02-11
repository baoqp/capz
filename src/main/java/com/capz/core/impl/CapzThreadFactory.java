package com.capz.core.impl;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class CapzThreadFactory implements ThreadFactory {

    private static final Object FOO = new Object();
    private static Map<CapzThread, Object> weakMap = new WeakHashMap<>();

    private static synchronized void addToMap(CapzThread thread) {
        weakMap.put(thread, FOO);
    }

    private final String prefix;
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private final BlockedThreadChecker checker;
    private final boolean worker;
    private final long maxExecTime;

    CapzThreadFactory(String prefix, BlockedThreadChecker checker, boolean worker, long maxExecTime) {
        this.prefix = prefix;
        this.checker = checker;
        this.worker = worker;
        this.maxExecTime = maxExecTime;
    }

    public Thread newThread(Runnable runnable) {
        CapzThread t = new CapzThread(runnable, prefix + threadCount.getAndIncrement(), worker, maxExecTime);
        if (checker != null) {
            checker.registerThread(t);
        }
        addToMap(t);
        // we want to prevent the JVM from exiting until Vert.x instances are closed
        t.setDaemon(false);
        return t;
    }
}
