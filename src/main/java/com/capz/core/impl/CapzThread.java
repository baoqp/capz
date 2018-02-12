package com.capz.core.impl;

import io.netty.util.concurrent.FastThreadLocalThread;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Bao Qingping
 */
@Setter
@Getter
public class CapzThread extends FastThreadLocalThread {

    private final boolean worker;
    private final long maxExecTime;
    private long execStart;
    private AbstractContext context;

    public CapzThread(Runnable target, String name, boolean worker, long maxExecTime) {
        super(target, name);
        this.worker = worker;
        this.maxExecTime = maxExecTime;
    }

    public final void executeStart() {
        execStart = System.nanoTime();
    }

    public final void executeEnd() {
        execStart = 0;
    }


    public long startTime() {
        return execStart;
    }

}