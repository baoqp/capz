package com.capz.core.eventbus.impl;

public class Handlers {

  private final AtomicInteger pos = new AtomicInteger(0);
  public final List<HandlerHolder> list = new CopyOnWriteArrayList<>();

  public HandlerHolder choose() {
    while (true) {
      int size = list.size();
      if (size == 0) {
        return null;
      }
      int p = pos.getAndIncrement();
      if (p >= size - 1) {
        pos.set(0);
      }
      try {
        return list.get(p);
      } catch (IndexOutOfBoundsException e) {
        // Can happen
        pos.set(0);
      }
    }
  }
}