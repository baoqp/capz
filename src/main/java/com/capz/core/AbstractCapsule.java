package com.capz.core;

import lombok.Getter;

/**
 * @author Bao Qingping
 */
@Getter
public abstract class AbstractCapsule implements Capsule {

    Capz capz;

    Context context;

    @Override
    public void init(Capz capz, Context context) {
        this.capz = capz;
        this.context = context;
    }

}
