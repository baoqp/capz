package com.capz.core.impl;

import com.capz.core.Exception.CapzException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

// 检查线程执行时间，如果时间太长则可能是阻塞住了
public class BlockedThreadChecker {

    private static final Logger log = LoggerFactory.getLogger(BlockedThreadChecker.class);

    private static final Object O = new Object(); //占位用
    private final Map<CapzThread, Object> threads = new WeakHashMap<>();
    private final Timer timer; // Need to use our own timer - can't use event loop for this

    BlockedThreadChecker(long interval, final long warningExceptionTime) {
        timer = new Timer("vertx-blocked-thread-checker", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (BlockedThreadChecker.this) {
                    long now = System.nanoTime();
                    for (CapzThread thread : threads.keySet()) {
                        long execStart = thread.startTime();
                        long dur = now - execStart;
                        final long timeLimit = thread.getMaxExecTime();
                        if (execStart != 0 && dur > timeLimit) {
                            final String message = "Thread " + thread + " has been blocked for " + (dur / 1000000) + " ms, time limit is " + (timeLimit / 1000000);
                            if (dur <= warningExceptionTime) {
                                log.warn(message);
                            } else {
                                CapzException stackTrace = new CapzException("Thread blocked");
                                stackTrace.setStackTrace(thread.getStackTrace());
                                log.warn(message, stackTrace);
                            }
                        }
                    }
                }
            }
        }, interval, interval);
    }

    public synchronized void registerThread(CapzThread thread) {
        threads.put(thread, O);
    }

    public void close() {
        timer.cancel();
    }
}
