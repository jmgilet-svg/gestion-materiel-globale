package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.InterventionEntity;
import com.materiel.suite.backend.v1.domain.ClientEntity;
import com.materiel.suite.backend.v1.domain.ResourceEntity;
import com.materiel.suite.backend.v1.domain.InterventionStatus;
import com.materiel.suite.backend.v1.repo.InterventionRepository;
import com.materiel.suite.backend.v1.repo.ResourceRepository;
import com.materiel.suite.backend.v1.service.ChangeFeedService;
import com.materiel.suite.backend.v1.repo.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class PlanningController {
  private final ResourceRepository resources;
  private final InterventionRepository interventions;
  private final ChangeFeedService changes;
  private final ClientRepository clients;
  public PlanningController(ResourceRepository r, InterventionRepository i, ChangeFeedService ch, ClientRepository c){
    this.resources=r; this.interventions=i; this.changes=ch; this.clients=c;
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

  /* ===== Validation & duplication ===== */
  @PostMapping("/interventions/validate")
  public Map<String,Object> validate(@RequestBody Map<String,Object> body){
    InterventionEntity i = fromDto(body);
    // locked/done => refuse déplacement
    if (i.getStatus()==InterventionStatus.LOCKED || i.getStatus()==InterventionStatus.DONE){
      return Map.of("ok", false, "suggestions", List.of());
    }
    var overlaps = interventions.overlapByResource(i.getResource(), i.getStartDateTime(), i.getEndDateTime())
        .stream().filter(o -> !o.getId().equals(i.getId())).toList();
    if (overlaps.isEmpty()) return Map.of("ok", true, "suggestions", List.of());
    // suggestions : 1) placer après le dernier overlap
    LocalDateTime latest = overlaps.stream().map(InterventionEntity::getEndDateTime).max(LocalDateTime::compareTo).orElse(i.getEndDateTime());
    Duration dur = Duration.between(i.getStartDateTime(), i.getEndDateTime());
    Map<String,Object> s1 = Map.of(
        "label", "Décaler après le créneau suivant",
        "resourceId", i.getResource().getId().toString(),
        "startDateTime", latest.toString(),
        "endDateTime", latest.plus(dur).toString()
    );
    // 2) première autre ressource libre
    List<Map<String,Object>> sug = new ArrayList<>();
    sug.add(s1);
    for (ResourceEntity r : resources.findAll()){
      if (r.getId().equals(i.getResource().getId())) continue;
      var ol = interventions.overlapByResource(r, i.getStartDateTime(), i.getEndDateTime());
      if (ol.isEmpty()){ sug.add(Map.of("label","Basculer sur "+r.getName(), "resourceId", r.getId().toString(),
          "startDateTime", i.getStartDateTime().toString(), "endDateTime", i.getEndDateTime().toString())); break; }
    }
    return Map.of("ok", false, "suggestions", sug);
  }

  @PostMapping("/interventions/{id}/duplicate")
  public Map<String,Object> duplicate(@PathVariable UUID id, @RequestParam(defaultValue = "7") int days){
    InterventionEntity src = interventions.findById(id).orElseThrow();
    InterventionEntity c = new InterventionEntity();
    c.setId(UUID.randomUUID());
    c.setResource(src.getResource());
    c.setLabel(src.getLabel()+" (copie)");
    c.setColor(src.getColor());
    c.setStartDateTime(src.getStartDateTime().plusDays(days));
    c.setEndDateTime(src.getEndDateTime().plusDays(days));
    c.setStatus(src.getStatus());
    c = interventions.save(c);
    changes.emit("INTERVENTION_CREATED", c.getId().toString(), Map.of("resourceId", c.getResource().getId().toString()));
    return toDto(c);
  }

  /* ===== Mapping ===== */
  private Map<String,Object> toDto(InterventionEntity i){
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("id", i.getId().toString());
    m.put("resourceId", i.getResource().getId().toString());
    m.put("resource", Map.of("id", i.getResource().getId().toString(), "name", i.getResource().getName()));
    m.put("label", i.getLabel());
    m.put("color", i.getColor());
    if (i.getClient()!=null) m.put("clientId", i.getClient().getId().toString());
    if (i.getStartDateTime()!=null) m.put("startDateTime", i.getStartDateTime().toString());
    if (i.getEndDateTime()!=null) m.put("endDateTime", i.getEndDateTime().toString());
    m.put("dateDebut", i.getDateDebut()!=null? i.getDateDebut().toString() : null);
    m.put("dateFin", i.getDateFin()!=null? i.getDateFin().toString() : null);
    m.put("status", i.getStatus().name());
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
    // client optionnel
    String cid = String.valueOf(m.get("clientId"));
    if (StringUtils.hasText(cid)){
      ClientEntity ce = clients.findById(UUID.fromString(cid)).orElse(null);
      i.setClient(ce);
    }
    i.setLabel(String.valueOf(m.getOrDefault("label","")));
    Object color = m.get("color"); if (color!=null) i.setColor(String.valueOf(color));
    Object sdt = m.get("startDateTime"); if (sdt!=null) i.setStartDateTime(LocalDateTime.parse(String.valueOf(sdt)));
    Object edt = m.get("endDateTime"); if (edt!=null) i.setEndDateTime(LocalDateTime.parse(String.valueOf(edt)));
    // Fallback si pas d'heure
    if (i.getStartDateTime()==null) i.setStartDateTime(LocalDate.now().atTime(8,0));
    if (i.getEndDateTime()==null) i.setEndDateTime(i.getStartDateTime().plusHours(1));
    Object st = m.get("status");
    if (st!=null) i.setStatus(InterventionStatus.valueOf(String.valueOf(st)));
    return i;
  }
}
