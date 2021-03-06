package com.capz.core;

import java.util.function.Function;

// 包装异步操作的结果
public interface AsyncResult<T> {

    T result();

    Throwable cause();

    boolean succeeded();


    boolean failed();

    // 使用mapper对结果进行变换
    default <U> AsyncResult<U> map(Function<T, U> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        return new AsyncResult<U>() {
            @Override
            public U result() {
                if (succeeded()) {
                    return mapper.apply(AsyncResult.this.result());
                } else {
                    return null;
                }
            }

            @Override
            public Throwable cause() {
                return AsyncResult.this.cause();
            }

            @Override
            public boolean succeeded() {
                return AsyncResult.this.succeeded();
            }

            @Override
            public boolean failed() {
                return AsyncResult.this.failed();
            }
        };
    }

    default <V> AsyncResult<V> map(V value) {
        return map(t -> value);
    }

    default <V> AsyncResult<V> mapEmpty() {
        return map((V) null);
    }


    // 消息处理失败时，使用mapper处理返回值
    default AsyncResult<T> otherwise(Function<Throwable, T> mapper) {
        if (mapper == null) {
            throw new NullPointerException();
        }
        return new AsyncResult<T>() {
            @Override
            public T result() {
                if (AsyncResult.this.succeeded()) {
                    return AsyncResult.this.result();
                } else if (AsyncResult.this.failed()) {
                    return mapper.apply(AsyncResult.this.cause());
                } else {
                    return null;
                }
            }

            @Override
            public Throwable cause() {
                return null;
            }

            @Override
            public boolean succeeded() {
                return AsyncResult.this.succeeded() || AsyncResult.this.failed();
            }

            @Override
            public boolean failed() {
                return false;
            }
        };
    }

    default AsyncResult<T> otherwise(T value) {
        return otherwise(err -> value);
    }

    default AsyncResult<T> otherwiseEmpty() {
        return otherwise(err -> null);
    }
}
