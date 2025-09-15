package com.materiel.suite.client.service;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;

import java.util.*;

/**
 * Change feed lecteur minimal — fondation Sprint A.
 * Garde le dernier curseur et renvoie les events depuis la dernière synchro.
 */
public class SyncService {
  private final RestClient rc;
  private long lastCursor = 0L;
  public SyncService(RestClient rc){ this.rc = rc; }

  @SuppressWarnings("unchecked")
  public synchronized List<Map<String,Object>> pull() {
    try {
      String body = rc.get("/api/v1/sync/changes?since=" + lastCursor);
      Map<String,Object> obj = (Map<String,Object>) SimpleJson.parse(body);
      Object cur = obj.get("cursor");
      if (cur instanceof Number n) lastCursor = n.longValue();
      List<Map<String,Object>> events = new ArrayList<>();
      for (Object e : SimpleJson.asArr(obj.getOrDefault("events", List.of()))){
        events.add((Map<String,Object>) e);
      }
      return events;
    } catch(Exception e){
      return List.of();
    }
  }

  public synchronized long getLastCursor(){ return lastCursor; }
  public synchronized void setLastCursor(long c){ this.lastCursor = c; }
}

