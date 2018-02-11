package com.capz.core.Exception;

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
}
