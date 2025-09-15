package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.InterventionEntity;
import com.materiel.suite.backend.v1.domain.ResourceEntity;
import com.materiel.suite.backend.v1.repo.InterventionRepository;
import com.materiel.suite.backend.v1.repo.ResourceRepository;
import com.materiel.suite.backend.v1.service.ChangeFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class PlanningController {
  private final ResourceRepository resources;
  private final InterventionRepository interventions;
  private final ChangeFeedService changes;
  public PlanningController(ResourceRepository r, InterventionRepository i, ChangeFeedService ch){
    this.resources=r; this.interventions=i; this.changes=ch;
  }

  /* ===== Resources ===== */
  @GetMapping("/resources")
  public List<ResourceEntity> listResources(){ return resources.findAll(); }

  @PostMapping("/resources")
  public ResourceEntity createResource(@RequestBody ResourceEntity r){
    if (r.getId()==null) r.setId(UUID.randomUUID());
    return resources.save(r);
  }
  @PutMapping("/resources/{id}")
  public ResourceEntity updateResource(@PathVariable UUID id, @RequestBody ResourceEntity r){
    r.setId(id); return resources.save(r);
  }
  @DeleteMapping("/resources/{id}")
  public ResponseEntity<Void> deleteResource(@PathVariable UUID id){
    resources.deleteById(id); return ResponseEntity.noContent().build();
  }

  /* ===== Interventions ===== */
  @GetMapping("/interventions")
  public List<Map<String,Object>> listInterventions(@RequestParam LocalDate from, @RequestParam LocalDate to){
    LocalDateTime f = from.atStartOfDay();
    LocalDateTime t = to.atTime(23,59,59);
    return interventions.overlap(f,t).stream().map(this::toDto).collect(Collectors.toList());
  }

  @PostMapping("/interventions")
  public Map<String,Object> createIntervention(@RequestBody Map<String,Object> body){
    InterventionEntity i = fromDto(body);
    if (i.getId()==null) i.setId(UUID.randomUUID());
    InterventionEntity saved = interventions.save(i);
    changes.emit("INTERVENTION_CREATED", saved.getId().toString(), Map.of("resourceId", saved.getResource().getId().toString()));
    return toDto(saved);
  }
  @PutMapping("/interventions/{id}")
  public Map<String,Object> updateIntervention(@PathVariable UUID id, @RequestBody Map<String,Object> body){
    InterventionEntity i = fromDto(body); i.setId(id);
    InterventionEntity saved = interventions.save(i);
    changes.emit("INTERVENTION_UPDATED", saved.getId().toString(), Map.of("resourceId", saved.getResource().getId().toString()));
    return toDto(saved);
  }
  @DeleteMapping("/interventions/{id}")
  public ResponseEntity<Void> deleteIntervention(@PathVariable UUID id){
    interventions.deleteById(id);
    changes.emit("INTERVENTION_DELETED", id.toString(), Map.of());
    return ResponseEntity.noContent().build();
  }

  /* ===== Mapping ===== */
  private Map<String,Object> toDto(InterventionEntity i){
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("id", i.getId().toString());
    m.put("resourceId", i.getResource().getId().toString());
    m.put("resource", Map.of("id", i.getResource().getId().toString(), "name", i.getResource().getName()));
    m.put("label", i.getLabel());
    m.put("color", i.getColor());
    if (i.getStartDateTime()!=null) m.put("startDateTime", i.getStartDateTime().toString());
    if (i.getEndDateTime()!=null) m.put("endDateTime", i.getEndDateTime().toString());
    m.put("dateDebut", i.getDateDebut()!=null? i.getDateDebut().toString() : null);
    m.put("dateFin", i.getDateFin()!=null? i.getDateFin().toString() : null);
    return m;
  }
  private InterventionEntity fromDto(Map<String,Object> m){
    InterventionEntity i = new InterventionEntity();
    Object id = m.get("id");
    if (id!=null) i.setId(UUID.fromString(String.valueOf(id)));
    String rid = String.valueOf(m.get("resourceId"));
    if (!StringUtils.hasText(rid) && m.get("resource") instanceof Map<?,?> rmap){
      Object rr = ((Map<?,?>) rmap).get("id");
      if (rr!=null) rid = String.valueOf(rr);
    }
    ResourceEntity r = resources.findById(UUID.fromString(rid)).orElseThrow();
    i.setResource(r);
    i.setLabel(String.valueOf(m.getOrDefault("label","")));
    Object color = m.get("color"); if (color!=null) i.setColor(String.valueOf(color));
    Object sdt = m.get("startDateTime"); if (sdt!=null) i.setStartDateTime(LocalDateTime.parse(String.valueOf(sdt)));
    Object edt = m.get("endDateTime"); if (edt!=null) i.setEndDateTime(LocalDateTime.parse(String.valueOf(edt)));
    // Fallback si pas d'heure
    if (i.getStartDateTime()==null) i.setStartDateTime(LocalDate.now().atTime(8,0));
    if (i.getEndDateTime()==null) i.setEndDateTime(i.getStartDateTime().plusHours(1));
    return i;
  }
}
