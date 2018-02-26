package com.capz.core.http.impl;

import com.capz.core.Handler;
import com.capz.core.http.HttpConnection;
import com.capz.core.http.HttpServerRequest;

import java.util.Objects;

public class HttpHandlers {

    final Handler<HttpServerRequest> requesthHandler;
    final Handler<HttpConnection> connectionHandler;
    final Handler<Throwable> exceptionHandler;

    public HttpHandlers(
            Handler<HttpServerRequest> requesthHandler,
            Handler<HttpConnection> connectionHandler,
            Handler<Throwable> exceptionHandler) {

        this.requesthHandler = requesthHandler;
        this.connectionHandler = connectionHandler;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpHandlers that = (HttpHandlers) o;

        if (!Objects.equals(requesthHandler, that.requesthHandler)) return false;

        if (!Objects.equals(connectionHandler, that.connectionHandler)) return false;
        if (!Objects.equals(exceptionHandler, that.exceptionHandler)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (requesthHandler != null) {
            result = 31 * result + requesthHandler.hashCode();
        }

        if (connectionHandler != null) {
            result = 31 * result + connectionHandler.hashCode();
        }
        if (exceptionHandler != null) {
            result = 31 * result + exceptionHandler.hashCode();
        }
        return result;
    }
}