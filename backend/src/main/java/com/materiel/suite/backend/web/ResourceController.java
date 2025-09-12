package com.materiel.suite.backend.web;

import com.materiel.suite.backend.model.Resource;
import com.materiel.suite.backend.store.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
  private final InMemoryStore store;
  public ResourceController(InMemoryStore s){ this.store = s; }

  @GetMapping public List<Resource> list(){ return store.resources(); }
  @PostMapping public Resource create(@RequestBody Resource r){ return store.save(r); }
  @PutMapping("/{id}") public Resource update(@PathVariable UUID id, @RequestBody Resource r){ r.setId(id); return store.save(r); }
  @DeleteMapping("/{id}") public void delete(@PathVariable UUID id){ store.deleteResource(id); }
}
