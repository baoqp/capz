package com.capz.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
public class DeploymentOptions {

    public static final boolean DEFAULT_WORKER = false;
    public static final boolean DEFAULT_MULTI_THREADED = false;
    public static final String DEFAULT_ISOLATION_GROUP = null;
    public static final boolean DEFAULT_HA = false;
    public static final int DEFAULT_INSTANCES = 1;


    private boolean worker;
    private boolean multiThreaded;
    private String isolationGroup;
    private String workerPoolName;
    private int workerPoolSize;
    private long maxWorkerExecuteTime;
    private boolean ha;
    private List<String> extraClasspath;
    private int instances;
    private List<String> isolatedClasses;


    public DeploymentOptions() {
        this.worker = DEFAULT_WORKER;

        this.multiThreaded = DEFAULT_MULTI_THREADED;
        this.isolationGroup = DEFAULT_ISOLATION_GROUP;
        this.ha = DEFAULT_HA;
        this.instances = DEFAULT_INSTANCES;
        this.workerPoolName = null;
        this.workerPoolSize = CapzOptions.DEFAULT_WORKER_POOL_SIZE;
        this.maxWorkerExecuteTime = CapzOptions.DEFAULT_MAX_WORKER_EXECUTE_TIME;
    }


    public DeploymentOptions(DeploymentOptions other) {
        this.worker = other.isWorker();
        this.multiThreaded = other.isMultiThreaded();
        this.isolationGroup = other.getIsolationGroup();
        this.ha = other.isHa();
        this.extraClasspath = other.getExtraClasspath() == null ? null : new ArrayList<>(other.getExtraClasspath());
        this.instances = other.instances;
        this.isolatedClasses = other.getIsolatedClasses() == null ? null : new ArrayList<>(other.getIsolatedClasses());
        this.workerPoolName = other.workerPoolName;
        setWorkerPoolSize(other.workerPoolSize);
        setMaxWorkerExecuteTime(other.maxWorkerExecuteTime);
    }


    public boolean isWorker() {
        return worker;
    }


    public DeploymentOptions setWorker(boolean worker) {
        this.worker = worker;
        return this;
    }


    public boolean isMultiThreaded() {
        return multiThreaded;
    }


    public DeploymentOptions setMultiThreaded(boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
        return this;
    }


    public String getIsolationGroup() {
        return isolationGroup;
    }


    public DeploymentOptions setIsolationGroup(String isolationGroup) {
        this.isolationGroup = isolationGroup;
        return this;
    }


    public boolean isHa() {
        return ha;
    }


    public DeploymentOptions setHa(boolean ha) {
        this.ha = ha;
        return this;
    }


    public List<String> getExtraClasspath() {
        return extraClasspath;
    }


    public DeploymentOptions setExtraClasspath(List<String> extraClasspath) {
        this.extraClasspath = extraClasspath;
        return this;
    }


    public int getInstances() {
        return instances;
    }


    public DeploymentOptions setInstances(int instances) {
        this.instances = instances;
        return this;
    }


    public List<String> getIsolatedClasses() {
        return isolatedClasses;
    }


    public DeploymentOptions setIsolatedClasses(List<String> isolatedClasses) {
        this.isolatedClasses = isolatedClasses;
        return this;
    }


    public String getWorkerPoolName() {
        return workerPoolName;
    }


    public DeploymentOptions setWorkerPoolName(String workerPoolName) {
        this.workerPoolName = workerPoolName;
        return this;
    }


    public int getWorkerPoolSize() {
        return workerPoolSize;
    }


    public DeploymentOptions setWorkerPoolSize(int workerPoolSize) {
        if (workerPoolSize < 1) {
            throw new IllegalArgumentException("workerPoolSize must be > 0");
        }
        this.workerPoolSize = workerPoolSize;
        return this;
    }


    public long getMaxWorkerExecuteTime() {
        return maxWorkerExecuteTime;
    }


    public DeploymentOptions setMaxWorkerExecuteTime(long maxWorkerExecuteTime) {
        if (maxWorkerExecuteTime < 1) {
            throw new IllegalArgumentException("maxWorkerExecuteTime must be > 0");
        }
        this.maxWorkerExecuteTime = maxWorkerExecuteTime;
        return this;
    }


}
