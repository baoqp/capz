package com.capz.core.impl;

import com.capz.core.AsyncResult;
import com.capz.core.Handler;

class FutureImpl<T> implements Future<T>, Handler<AsyncResult<T>> {

    private boolean failed;
    private boolean succeeded;
    private Handler<AsyncResult<T>> handler;
    private T result;
    private Throwable throwable;


    public T result() {
        return result;
    }


    public Throwable cause() {
        return throwable;
    }


    public synchronized boolean succeeded() {
        return succeeded;
    }


    public synchronized boolean failed() {
        return failed;
    }


    public synchronized boolean isComplete() {
        return failed || succeeded;
    }


    public Future<T> setHandler(Handler<AsyncResult<T>> handler) {
        boolean callHandler;
        synchronized (this) {
            this.handler = handler;
            callHandler = isComplete();
        }
        if (callHandler) {
            handler.handle(this);
        }
        return this;
    }

    @Override
    public void complete(T result) {
        if (!tryComplete(result)) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public void complete() {
        if (!tryComplete()) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public void fail(Throwable cause) {
        if (!tryFail(cause)) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public void fail(String failureMessage) {
        if (!tryFail(failureMessage)) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }

    @Override
    public boolean tryComplete(T result) {
        Handler<AsyncResult<T>> h;
        synchronized (this) {
            if (succeeded || failed) {
                return false;
            }
            this.result = result;
            succeeded = true;
            h = handler;
        }
        if (h != null) {
            h.handle(this);
        }
        return true;
    }

    @Override
    public boolean tryComplete() {
        return tryComplete(null);
    }

    public void handle(Future<T> ar) {
        if (ar.succeeded()) {
            complete(ar.result());
        } else {
            fail(ar.cause());
        }
    }

    @Override
    public Handler<AsyncResult<T>> completer() {
        return this;
    }

    @Override
    public void handle(AsyncResult<T> asyncResult) {
        if (asyncResult.succeeded()) {
            complete(asyncResult.result());
        } else {
            fail(asyncResult.cause());
        }
    }

    @Override
    public boolean tryFail(Throwable cause) {
        Handler<AsyncResult<T>> h;
        synchronized (this) {
            if (succeeded || failed) {
                return false;
            }
            this.throwable = cause  ;
            failed = true;
            h = handler;
        }
        if (h != null) {
            h.handle(this);
        }
        return true;
    }

    @Override
    public boolean tryFail(String failureMessage) {
        return tryFail((Throwable)null);
    }

    @Override
    public String toString() {
        synchronized (this) {
            if (succeeded) {
                return "Future{result=" + result + "}";
            }
            if (failed) {
                return "Future{cause=" + throwable.getMessage() + "}";
            }
            return "Future{unresolved}";
        }
    }
}
