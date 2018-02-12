package com.capz.core.impl;

import com.capz.core.Capz;
import com.capz.core.CapzOptions;
import com.capz.core.Context;

public class CapzFactory {


    public Capz vertx() {
        return new CapzImpl();
    }

    // TODO
    public Capz vertx(CapzOptions options) {
        return null;
        //return new CapzImpl(options);
    }


    public Context context() {
        return CapzImpl.context();
    }
}