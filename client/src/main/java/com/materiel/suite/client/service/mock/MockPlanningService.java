package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.service.PlanningService;

import java.time.LocalDate;
import java.time.LocalDateTime;
n
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockPlanningService implements PlanningService {
  private final Map<UUID, Resource> resources = new ConcurrentHashMap<>();
  private final Map<UUID, Intervention> interventions = new ConcurrentHashMap<>();

  public MockPlanningService(){
    if (resources.isEmpty()){
      Resource r1 = new Resource(UUID.randomUUID(), "Grue A");
      Resource r2 = new Resource(UUID.randomUUID(), "Grue B");
      Resource r3 = new Resource(UUID.randomUUID(), "Nacelle 18m");
      resources.put(r1.getId(), r1); resources.put(r2.getId(), r2); resources.put(r3.getId(), r3);
      LocalDate base = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
      add(new Intervention(UUID.randomUUID(), r1.getId(), "Chantier Alpha", base.plusDays(0), base.plusDays(2), "#5E81AC"));
      add(new Intervention(UUID.randomUUID(), r1.getId(), "Charpente Beta", base.plusDays(1), base.plusDays(3), "#A3BE8C"));
      add(new Intervention(UUID.randomUUID(), r1.getId(), "Levage Gamma", base.plusDays(2), base.plusDays(2), "#EBCB8B"));
      add(new Intervention(UUID.randomUUID(), r2.getId(), "Usine Delta", base.plusDays(0), base.plusDays(0), "#88C0D0"));
      add(new Intervention(UUID.randomUUID(), r2.getId(), "Entretien", base.plusDays(4), base.plusDays(5), "#BF616A"));
    }
  }
  private void add(Intervention i){ interventions.put(i.getId(), i); }

  @Override public List<Resource> listResources(){ return new ArrayList<>(resources.values()); }
  @Override public Resource saveResource(Resource r){ if(r.getId()==null) r.setId(UUID.randomUUID()); resources.put(r.getId(), r); return r; }
  @Override public void deleteResource(UUID id){ resources.remove(id); }

  @Override public List<Intervention> listInterventions(LocalDate from, LocalDate to){
    List<Intervention> list = new ArrayList<>();
    for (var i : interventions.values()){
      if (!(i.getDateFin().isBefore(from) || i.getDateDebut().isAfter(to))) list.add(i);
    }
    list.sort(Comparator.comparing(Intervention::getResourceId).thenComparing(Intervention::getDateDebut));
    return list;
  }
  @Override public Intervention saveIntervention(Intervention i){ if(i.getId()==null) i.setId(UUID.randomUUID()); interventions.put(i.getId(), i); return i; }
  @Override public void deleteIntervention(UUID id){ interventions.remove(id); }
  @Override public List<Conflict> listConflicts(LocalDate from, LocalDate to){
    List<Conflict> out = new ArrayList<>();
    for (Resource r : resources.values()){
      List<Intervention> list = listInterventions(from, to);
      list.removeIf(i -> !i.getResourceId().equals(r.getId()));
      list.sort(Comparator.comparing(Intervention::getDateDebut));
      LocalDate lastEnd = null; Intervention last = null;
      for (var it : list){
        if (lastEnd!=null && !it.getDateDebut().isAfter(lastEnd)){
          out.add(new Conflict(last.getId(), it.getId(), r.getId()));
        }
        if (lastEnd==null || it.getDateFin().isAfter(lastEnd)){
          lastEnd = it.getDateFin(); last = it;
        }
      }
    }
    return out;
  }
  @Override public boolean resolveShift(UUID id, int minutes){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    i.setDateHeureDebut(i.getDateHeureDebut().plusMinutes(minutes));
    i.setDateHeureFin(i.getDateHeureFin().plusMinutes(minutes));
    return true;
  }
  @Override public boolean resolveReassign(UUID id, UUID resourceId){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    for (var it : interventions.values()){
      if (!it.getResourceId().equals(resourceId) || it.getId().equals(id)) continue;
      boolean overlap = !i.getDateHeureFin().isBefore(it.getDateHeureDebut())
          && !i.getDateHeureDebut().isAfter(it.getDateHeureFin());
      if (overlap) return false;
    }
    i.setResourceId(resourceId);
    return true;
  }
  @Override public boolean resolveSplit(UUID id, LocalDateTime splitAt){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    if (!splitAt.isAfter(i.getDateHeureDebut()) || !i.getDateHeureFin().isAfter(splitAt)) return false;
    Intervention tail = new Intervention(UUID.randomUUID(), i.getResourceId(), i.getLabel()+" (suite)",
        splitAt.toLocalDate(), i.getDateHeureFin().toLocalDate(), i.getColor());
    tail.setDateHeureDebut(splitAt);
    tail.setDateHeureFin(i.getDateHeureFin());
    interventions.put(tail.getId(), tail);
    i.setDateHeureFin(splitAt.minusMinutes(1));
    return true;
  }
}
