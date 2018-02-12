package com.capz.core.impl;


import com.capz.core.AsyncResult;
import com.capz.core.Handler;

import java.util.function.Function;

public interface Future<T> extends AsyncResult<T>, Handler<AsyncResult<T>> {

    static <T> Future<T> future(Handler<Future<T>> handler) {
        Future<T> fut = future();
        handler.handle(fut);
        return fut;
    }

    // 返回一个空的future
    static <T> Future<T> future() {
        return factory.future();
    }


    static <T> Future<T> succeededFuture() {
        return factory.succeededFuture();
    }


    static <T> Future<T> succeededFuture(T result) {
        return factory.succeededFuture(result);
    }


    static <T> Future<T> failedFuture(Throwable t) {
        return factory.failedFuture(t);
    }


    static <T> Future<T> failedFuture(String failureMessage) {
        return factory.failureFuture(failureMessage);
    }

    boolean isComplete();

    Future<T> setHandler(Handler<AsyncResult<T>> handler);

    void complete(T result);

    void complete();

    void fail(Throwable cause);

    void fail(String failureMessage);

    boolean tryComplete(T result);

    boolean tryComplete();

    boolean tryFail(Throwable cause);

    boolean tryFail(String failureMessage);


    @Override
    T result();

    @Override
    Throwable cause();

    @Override
    boolean succeeded();

    @Override
    boolean failed();


    default <U> Future<U> compose(Handler<T> handler, Future<U> next) {
        setHandler(ar -> {
            if (ar.succeeded()) {
                try {
                    handler.handle(ar.result());
                } catch (Throwable err) {
                    if (next.isComplete()) {
                        throw err;
                    }
                    next.fail(err);
                }
            } else {
                next.fail(ar.cause());
            }
        });
        return next;
    }


    default <U> Future<U> compose(Function<T, Future<U>> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<U> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                Future<U> apply;
                try {
                    apply = mapper.apply(ar.result());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                apply.setHandler(ret);
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }


    default <U> Future<U> map(Function<T, U> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<U> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                U mapped;
                try {
                    mapped = mapper.apply(ar.result());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                ret.complete(mapped);
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }


    default <V> Future<V> map(V value) {
        Future<V> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                ret.complete(value);
            } else {
                ret.fail(ar.cause());
            }
        });
        return ret;
    }


    @Override
    default <V> Future<V> mapEmpty() {
        return (Future<V>) AsyncResult.super.mapEmpty();
    }


    @Override
    void handle(AsyncResult<T> asyncResult);


    default Handler<AsyncResult<T>> completer() {
        return this;
    }

    default Future<T> recover(Function<Throwable, Future<T>> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<T> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                ret.complete(result());
            } else {
                Future<T> mapped;
                try {
                    mapped = mapper.apply(ar.cause());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                mapped.setHandler(ret);
            }
        });
        return ret;
    }


    default Future<T> otherwise(Function<Throwable, T> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        Future<T> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                ret.complete(result());
            } else {
                T value;
                try {
                    value = mapper.apply(ar.cause());
                } catch (Throwable e) {
                    ret.fail(e);
                    return;
                }
                ret.complete(value);
            }
        });
        return ret;
    }


    default Future<T> otherwise(T value) {
        Future<T> ret = Future.future();
        setHandler(ar -> {
            if (ar.succeeded()) {
                ret.complete(result());
            } else {
                ret.complete(value);
            }
        });
        return ret;
    }


    default Future<T> otherwiseEmpty() {
        return (Future<T>) AsyncResult.super.otherwiseEmpty();
    }

    FutureFactory factory = new FutureFactory();

}
