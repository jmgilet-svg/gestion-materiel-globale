package com.materiel.suite.backend.resources;

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
@RequestMapping("/api/v2/resource-types")
public class ResourceTypeV2Controller {
  private final ResourceTypeCatalogService service;

  public ResourceTypeV2Controller(ResourceTypeCatalogService service){
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<ResourceTypeV2Dto>> list(){
    return ResponseEntity.ok(service.list());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ResourceTypeV2Dto> get(@PathVariable String id){
    return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ResourceTypeV2Dto> create(@RequestBody ResourceTypeV2Dto body){
    return ResponseEntity.ok(service.create(body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ResourceTypeV2Dto> update(@PathVariable String id, @RequestBody ResourceTypeV2Dto body){
    return ResponseEntity.ok(service.update(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id){
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
