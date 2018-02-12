package com.capz.core;

import com.capz.core.impl.Future;

/**
 * @author Bao Qingping
 */
public interface Capsule {

    Capz getCapz();

    void init(Capz capz, Context context);

    void start(Future<Void> startFuture) throws Exception;

    void stop(Future<Void> stopFuture) throws Exception;
}
