package com.capz.core.Exception;

import com.capz.core.net.impl.CapzEventLoopGroup;

/**
 * @author Bao Qingping
 */
public class CapzException extends RuntimeException {

    public CapzException(String message) {
        super(message);
    }

    public CapzException(String message, Throwable cause) {
        super(message, cause);
    }

    public CapzException(Throwable cause) {
        super(cause);
    }
}
