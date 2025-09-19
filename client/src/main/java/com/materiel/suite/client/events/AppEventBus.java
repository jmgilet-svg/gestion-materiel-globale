package com.materiel.suite.client.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Event bus minimaliste (thread-safe) pour diffuser des événements UI. */
public final class AppEventBus {
  private static final AppEventBus INSTANCE = new AppEventBus();
  private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

  private AppEventBus(){
  }

  public static AppEventBus get(){
    return INSTANCE;
  }

  public <T> AutoCloseable subscribe(Class<T> type, Consumer<T> handler){
    var list = listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>());
    list.add(handler);
    return () -> list.remove(handler);
  }

  @SuppressWarnings("unchecked")
  public <T> void publish(T event){
    if (event == null){
      return;
    }
    var list = listeners.getOrDefault(event.getClass(), List.of());
    for (var listener : list){
      ((Consumer<T>) listener).accept(event);
    }
  }
}
