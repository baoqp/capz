package com.capz.core;

import com.capz.core.impl.Future;

public interface CapsuleFactory {

    static String removePrefix(String identifer) {
        int pos = identifer.indexOf(':');
        if (pos != -1) {
            if (pos == identifer.length() - 1) {
                throw new IllegalArgumentException("Invalid identifier: " + identifer);
            }
            return identifer.substring(pos + 1);
        } else {
            return identifer;
        }
    }


    default int order() {
        return 0;
    }


    default boolean requiresResolve() {
        return false;
    }


    default boolean blockingCreate() {
        return false;
    }


    default void resolve(String identifier, DeploymentOptions deploymentOptions, ClassLoader classLoader, Future<String> resolution) {
        resolution.complete(identifier);
    }


    default void init(Capz capz) {
    }


    default void close() {
    }


    String prefix();


    Capsule createCapsule(String verticleName, ClassLoader classLoader) throws Exception;

}
