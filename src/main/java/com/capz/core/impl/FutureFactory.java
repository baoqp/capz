package com.capz.core.impl;

public class FutureFactory {

    private static final SucceededFuture EMPTY = new SucceededFuture<>(null);


    public <T> Future<T> future() {
        return new FutureImpl<>();
    }


    public <T> Future<T> succeededFuture() {
        @SuppressWarnings("unchecked")
        Future<T> fut = EMPTY;
        return fut;
    }


    public <T> Future<T> succeededFuture(T result) {
        return new SucceededFuture<>(result);
    }


    public <T> Future<T> failedFuture(Throwable t) {
        return new FailedFuture<>(t);
    }


    public <T> Future<T> failureFuture(String failureMessage) {
        return new FailedFuture<>(failureMessage);
    }
}
