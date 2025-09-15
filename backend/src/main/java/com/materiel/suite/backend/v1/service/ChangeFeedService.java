package com.materiel.suite.backend.v1.service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Change Feed minimal en mémoire (foundation pour offline-sync).
 * Stocke les derniers events (taille bornée) avec un curseur monotone.
 */
public class ChangeFeedService {
  private static final int MAX_EVENTS = 10_000;
  private final Deque<Map<String,Object>> events = new ConcurrentLinkedDeque<>();
  private final AtomicLong cursor = new AtomicLong(System.currentTimeMillis());

  public void emit(String type, String id, Map<String, Object> payload){
    long cur = cursor.incrementAndGet();
    Map<String,Object> ev = new LinkedHashMap<>();
    ev.put("cursor", cur);
    ev.put("type", type);
    ev.put("id", id);
    ev.put("at", Instant.now().toEpochMilli());
    ev.put("payload", payload==null? Map.of() : payload);
    events.addLast(ev);
    while (events.size() > MAX_EVENTS) events.pollFirst();
  }

  public Map<String,Object> fetchSince(long since){
    List<Map<String,Object>> out = new ArrayList<>();
    long cur = cursor.get();
    for (var ev : events){
      long c = (long) ev.get("cursor");
      if (c > since) out.add(ev);
    }
    Map<String,Object> res = new LinkedHashMap<>();
    res.put("cursor", cur);
    res.put("events", out);
    return res;
  }
}
