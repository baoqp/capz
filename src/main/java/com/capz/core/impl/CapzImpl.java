package com.capz.core.impl;

import com.capz.core.*;
import com.capz.core.eventbus.EventBus;
import com.capz.core.eventbus.impl.EventBusImpl;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Bao Qingping
 */
public class CapzImpl implements CapzInternal {

    private static final Logger log = LoggerFactory.getLogger(CapzImpl.class);

    private static final String NETTY_IO_RATIO_PROPERTY_NAME = "Capz.nettyIORatio";
    private static final int NETTY_IO_RATIO = Integer.getInteger(NETTY_IO_RATIO_PROPERTY_NAME, 50);

    static {
        // Netty resource leak detection has a performance overhead and we do not need it in Vert.x
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        // Use the JDK deflater/inflater by default
        System.setProperty("io.netty.noJdkZlibDecoder", "false");
    }

    private final ConcurrentMap<Long, InternalTimerHandler> timeouts = new ConcurrentHashMap<>();
    private final AtomicLong timeoutCounter = new AtomicLong(0);
    final WorkerExecutor workerPool;
    final WorkerExecutor internalBlockingPool;
    private final ThreadFactory eventLoopThreadFactory;
    private final NioEventLoopGroup eventLoopGroup;
    private final NioEventLoopGroup acceptorEventLoopGroup;
    private final BlockedThreadChecker checker;
    private EventBus eventBus;
    private boolean closed;
    private volatile Handler<Throwable> exceptionHandler;
    private final Map<String, SharedWorkerPool> namedWorkerPools;
    private final int defaultWorkerPoolSize;
    private final long defaultWorkerMaxExecTime;

    private DeploymentManager deploymentManager;


    public CapzImpl() {
        this(new CapzOptions());
    }

    CapzImpl(CapzOptions options) {
        this(options, null);
    }

    CapzImpl(CapzOptions options, Handler<AsyncResult<Capz>> resultHandler) {

        if (Capz.currentContext() != null) {
            log.warn("You're already on a Vert.x context, are you sure you want to create a new Capz instance?");
        }
        //closeHooks = new CloseHooks(log);
        checker = new BlockedThreadChecker(options.getBlockedThreadCheckInterval(), options.getWarningExceptionTime());
        eventLoopThreadFactory = new CapzThreadFactory("capz-eventloop-thread-", checker, false, options.getMaxEventLoopExecuteTime());
        eventLoopGroup = new NioEventLoopGroup(options.getEventLoopPoolSize(), eventLoopThreadFactory);
        eventLoopGroup.setIoRatio(NETTY_IO_RATIO);
        ThreadFactory acceptorEventLoopThreadFactory = new CapzThreadFactory("capz-acceptor-thread-", checker, false, options.getMaxEventLoopExecuteTime());

        acceptorEventLoopGroup = new NioEventLoopGroup(1, acceptorEventLoopThreadFactory);
        acceptorEventLoopGroup.setIoRatio(100);

        ExecutorService workerExec = Executors.newFixedThreadPool(options.getWorkerPoolSize(),
                new CapzThreadFactory("capz-worker-thread-", checker, true, options.getMaxWorkerExecuteTime()));
        ExecutorService internalBlockingExec = Executors.newFixedThreadPool(options.getInternalBlockingPoolSize(),
                new CapzThreadFactory("capz-internal-blocking-", checker, true, options.getMaxWorkerExecuteTime()));
        internalBlockingPool = new WorkerExecutorImpl(this, internalBlockingExec, true);
        namedWorkerPools = new HashMap<>();
        workerPool = new WorkerExecutorImpl(this, workerExec, true);
        defaultWorkerPoolSize = options.getWorkerPoolSize();
        defaultWorkerMaxExecTime = options.getMaxWorkerExecuteTime();

        deploymentManager = new DeploymentManager(this);

        createAndStartEventBus(options, resultHandler);

        // this.sharedData = new SharedDataImpl(this, clusterManager);
    }

    private void createAndStartEventBus(CapzOptions options, Handler<AsyncResult<Capz>> resultHandler) {
        eventBus = new EventBusImpl(this);

        eventBus.start(ar -> {
            if (ar.succeeded()) {
                if (resultHandler != null) resultHandler.handle(Future.succeededFuture(this));
            } else {
                log.error("Failed to start event bus", ar.cause());
                if (resultHandler != null) resultHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public EventBus eventBus() {
        if (eventBus == null) {
            synchronized (this) {
                return eventBus;
            }
        }
        return eventBus;
    }

    public long setPeriodic(long delay, Handler<Long> handler) {
        return scheduleTimeout(getOrCreateContext(), handler, delay, true);
    }


    public long setTimer(long delay, Handler<Long> handler) {
        return scheduleTimeout(getOrCreateContext(), handler, delay, false);
    }

    public void runOnContext(Handler<Void> task) {
        AbstractContext context = getOrCreateContext();
        context.runOnContext(task);
    }

    // The background pool is used for making blocking calls to legacy synchronous APIs
    public WorkerExecutor getWorkerPool() {
        return workerPool;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public EventLoopGroup getAcceptorEventLoopGroup() {
        return acceptorEventLoopGroup;
    }

    public AbstractContext getOrCreateContext() {
        AbstractContext ctx = getContext();
        if (ctx == null) {
            // We are running embedded - Create a context
            ctx = createEventLoopContext(null, null, Thread.currentThread().getContextClassLoader());
        }
        return ctx;
    }

    public boolean cancelTimer(long id) {
        InternalTimerHandler handler = timeouts.remove(id);
        if (handler != null) {
            //handler.context.removeCloseHook(handler);
            return handler.cancel();
        } else {
            return false;
        }
    }

    @Override
    public EventLoopContext createEventLoopContext(String deploymentID, WorkerExecutor workerPool,
                                                   ClassLoader tccl) {
        return new EventLoopContext(this, internalBlockingPool, workerPool != null ? workerPool : this.workerPool, deploymentID, tccl);
    }

    @Override
    public AbstractContext createWorkerContext(boolean multiThreaded, String deploymentID,
                                               WorkerExecutor pool, ClassLoader tccl) {
        if (pool == null) {
            pool = this.workerPool;
        }
        if (multiThreaded) {
            return new MultiThreadedWorkerContext(this, internalBlockingPool, workerPool, deploymentID, tccl);
        } else {
            return new WorkerContext(this, internalBlockingPool, workerPool, deploymentID, tccl);
        }
    }

    private long scheduleTimeout(AbstractContext context, Handler<Long> handler, long delay, boolean periodic) {
        if (delay < 1) {
            throw new IllegalArgumentException("Cannot schedule a timer with delay < 1 ms");
        }
        long timerId = timeoutCounter.getAndIncrement();
        InternalTimerHandler task = new InternalTimerHandler(timerId, handler, periodic, delay, context);
        timeouts.put(timerId, task);
        //context.addCloseHook(task);
        return timerId;
    }

    public static Context context() {
        Thread current = Thread.currentThread();
        if (current instanceof CapzThread) {
            return ((CapzThread) current).getContext();
        }
        return null;
    }

    public AbstractContext getContext() {
        AbstractContext context = (AbstractContext) context();
        if (context != null && context.owner == this) {
            return context;
        }
        return null;
    }


    @Override
    public void close() {
        close(null);
    }


    @Override
    public synchronized void close(Handler<AsyncResult<Void>> completionHandler) {
        if (closed || eventBus == null) {
            // Just call the handler directly since pools shutdown
            if (completionHandler != null) {
                completionHandler.handle(Future.succeededFuture());
            }
            return;
        }
        closed = true;


    }

    @Override
    public <T> void executeBlockingInternal(Action<T> action, Handler<AsyncResult<T>> resultHandler) {
        AbstractContext context = getOrCreateContext();

        context.executeBlocking(action, resultHandler);
    }

    @Override
    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered,
                                    Handler<AsyncResult<T>> asyncResultHandler) {
        AbstractContext context = getOrCreateContext();
        context.executeBlocking(blockingCodeHandler, ordered, asyncResultHandler);
    }

    @Override
    public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler,
                                    Handler<AsyncResult<T>> asyncResultHandler) {
        executeBlocking(blockingCodeHandler, true, asyncResultHandler);
    }

    @Override
    public EventLoopGroup nettyEventLoopGroup() {
        return eventLoopGroup;
    }


    private class InternalTimerHandler implements Handler<Void>, Closeable {
        final Handler<Long> handler;
        final boolean periodic;
        final long timerID;
        final AbstractContext context;
        final java.util.concurrent.Future<?> future;
        final AtomicBoolean cancelled;

        boolean cancel() {
            if (cancelled.compareAndSet(false, true)) {
                future.cancel(false);
                return true;
            } else {
                return false;
            }
        }

        InternalTimerHandler(long timerID, Handler<Long> runnable, boolean periodic,
                             long delay, AbstractContext context) {
            this.context = context;
            this.timerID = timerID;
            this.handler = runnable;
            this.periodic = periodic;
            this.cancelled = new AtomicBoolean();
            EventLoop el = context.nettyEventLoop();
            Runnable toRun = () -> context.runOnContext(this);
            if (periodic) {
                // 最终会定时调用handle(Void v)方法
                future = el.scheduleAtFixedRate(toRun, delay, delay, TimeUnit.MILLISECONDS);
            } else {
                future = el.schedule(toRun, delay, TimeUnit.MILLISECONDS);
            }

        }

        public void handle(Void v) {
            if (!cancelled.get()) {
                try {
                    handler.handle(timerID);
                } finally {
                    if (!periodic) {
                        // Clean up after it's fired
                        cleanupNonPeriodic();
                    }
                }
            }
        }

        private void cleanupNonPeriodic() {
            CapzImpl.this.timeouts.remove(timerID);

            AbstractContext context = getContext();
            if (context != null) {
                //context.removeCloseHook(this);
            }
        }

        // Called via Context close hook when Verticle is undeployed
        public void close(Handler<AsyncResult<Void>> completionHandler) {
            CapzImpl.this.timeouts.remove(timerID);
            cancel();
            completionHandler.handle(Future.succeededFuture());
        }

    }

    public Capz exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    @Override
    public Handler<Throwable> exceptionHandler() {
        return exceptionHandler;
    }


    @Override
    public WorkerExecutorImpl createSharedWorkerExecutor(String name) {
        return createSharedWorkerExecutor(name, defaultWorkerPoolSize);
    }

    @Override
    public WorkerExecutorImpl createSharedWorkerExecutor(String name, int poolSize) {
        return createSharedWorkerExecutor(name, poolSize, defaultWorkerMaxExecTime);
    }

    // TODO 返回值
    @Override
    public synchronized WorkerExecutorImpl createSharedWorkerExecutor(String name, int poolSize, long maxExecuteTime) {
        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize must be > 0");
        }
        if (maxExecuteTime < 1) {
            throw new IllegalArgumentException("maxExecuteTime must be > 0");
        }
        SharedWorkerPool sharedWorkerPool = namedWorkerPools.get(name);
        if (sharedWorkerPool == null) {
            ExecutorService workerExec = Executors.newFixedThreadPool(poolSize,
                    new CapzThreadFactory(name + "-", checker, true, maxExecuteTime));
            namedWorkerPools.put(name, sharedWorkerPool = new SharedWorkerPool(this, workerExec, true, name));
        } else {
            sharedWorkerPool.refCount++;
        }
        AbstractContext context = getOrCreateContext();
        //context.addCloseHook(sharedWorkerPool);
        return sharedWorkerPool;
    }


    class SharedWorkerPool extends WorkerExecutorImpl {

        private final String name;
        private int refCount = 1;

        SharedWorkerPool(Capz capz, ExecutorService executorService,
                         boolean releaseOnClose, String name) {
            super(capz, executorService, releaseOnClose);
            this.name = name;
        }

        void release() {
            synchronized (CapzImpl.this) {
                if (--refCount == 0) {
                    releaseWorkerExecutor(name);
                    super.close(null);
                }
            }
        }
    }

    synchronized void releaseWorkerExecutor(String name) {
        namedWorkerPools.remove(name);
    }

}
