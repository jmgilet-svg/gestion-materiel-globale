package com.materiel.suite.backend.intervention;

import com.materiel.suite.backend.intervention.dto.InterventionV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v2/interventions")
public class InterventionV2Controller {
  private final Map<String, InterventionV2Dto> store = new ConcurrentHashMap<>();

  @GetMapping
  public ResponseEntity<List<InterventionV2Dto>> list(){
    return ResponseEntity.ok(new ArrayList<>(store.values()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<InterventionV2Dto> get(@PathVariable String id){
    InterventionV2Dto dto = store.get(id);
    if (dto == null){
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
  }

  @PutMapping("/{id}")
  public ResponseEntity<InterventionV2Dto> upsert(@PathVariable String id, @RequestBody InterventionV2Dto body){
    if (body == null){
      return ResponseEntity.badRequest().build();
    }
    body.setId(id);
    store.put(id, body);
    return ResponseEntity.ok(body);
  }
}
