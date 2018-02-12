package com.capz.core;

@FunctionalInterface
public interface Handler<E> {
    void handle(E event);
}