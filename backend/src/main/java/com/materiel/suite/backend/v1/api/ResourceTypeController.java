package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.service.ResourceTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resource-types")
public class ResourceTypeController {
  private final ResourceTypeService service;
  public ResourceTypeController(ResourceTypeService service){ this.service = service; }

  @GetMapping
  public List<ResourceTypeDto> list(){
    return service.list();
  }

  @PostMapping
  public ResponseEntity<ResourceTypeDto> create(@RequestBody ResourceTypeDto body){
    if (body==null || !StringUtils.hasText(body.getCode())){
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(service.upsert(body));
  }

  @PutMapping("/{code}")
  public ResponseEntity<ResourceTypeDto> update(@PathVariable String code, @RequestBody ResourceTypeDto body){
    if (!StringUtils.hasText(code)) return ResponseEntity.badRequest().build();
    body.setCode(code);
    return ResponseEntity.ok(service.upsert(body));
  }

  @DeleteMapping("/{code}")
  public ResponseEntity<Void> delete(@PathVariable String code){
    service.delete(code);
    return ResponseEntity.noContent().build();
  }
}
