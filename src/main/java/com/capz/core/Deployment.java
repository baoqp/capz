package com.capz.core;


import com.capz.core.impl.AbstractContext;

import java.util.Set;

public interface Deployment {

    boolean addChild(Deployment deployment);

    void removeChild(Deployment deployment);

    void undeploy(Handler<AsyncResult<Void>> completionHandler);

    void doUndeploy(AbstractContext undeployingContext, Handler<AsyncResult<Void>> completionHandler);

    String deploymentID();

    String verticleIdentifier();

    DeploymentOptions deploymentOptions();

    Set<Capsule> getVerticles();

    boolean isChild();
}