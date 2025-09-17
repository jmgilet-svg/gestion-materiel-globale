package com.materiel.suite.backend.interventions;

import com.materiel.suite.backend.interventions.dto.InterventionTypeV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/intervention-types")
public class InterventionTypeV2Controller {
  private final InterventionTypeCatalogService service;

  public InterventionTypeV2Controller(InterventionTypeCatalogService service){
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<InterventionTypeV2Dto>> list(){
    return ResponseEntity.ok(service.list());
  }

  @GetMapping("/{id}")
  public ResponseEntity<InterventionTypeV2Dto> get(@PathVariable String id){
    return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<InterventionTypeV2Dto> create(@RequestBody InterventionTypeV2Dto body){
    return ResponseEntity.ok(service.create(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<InterventionTypeV2Dto> update(@PathVariable String id, @RequestBody InterventionTypeV2Dto body){
    return ResponseEntity.ok(service.update(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id){
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
