package com.materiel.suite.backend.planning;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PlanningController {
  private final PlanningService service;
  public PlanningController(PlanningService service){ this.service = service; }

  // Ressources
  @GetMapping("/resources")
  public List<ResourceDto> listResources(){ return service.listResources(); }
  @PostMapping("/resources")
  public ResourceDto saveResource(@RequestBody ResourceDto r){ return service.saveResource(r); }
  @DeleteMapping("/resources/{id}")
  public void deleteResource(@PathVariable UUID id){ service.deleteResource(id); }

  // Interventions
  @GetMapping("/interventions")
  public List<InterventionDto> listInterventions(
      @RequestParam @DateTimeFormat(iso= DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso= DateTimeFormat.ISO.DATE) LocalDate to){
    return service.listInterventions(from, to);
  }
  @PostMapping("/interventions")
  public InterventionDto saveIntervention(@RequestBody InterventionDto i){ return service.saveIntervention(i); }
  @DeleteMapping("/interventions/{id}")
  public void deleteIntervention(@PathVariable UUID id){ service.deleteIntervention(id); }
}

