package com.materiel.suite.backend.web;

import com.materiel.suite.backend.model.Intervention;
import com.materiel.suite.backend.store.InMemoryStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interventions")
public class InterventionController {
  private final InMemoryStore store;
  public InterventionController(InMemoryStore s){ this.store=s; }

  @GetMapping
  public List<Intervention> list(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
    return store.interventions(from, to);
  }

  @PostMapping public Intervention create(@RequestBody Intervention i){ return store.save(i); }
  @PutMapping("/{id}") public Intervention update(@PathVariable UUID id, @RequestBody Intervention i){ i.setId(id); return store.save(i); }
  @DeleteMapping("/{id}") public void delete(@PathVariable UUID id){ store.deleteIntervention(id); }
}
