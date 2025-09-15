package com.materiel.suite.backend.v1.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

@Service
public class ChangeFeedService {
  private long cursor = 0L;
  private final List<Map<String,Object>> events = new ArrayList<>();
  private final List<String> webhooks = new ArrayList<>();
  private final HttpClient http = HttpClient.newHttpClient();

  public synchronized void emit(String type, String id, Map<String,Object> payload){
    Map<String,Object> ev = new LinkedHashMap<>();
    ev.put("cursor", ++cursor);
    ev.put("type", type);
    ev.put("id", id);
    ev.put("time", Instant.now().toString());
    if (payload!=null) ev.putAll(payload);
    events.add(ev);
    // Fire and forget webhooks
    for (String url : List.copyOf(webhooks)){
      try {
        String json = "{\"type\":\""+type+"\",\"id\":\""+id+"\",\"cursor\":"+cursor+"}";
        var req = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
      } catch(Exception ignore){}
    }
  }
  public synchronized Map<String,Object> getSince(long since){
    List<Map<String,Object>> out = new ArrayList<>();
    for (var e : events) if (((Number)e.get("cursor")).longValue()>since) out.add(e);
    return Map.of("cursor", cursor, "events", out);
  }
  public synchronized Map<String,Object> fetchSince(long since){
    return getSince(since);
  }
  public synchronized void registerWebhook(String url){
    if (url!=null && !url.isBlank() && !webhooks.contains(url)) webhooks.add(url);
  }
  public synchronized void unregisterWebhook(String url){
    webhooks.remove(url);
  }
  public synchronized List<String> listWebhooks(){ return List.copyOf(webhooks); }
}
