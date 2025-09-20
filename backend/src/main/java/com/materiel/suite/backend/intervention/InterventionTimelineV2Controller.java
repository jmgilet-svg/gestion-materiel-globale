package com.materiel.suite.backend.intervention;

import com.materiel.suite.backend.intervention.dto.TimelineEventV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/interventions")
public class InterventionTimelineV2Controller {
  private final TimelineEventStore store;

  public InterventionTimelineV2Controller(TimelineEventStore store){
    this.store = store;
  }

  @GetMapping("/{id}/timeline")
  public ResponseEntity<List<TimelineEventV2Dto>> list(@PathVariable("id") String interventionId){
    if (interventionId == null || interventionId.isBlank()){
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(store.list(interventionId));
  }

  @PostMapping("/{id}/timeline")
  public ResponseEntity<TimelineEventV2Dto> append(@PathVariable("id") String interventionId,
                                                   @RequestBody TimelineEventV2Dto body){
    if (interventionId == null || interventionId.isBlank() || body == null){
      return ResponseEntity.badRequest().build();
    }
    TimelineEventV2Dto event = store.append(interventionId, body);
    if (event == null){
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(event);
  }
}
