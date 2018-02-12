package com.capz.core.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Exception.NoStackTraceThrowable;
import com.capz.core.Handler;

public class FailedFuture<T> implements Future<T> {

    private final Throwable cause;


    FailedFuture(Throwable t) {
        cause = t ;
    }


    FailedFuture(String failureMessage) {
        this(new NoStackTraceThrowable(failureMessage));
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Future<T> setHandler(Handler<AsyncResult<T>> handler) {
        handler.handle(this);
        return this;
    }

    @Override
    public void complete(T result) {
        throw new IllegalStateException("Result is already complete: failed");
    }

    @Override
    public void complete() {
        throw new IllegalStateException("Result is already complete: failed");
    }

    @Override
    public void fail(Throwable cause) {
        throw new IllegalStateException("Result is already complete: failed");
    }

    @Override
    public void fail(String failureMessage) {
        throw new IllegalStateException("Result is already complete: failed");
    }

    @Override
    public boolean tryComplete(T result) {
        return false;
    }

    @Override
    public boolean tryComplete() {
        return false;
    }

    @Override
    public boolean tryFail(Throwable cause) {
        return false;
    }

    @Override
    public boolean tryFail(String failureMessage) {
        return false;
    }

    @Override
    public T result() {
        return null;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean succeeded() {
        return false;
    }

    @Override
    public boolean failed() {
        return true;
    }

    @Override
    public void handle(AsyncResult<T> asyncResult) {
        throw new IllegalStateException("Result is already complete: failed");
    }

    @Override
    public String toString() {
        return "Future{cause=" + cause.getMessage() + "}";
    }
}
