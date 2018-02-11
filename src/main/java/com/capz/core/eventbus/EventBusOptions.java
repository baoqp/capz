package com.capz.core.eventbus;


import com.capz.core.CapzOptions;
import lombok.Getter;

@Getter
public class EventBusOptions {

    private boolean clustered = CapzOptions.DEFAULT_CLUSTERED;
    private String clusterPublicHost = CapzOptions.DEFAULT_CLUSTER_PUBLIC_HOST;
    private int clusterPublicPort = CapzOptions.DEFAULT_CLUSTER_PUBLIC_PORT;
    private long clusterPingInterval = CapzOptions.DEFAULT_CLUSTER_PING_INTERVAL;
    private long clusterPingReplyInterval = CapzOptions.DEFAULT_CLUSTER_PING_REPLY_INTERVAL;


    public static final int DEFAULT_PORT = CapzOptions.DEFAULT_CLUSTER_PORT;


    public static final String DEFAULT_HOST = CapzOptions.DEFAULT_CLUSTER_HOST;


    public static final int DEFAULT_ACCEPT_BACKLOG = -1;

    private int port;

    private String host;

    private int acceptBacklog;

    public static final int DEFAULT_RECONNECT_ATTEMPTS = 0;

    public static final long DEFAULT_RECONNECT_INTERVAL = 1000;

    public static final int DEFAULT_CONNECT_TIMEOUT = 60 * 1000;

    public static final boolean DEFAULT_TRUST_ALL = true;

    private int reconnectAttempts;
    private long reconnectInterval;

    private int connectTimeout;
    private boolean trustAll;


    public EventBusOptions() {

        clustered = CapzOptions.DEFAULT_CLUSTERED;

        port = DEFAULT_PORT;
        host = DEFAULT_HOST;
        acceptBacklog = DEFAULT_ACCEPT_BACKLOG;

        reconnectAttempts = DEFAULT_RECONNECT_ATTEMPTS;
        reconnectInterval = DEFAULT_RECONNECT_INTERVAL;

        connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        trustAll = DEFAULT_TRUST_ALL;
    }


    public EventBusOptions setAcceptBacklog(int acceptBacklog) {
        this.acceptBacklog = acceptBacklog;
        return this;
    }


    public EventBusOptions setHost(String host) {
        this.host = host;
        return this;
    }


    public EventBusOptions setPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("clusterPort p must be in range 0 <= p <= 65535");
        }
        this.port = port;
        return this;
    }


    public EventBusOptions setReconnectAttempts(int attempts) {
        this.reconnectAttempts = attempts;
        return this;
    }

    public EventBusOptions setReconnectInterval(long interval) {
        this.reconnectInterval = interval;
        return this;
    }



    public EventBusOptions setClustered(boolean clustered) {
        this.clustered = clustered;
        return this;
    }


    public EventBusOptions setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }


    public EventBusOptions setClusterPingInterval(long clusterPingInterval) {
        if (clusterPingInterval < 1) {
            throw new IllegalArgumentException("clusterPingInterval must be greater than 0");
        }
        this.clusterPingInterval = clusterPingInterval;
        return this;
    }


    public EventBusOptions setClusterPingReplyInterval(long clusterPingReplyInterval) {
        if (clusterPingReplyInterval < 1) {
            throw new IllegalArgumentException("clusterPingReplyInterval must be greater than 0");
        }
        this.clusterPingReplyInterval = clusterPingReplyInterval;
        return this;
    }


    public EventBusOptions setClusterPublicHost(String clusterPublicHost) {
        this.clusterPublicHost = clusterPublicHost;
        return this;
    }

    public int getClusterPublicPort() {
        return clusterPublicPort;
    }


    public EventBusOptions setClusterPublicPort(int clusterPublicPort) {
        if (clusterPublicPort < 0 || clusterPublicPort > 65535) {
            throw new IllegalArgumentException("clusterPublicPort p must be in range 0 <= p <= 65535");
        }
        this.clusterPublicPort = clusterPublicPort;
        return this;
    }
}
