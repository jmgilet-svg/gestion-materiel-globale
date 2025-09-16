package com.materiel.suite.backend.planning;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryPlanningService implements PlanningService {
  private final Map<UUID, ResourceDto> resources = new ConcurrentHashMap<>();
  private final Map<UUID, InterventionDto> interventions = new ConcurrentHashMap<>();

  @jakarta.annotation.PostConstruct
  public void seed(){
    if (!resources.isEmpty()) return;
    ResourceDto r1 = new ResourceDto(UUID.randomUUID(), "Grue A");
    ResourceDto r2 = new ResourceDto(UUID.randomUUID(), "Grue B");
    ResourceDto r3 = new ResourceDto(UUID.randomUUID(), "Nacelle 18m");
    resources.put(r1.id(), r1); resources.put(r2.id(), r2); resources.put(r3.id(), r3);

    LocalDate base = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    add(new InterventionDto(UUID.randomUUID(), r1.id(), "BTP Construction",
        "Pont Anne-de-Bretagne", "GMK4100L-1", "Actros 26t", "Bruno", "Agence 1", "CONFIRMED",
        false, "Q-2025-018", "C-2025-003", "BL-1023", null, "#7CB9E8", false,
        base.atTime(9,0), base.atTime(17,0)));
    add(new InterventionDto(UUID.randomUUID(), r1.id(), "Durand BTP",
        "Passerelle Ouest", "AC 100", null, null, "Agence 2", "PLANNED",
        false, null, null, null, null, "#A3BE8C", false,
        base.plusDays(1).atTime(8,0), base.plusDays(1).atTime(12,30)));
  }
  private void add(InterventionDto d){ interventions.put(d.id(), d); }

  @Override public List<ResourceDto> listResources(){ return new ArrayList<>(resources.values()); }
  @Override public ResourceDto saveResource(ResourceDto r){
    UUID id = r.id()==null? UUID.randomUUID() : r.id();
    ResourceDto s = new ResourceDto(id, r.name());
    resources.put(id, s);
    return s;
  }
  @Override public void deleteResource(UUID id){ resources.remove(id); }

  @Override public List<InterventionDto> listInterventions(LocalDate from, LocalDate to){
    List<InterventionDto> list = new ArrayList<>();
    for (var i : interventions.values()){
      LocalDate sd = i.dateHeureDebut().toLocalDate();
      LocalDate ed = i.dateHeureFin().toLocalDate();
      if (!(ed.isBefore(from) || sd.isAfter(to))) list.add(i);
    }
    list.sort(Comparator.comparing(InterventionDto::dateHeureDebut));
    return list;
  }
  @Override public InterventionDto saveIntervention(InterventionDto i){
    UUID id = i.id()==null? UUID.randomUUID() : i.id();
    InterventionDto s = new InterventionDto(id, i.resourceId(), i.clientName(), i.siteLabel(), i.craneName(),
        i.truckName(), i.driverName(), i.agency(), i.status(), i.favorite(), i.quoteNumber(), i.orderNumber(),
        i.deliveryNumber(), i.invoiceNumber(), i.color(), i.locked(), i.dateHeureDebut(), i.dateHeureFin());
    interventions.put(id, s);
    return s;
  }
  @Override public void deleteIntervention(UUID id){ interventions.remove(id); }
}

