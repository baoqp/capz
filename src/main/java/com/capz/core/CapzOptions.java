package com.capz.core;

import com.capz.core.eventbus.EventBusOptions;
import lombok.Getter;

import java.util.Objects;

@Getter
public class CapzOptions {

    public static final int DEFAULT_EVENT_LOOP_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();


    public static final int DEFAULT_WORKER_POOL_SIZE = 20;


    public static final int DEFAULT_INTERNAL_BLOCKING_POOL_SIZE = 20;


    public static final boolean DEFAULT_CLUSTERED = false;


    public static final String DEFAULT_CLUSTER_HOST = "localhost";


    public static final int DEFAULT_CLUSTER_PORT = 0;


    public static final String DEFAULT_CLUSTER_PUBLIC_HOST = null;


    public static final int DEFAULT_CLUSTER_PUBLIC_PORT = -1;


    public static final long DEFAULT_CLUSTER_PING_INTERVAL = 20000;


    public static final long DEFAULT_CLUSTER_PING_REPLY_INTERVAL = 20000;


    public static final long DEFAULT_BLOCKED_THREAD_CHECK_INTERVAL = 1000;


    public static final long DEFAULT_MAX_EVENT_LOOP_EXECUTE_TIME = 2L * 1000 * 1000000;


    public static final long DEFAULT_MAX_WORKER_EXECUTE_TIME = 60L * 1000 * 1000000;


    public static final int DEFAULT_QUORUM_SIZE = 1;


    public static final String DEFAULT_HA_GROUP = "__DEFAULT__";


    public static final boolean DEFAULT_HA_ENABLED = false;


    public static final boolean DEFAULT_FILE_CACHING_ENABLED = true; // ???


    public static final boolean DEFAULT_PREFER_NATIVE_TRANSPORT = false;


    private static final long DEFAULT_WARNING_EXCEPTION_TIME = 5L * 1000 * 1000000;

    private int eventLoopPoolSize = DEFAULT_EVENT_LOOP_POOL_SIZE;
    private int workerPoolSize = DEFAULT_WORKER_POOL_SIZE;
    private int internalBlockingPoolSize = DEFAULT_INTERNAL_BLOCKING_POOL_SIZE;
    private long blockedThreadCheckInterval = DEFAULT_BLOCKED_THREAD_CHECK_INTERVAL;
    private long maxEventLoopExecuteTime = DEFAULT_MAX_EVENT_LOOP_EXECUTE_TIME;
    private long maxWorkerExecuteTime = DEFAULT_MAX_WORKER_EXECUTE_TIME;
    //private ClusterManager clusterManager;
    private boolean haEnabled = DEFAULT_HA_ENABLED;
    private int quorumSize = DEFAULT_QUORUM_SIZE;
    private String haGroup = DEFAULT_HA_GROUP;
    private long warningExceptionTime = DEFAULT_WARNING_EXCEPTION_TIME;
    private EventBusOptions eventBusOptions = new EventBusOptions();
    private boolean fileResolverCachingEnabled = DEFAULT_FILE_CACHING_ENABLED;
    private boolean preferNativeTransport = DEFAULT_PREFER_NATIVE_TRANSPORT;


    public CapzOptions setEventLoopPoolSize(int eventLoopPoolSize) {
        assert eventLoopPoolSize >= 1;
        this.eventLoopPoolSize = eventLoopPoolSize;
        return this;
    }


    public CapzOptions setWorkerPoolSize(int workerPoolSize) {
        assert workerPoolSize >= 1;
        this.workerPoolSize = workerPoolSize;
        return this;
    }


    public CapzOptions setClustered(boolean clustered) {
        eventBusOptions.setClustered(clustered);
        return this;
    }


    public String getClusterHost() {
        return eventBusOptions.getHost();
    }


    public CapzOptions setClusterHost(String clusterHost) {
        this.eventBusOptions.setHost(clusterHost);
        return this;
    }


    public String getClusterPublicHost() {
        return getEventBusOptions().getClusterPublicHost();
    }


    public VertxOptions setClusterPublicHost(String clusterPublicHost) {
        getEventBusOptions().setClusterPublicHost(clusterPublicHost);
        return this;
    }

    public int getClusterPort() {
        return eventBusOptions.getPort();
    }


    public CapzOptions setClusterPort(int clusterPort) {
        eventBusOptions.setPort(clusterPort);
        return this;
    }


    public int getClusterPublicPort() {
        return eventBusOptions.getClusterPublicPort();
    }


    public CapzOptions setClusterPublicPort(int clusterPublicPort) {
        getEventBusOptions().setClusterPublicPort(clusterPublicPort);
        return this;
    }


    public long getClusterPingInterval() {
        return getEventBusOptions().getClusterPingInterval();
    }


    public CapzOptions setClusterPingInterval(long clusterPingInterval) {
        eventBusOptions.setClusterPingInterval(clusterPingInterval);
        return this;
    }

    /**
     * Get the value of cluster ping reply interval, in ms.
     * <p>
     * After sending a ping, if a pong is not received in this time, the node will be considered dead.
     *
     * @return the value of cluster ping reply interval
     */
    public long getClusterPingReplyInterval() {
        return eventBusOptions.getClusterPingReplyInterval();
    }

    /**
     * Set the value of cluster ping reply interval, in ms.
     *
     * @param clusterPingReplyInterval The value of cluster ping reply interval, in ms.
     * @return a reference to this, so the API can be used fluently
     */
    public VertxOptions setClusterPingReplyInterval(long clusterPingReplyInterval) {
        eventBusOptions.setClusterPingReplyInterval(clusterPingReplyInterval);
        return this;
    }

    /**
     * Get the value of blocked thread check period, in ms.
     * <p>
     * This setting determines how often Vert.x will check whether event loop threads are executing for too long.
     *
     * @return the value of blocked thread check period, in ms.
     */
    public long getBlockedThreadCheckInterval() {
        return blockedThreadCheckInterval;
    }


    public CapzOptions setBlockedThreadCheckInterval(long blockedThreadCheckInterval) {
        if (blockedThreadCheckInterval < 1) {
            throw new IllegalArgumentException("blockedThreadCheckInterval must be > 0");
        }
        this.blockedThreadCheckInterval = blockedThreadCheckInterval;
        return this;
    }


    public CapzOptions setMaxEventLoopExecuteTime(long maxEventLoopExecuteTime) {
        if (maxEventLoopExecuteTime < 1) {
            throw new IllegalArgumentException("maxEventLoopExecuteTime must be > 0");
        }
        this.maxEventLoopExecuteTime = maxEventLoopExecuteTime;
        return this;
    }


    public CapzOptions setMaxWorkerExecuteTime(long maxWorkerExecuteTime) {
        if (maxWorkerExecuteTime < 1) {
            throw new IllegalArgumentException("maxWorkerpExecuteTime must be > 0");
        }
        this.maxWorkerExecuteTime = maxWorkerExecuteTime;
        return this;
    }



    /*public CapzOptions setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        return this;
    }*/


    public CapzOptions setInternalBlockingPoolSize(int internalBlockingPoolSize) {
        if (internalBlockingPoolSize < 1) {
            throw new IllegalArgumentException("internalBlockingPoolSize must be > 0");
        }
        this.internalBlockingPoolSize = internalBlockingPoolSize;
        return this;
    }


    public CapzOptions setHAEnabled(boolean haEnabled) {
        this.haEnabled = haEnabled;
        return this;
    }


    public CapzOptions setQuorumSize(int quorumSize) {
        if (quorumSize < 1) {
            throw new IllegalArgumentException("quorumSize should be >= 1");
        }
        this.quorumSize = quorumSize;
        return this;
    }


    public CapzOptions setHAGroup(String haGroup) {
        Objects.requireNonNull(haGroup, "ha group cannot be null");
        this.haGroup = haGroup;
        return this;
    }


    public CapzOptions setWarningExceptionTime(long warningExceptionTime) {
        if (warningExceptionTime < 1) {
            throw new IllegalArgumentException("warningExceptionTime must be > 0");
        }
        this.warningExceptionTime = warningExceptionTime;
        return this;
    }


    public CapzOptions setEventBusOptions(EventBusOptions options) {
        Objects.requireNonNull(options);
        this.eventBusOptions = options;
        return this;
    }


    public CapzOptions setPreferNativeTransport(boolean preferNativeTransport) {
        this.preferNativeTransport = preferNativeTransport;
        return this;
    }

}
