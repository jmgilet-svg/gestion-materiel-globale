package com.materiel.suite.backend.intervention;

import com.materiel.suite.backend.intervention.dto.TimelineEventV2Dto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TimelineEventStore {
  private final Map<String, List<TimelineEventV2Dto>> store = new ConcurrentHashMap<>();

  public List<TimelineEventV2Dto> list(String interventionId){
    List<TimelineEventV2Dto> events = store.getOrDefault(interventionId, List.of());
    List<TimelineEventV2Dto> copy = new ArrayList<>(events);
    copy.sort(Comparator.comparing(TimelineEventV2Dto::getTimestamp,
        Comparator.nullsLast(Comparator.naturalOrder())));
    return copy;
  }

  public TimelineEventV2Dto append(String interventionId, TimelineEventV2Dto body){
    if (interventionId == null || interventionId.isBlank() || body == null){
      return null;
    }
    TimelineEventV2Dto event = new TimelineEventV2Dto();
    event.setId(UUID.randomUUID().toString());
    event.setInterventionId(interventionId);
    Instant timestamp = body.getTimestamp() != null ? body.getTimestamp() : Instant.now();
    event.setTimestamp(timestamp);
    String type = body.getType();
    event.setType(type == null || type.isBlank() ? "INFO" : type);
    event.setMessage(body.getMessage());
    event.setAuthor(body.getAuthor());
    store.computeIfAbsent(interventionId,
            key -> Collections.synchronizedList(new ArrayList<>()))
        .add(event);
    return event;
  }

  public void appendAction(String interventionId, String message, String author, Instant timestamp){
    TimelineEventV2Dto dto = new TimelineEventV2Dto();
    dto.setType("ACTION");
    dto.setMessage(message);
    dto.setAuthor(author);
    dto.setTimestamp(timestamp);
    append(interventionId, dto);
  }
}
