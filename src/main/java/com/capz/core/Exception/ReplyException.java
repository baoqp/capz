package com.capz.core.Exception;

import com.capz.core.eventbus.ReplyFailure;

public class ReplyException extends CapzException {

    private final ReplyFailure failureType;
    private final int failureCode;


    public ReplyException(ReplyFailure failureType, int failureCode, String message) {
        super(message);
        this.failureType = failureType;
        this.failureCode = failureCode;
    }


    public ReplyException(ReplyFailure failureType, String message) {
        this(failureType, -1, message);
    }


    public ReplyException(ReplyFailure failureType) {
        this(failureType, -1, null);
    }


    public ReplyFailure failureType() {
        return failureType;
    }


    public int failureCode() {
        return failureCode;
    }

    @Override
    public String toString() {
        String message = getMessage();
        return "(" + failureType + "," + failureCode + ") " + (message != null ? message : "");
    }

}