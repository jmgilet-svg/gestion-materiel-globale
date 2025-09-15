package com.materiel.suite.backend.v1.service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache d'idempotence simple (clé = route + Idempotency-Key).
 * En production, à stocker en base/redis avec TTL.
 */
public class IdempotencyService {
  private static final long TTL_MS = 10 * 60 * 1000; // 10 min
  private final Map<String, Entry> cache = new ConcurrentHashMap<>();

  public <T> void remember(String route, String key, T obj){
    cache.put(route+"|"+key, new Entry(obj, Instant.now().toEpochMilli()));
  }
  @SuppressWarnings("unchecked")
  public <T> T findExisting(String route, String key, Class<T> cls){
    Entry e = cache.get(route+"|"+key);
    if (e==null) return null;
    if (Instant.now().toEpochMilli() - e.at > TTL_MS){ cache.remove(route+"|"+key); return null; }
    if (cls.isInstance(e.obj)) return (T) e.obj;
    return null;
  }
  private record Entry(Object obj, long at){}
}
