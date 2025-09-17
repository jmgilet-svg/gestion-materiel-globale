package com.materiel.suite.backend.store;

import com.materiel.suite.backend.model.Intervention;
import com.materiel.suite.backend.model.Resource;
import com.materiel.suite.backend.model.ResourceRef;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryStore {
  private final Map<UUID, Resource> resources = new ConcurrentHashMap<>();
  private final Map<UUID, Intervention> interventions = new ConcurrentHashMap<>();

  @PostConstruct
  public void seed(){
    if (!resources.isEmpty()) return;
    Resource r1 = new Resource(UUID.randomUUID(), "Grue A"); r1.setIcon("🏗️"); r1.setUnitPriceHt(new BigDecimal("130.00"));
    Resource r2 = new Resource(UUID.randomUUID(), "Grue B"); r2.setIcon("🏗️"); r2.setUnitPriceHt(new BigDecimal("120.00"));
    Resource r3 = new Resource(UUID.randomUUID(), "Nacelle 18m"); r3.setIcon("🛠️"); r3.setUnitPriceHt(new BigDecimal("95.00"));
    resources.put(r1.getId(), r1);
    resources.put(r2.getId(), r2);
    resources.put(r3.getId(), r3);
    LocalDate mon = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    addInter(r1, "Chantier Alpha", mon.atTime(8,0), mon.plusDays(2).atTime(17,0), "#5E81AC");
    addInter(r1, "Charpente Beta", mon.plusDays(1).atTime(9,0), mon.plusDays(3).atTime(12,0), "#A3BE8C");
    addInter(r1, "Levage Gamma", mon.plusDays(2).atTime(8,0), mon.plusDays(2).atTime(16,0), "#EBCB8B");
    addInter(r2, "Usine Delta", mon.atTime(13,0), mon.atTime(18,0), "#88C0D0");
    addInter(r2, "Entretien", mon.plusDays(4).atTime(8,0), mon.plusDays(5).atTime(12,0), "#BF616A");
  }
  private void addInter(Resource r, String label, LocalDateTime s, LocalDateTime e, String color){
    Intervention it = new Intervention();
    it.setId(UUID.randomUUID());
    it.setResourceId(r.getId());
    it.setResources(List.of(new ResourceRef(r.getId(), r.getName(), r.getIcon())));
    it.setLabel(label);
    it.setDateHeureDebut(s);
    it.setDateHeureFin(e);
    it.setColor(color);
    interventions.put(it.getId(), it);
  }

  // Resources
  public List<Resource> resources(){ return new ArrayList<>(resources.values()); }
  public Resource save(Resource r){ if (r.getId()==null) r.setId(UUID.randomUUID()); resources.put(r.getId(), r); return r; }
  public void deleteResource(UUID id){ resources.remove(id); }

  // Interventions
  public List<Intervention> interventions(LocalDate from, LocalDate to){
    LocalDateTime s = from.atStartOfDay(), e = to.plusDays(1).atStartOfDay();
    return interventions.values().stream()
        .filter(i -> !(i.getDateHeureFin().isBefore(s) || i.getDateHeureDebut().isAfter(e)))
        .sorted(Comparator.comparing(Intervention::getDateHeureDebut))
        .collect(Collectors.toList());
  }
  public Intervention save(Intervention i){
    if (i.getId()==null) i.setId(UUID.randomUUID());
    if (i.getResourceId()!=null && i.getResources().isEmpty()){
      Resource r = resources.get(i.getResourceId());
      if (r!=null){
        i.setResources(List.of(new ResourceRef(r.getId(), r.getName(), r.getIcon())));
      }
    }
    interventions.put(i.getId(), i);
    return i;
  }
  public void deleteIntervention(UUID id){ interventions.remove(id); }
  public Optional<Intervention> findIntervention(UUID id){ return Optional.ofNullable(interventions.get(id)); }
}
