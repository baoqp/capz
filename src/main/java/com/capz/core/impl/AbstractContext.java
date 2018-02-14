package com.capz.core.impl;

import com.capz.core.*;
import com.sun.corba.se.impl.corba.ContextImpl;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractContext implements ContextInternal {

    private static final Logger log = LoggerFactory.getLogger(AbstractContext.class);

    private static EventLoop getEventLoop(CapzInternal capzInternal) {
        EventLoopGroup group = capzInternal.getEventLoopGroup();
        if (group != null) {
            return group.next();
        } else {
            return null;
        }
    }

    private static final String THREAD_CHECKS_PROP_NAME = "capz.threadChecks";
    private static final String DISABLE_TIMINGS_PROP_NAME = "capz.disableContextTimings";
    private static final String DISABLE_TCCL_PROP_NAME = "capz.disableTCCL";
    private static final boolean THREAD_CHECKS = Boolean.getBoolean(THREAD_CHECKS_PROP_NAME);
    private static final boolean DISABLE_TIMINGS = Boolean.getBoolean(DISABLE_TIMINGS_PROP_NAME);
    private static final boolean DISABLE_TCCL = Boolean.getBoolean(DISABLE_TCCL_PROP_NAME);

    protected final CapzInternal owner;
    protected final String deploymentID;
    @Getter
    @Setter
    private Deployment deployment;

    private final ClassLoader tccl;
    private final EventLoop eventLoop;
    protected CapzThread contextThread;
    private ConcurrentMap<Object, Object> contextData;
    private volatile Handler<Throwable> exceptionHandler;
    protected final WorkerExecutor workerPool;
    protected final WorkerExecutor internalBlockingPool;
    final TaskQueue orderedTasks;
    protected final TaskQueue internalOrderedTasks;

    protected AbstractContext(CapzInternal capzInternal, WorkerExecutor internalBlockingPool,
                              WorkerExecutor workerPool, String deploymentID, ClassLoader tccl) {
        this(capzInternal, getEventLoop(capzInternal), internalBlockingPool, workerPool, deploymentID, tccl);
    }

    protected AbstractContext(CapzInternal capzInternal, EventLoop eventLoop, WorkerExecutor internalBlockingPool,
                              WorkerExecutor workerPool, String deploymentID, ClassLoader tccl) {
        if (DISABLE_TCCL && !tccl.getClass().getName().equals("sun.misc.Launcher$AppClassLoader")) {
            log.warn("You have disabled TCCL checks but you have a custom TCCL to set.");
        }
        this.deploymentID = deploymentID;
        this.eventLoop = eventLoop;
        this.tccl = tccl;
        this.owner = capzInternal;
        this.workerPool = workerPool;
        this.internalBlockingPool = internalBlockingPool;
        this.orderedTasks = new TaskQueue();
        this.internalOrderedTasks = new TaskQueue();
    }

    public static void setContext(AbstractContext context) {
        Thread current = Thread.currentThread();
        if (current instanceof CapzThread) {
            setContext((CapzThread) current, context);
        } else {
            throw new IllegalStateException("Attempt to setContext on non Capz thread " + Thread.currentThread());
        }
    }

    private static void setContext(CapzThread thread, AbstractContext context) {
        thread.setContext(context);
        if (!DISABLE_TCCL) {
            if (context != null) {
                context.setTCCL();
            } else {
                Thread.currentThread().setContextClassLoader(null);
            }
        }
    }


    protected abstract void executeAsync(Handler<Void> task);

    @Override
    public abstract boolean isEventLoopContext();

    @Override
    public abstract boolean isMultiThreadedWorkerContext();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) contextData().get(key);
    }

    @Override
    public void put(String key, Object value) {
        contextData().put(key, value);
    }

    @Override
    public boolean remove(String key) {
        return contextData().remove(key) != null;
    }

    @Override
    public boolean isWorkerContext() {
        return !isEventLoopContext();
    }

    public static boolean isOnWorkerThread() {
        return isOnCapzThread(true);
    }

    public static boolean isOnEventLoopThread() {
        return isOnCapzThread(false);
    }

    public static boolean isOnCapzThread() {
        Thread t = Thread.currentThread();
        return (t instanceof CapzThread);
    }

    private static boolean isOnCapzThread(boolean worker) {
        Thread t = Thread.currentThread();
        if (t instanceof CapzThread) {
            CapzThread vt = (CapzThread) t;
            return vt.isWorker() == worker;
        }
        return false;
    }

    // This is called to execute code where the origin is IO (from Netty probably).
    // In such a case we should already be on an event loop thread (as Netty manages the event loops)
    // but check this anyway, then execute directly
    public void executeFromIO(ContextTask task) {
        if (THREAD_CHECKS) {
            checkCorrectThread();
        }

        wrapTask(task, null, true).run();
    }

    protected abstract void checkCorrectThread();

    // Run the task asynchronously on this same context
    @Override
    public void runOnContext(Handler<Void> task) {
        try {
            executeAsync(task);
        } catch (RejectedExecutionException ignore) {
            // Pool is already shut down
        }
    }

    @Override
    public String deploymentID() {
        return deploymentID;
    }


    public EventLoop nettyEventLoop() {
        return eventLoop;
    }

    public CapzInternal owner() {
        return owner;
    }

    // Execute an internal task on the internal blocking ordered executor
    public <T> void executeBlocking(Action<T> action, Handler<AsyncResult<T>> resultHandler) {
        executeBlocking(action, null, resultHandler,
                internalBlockingPool.getExecutorService(), internalOrderedTasks);
    }

    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler,
                                    Handler<AsyncResult<T>> resultHandler) {

        executeBlocking(blockingCodeHandler, true, resultHandler);
    }

    @Override
    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered,
                                    Handler<AsyncResult<T>> resultHandler) {

        executeBlocking(null, blockingCodeHandler, resultHandler,
                workerPool.getExecutorService(), ordered ? orderedTasks : null);
    }


    @Override
    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, TaskQueue queue,
                                    Handler<AsyncResult<T>> resultHandler) {

        executeBlocking(null, blockingCodeHandler, resultHandler,
                workerPool.getExecutorService(), queue);
    }

    <T> void executeBlocking(Action<T> action, Handler<Future<T>> blockingCodeHandler,
                             Handler<AsyncResult<T>> resultHandler,
                             Executor exec, TaskQueue queue) {
        try {
            Runnable command = () -> {
                CapzThread current = (CapzThread) Thread.currentThread();

                if (!DISABLE_TIMINGS) {
                    current.executeStart();
                }
                Future<T> res = Future.future();
                try {
                    if (blockingCodeHandler != null) {
                        AbstractContext.setContext(this);
                        blockingCodeHandler.handle(res);
                    } else {
                        T result = action.perform();
                        res.complete(result);
                    }
                } catch (Throwable e) {
                    res.fail(e);
                } finally {
                    if (!DISABLE_TIMINGS) {
                        current.executeEnd();
                    }
                }

                if (resultHandler != null) {
                    runOnContext(v -> res.setHandler(resultHandler));
                }
            };

            if (queue != null) {
                queue.execute(command, exec);
            } else {
                exec.execute(command);
            }
        } catch (RejectedExecutionException e) {

            throw e;
        }
    }

    public synchronized ConcurrentMap<Object, Object> contextData() {
        if (contextData == null) {
            contextData = new ConcurrentHashMap<>();
        }
        return contextData;
    }

    protected Runnable wrapTask(ContextTask cTask, Handler<Void> hTask, boolean checkThread) {

        return () -> {
            Thread th = Thread.currentThread();
            if (!(th instanceof CapzThread)) {
                throw new IllegalStateException(
                        "Uh oh! Event loop context executing with wrong thread! Expected "
                                + contextThread + " got " + th);
            }
            CapzThread current = (CapzThread) th;
            if (THREAD_CHECKS && checkThread) {
                if (contextThread == null) {
                    contextThread = current;
                } else if (contextThread != current && !contextThread.isWorker()) {
                    throw new IllegalStateException(
                            "Uh oh! Event loop context executing with wrong thread! Expected "
                                    + contextThread + " got " + current);
                }
            }

            if (!DISABLE_TIMINGS) {
                current.executeStart();
            }
            try {
                setContext(current, AbstractContext.this);
                if (cTask != null) {
                    cTask.run();
                } else {
                    hTask.handle(null);
                }

            } catch (Throwable t) {
                log.error("Unhandled exception", t);
                Handler<Throwable> handler = this.exceptionHandler;
                if (handler == null) {
                    handler = owner.exceptionHandler();
                }
                if (handler != null) {
                    handler.handle(t);
                }

            } finally {
                // We don't unset the context after execution - this is done later when the context is closed via
                // CapzThreadFactory
                if (!DISABLE_TIMINGS) {
                    current.executeEnd();
                }
            }
        };
    }

    private void setTCCL() {
        Thread.currentThread().setContextClassLoader(tccl);
    }

    @Override
    public Context exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    public Handler<Throwable> exceptionHandler() {
        return exceptionHandler;
    }

}

