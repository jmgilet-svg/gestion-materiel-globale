package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.TimelineEvent;
import com.materiel.suite.client.service.TimelineService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Implémentation en mémoire pour l'historique d'intervention. */
public class MockTimelineService implements TimelineService {
  private final Map<String, List<TimelineEvent>> store = new ConcurrentHashMap<>();

  @Override
  public List<TimelineEvent> list(String interventionId){
    if (interventionId == null || interventionId.isBlank()){
      return List.of();
    }
    List<TimelineEvent> events = store.get(interventionId);
    if (events == null){
      return List.of();
    }
    List<TimelineEvent> copy = new ArrayList<>();
    synchronized (events){
      for (TimelineEvent event : events){
        copy.add(cloneEvent(event));
      }
    }
    copy.sort(Comparator.comparing(TimelineEvent::getTimestamp,
        Comparator.nullsLast(Comparator.naturalOrder())));
    return copy;
  }

  @Override
  public TimelineEvent append(String interventionId, TimelineEvent event){
    if (interventionId == null || interventionId.isBlank() || event == null){
      return null;
    }
    TimelineEvent stored = cloneEvent(event);
    if (stored.getId() == null || stored.getId().isBlank()){
      stored.setId(UUID.randomUUID().toString());
    }
    stored.setInterventionId(interventionId);
    if (stored.getTimestamp() == null){
      stored.setTimestamp(Instant.now());
    }
    store.computeIfAbsent(interventionId,
        key -> Collections.synchronizedList(new ArrayList<>())).add(stored);
    return cloneEvent(stored);
  }

  private TimelineEvent cloneEvent(TimelineEvent event){
    if (event == null){
      return null;
    }
    TimelineEvent copy = new TimelineEvent();
    copy.setId(event.getId());
    copy.setInterventionId(event.getInterventionId());
    copy.setTimestamp(event.getTimestamp());
    copy.setType(event.getType());
    copy.setMessage(event.getMessage());
    copy.setAuthor(event.getAuthor());
    return copy;
  }
}
