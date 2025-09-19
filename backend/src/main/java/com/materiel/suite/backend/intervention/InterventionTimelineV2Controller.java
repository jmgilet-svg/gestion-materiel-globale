package com.materiel.suite.backend.intervention;

import com.materiel.suite.backend.intervention.dto.TimelineEventV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v2/interventions")
public class InterventionTimelineV2Controller {
  private final Map<String, List<TimelineEventV2Dto>> store = new ConcurrentHashMap<>();

  @GetMapping("/{id}/timeline")
  public ResponseEntity<List<TimelineEventV2Dto>> list(@PathVariable("id") String interventionId){
    if (interventionId == null || interventionId.isBlank()){
      return ResponseEntity.badRequest().build();
    }
    List<TimelineEventV2Dto> events = store.getOrDefault(interventionId, List.of());
    List<TimelineEventV2Dto> copy = new ArrayList<>(events);
    copy.sort(Comparator.comparing(TimelineEventV2Dto::getTimestamp,
        Comparator.nullsLast(Comparator.naturalOrder())));
    return ResponseEntity.ok(copy);
  }

  @PostMapping("/{id}/timeline")
  public ResponseEntity<TimelineEventV2Dto> append(@PathVariable("id") String interventionId,
                                                   @RequestBody TimelineEventV2Dto body){
    if (interventionId == null || interventionId.isBlank() || body == null){
      return ResponseEntity.badRequest().build();
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
    return ResponseEntity.ok(event);
  }
}
