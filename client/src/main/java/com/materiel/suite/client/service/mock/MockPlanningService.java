package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.PlanningValidation;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockPlanningService implements PlanningService {
  private final Map<UUID, Resource> resources = new ConcurrentHashMap<>();
  private final Map<String, ResourceType> resourceTypes = new LinkedHashMap<>();
  private final Map<UUID, Intervention> interventions = new ConcurrentHashMap<>();

  public MockPlanningService(){
    resourceTypes.put("CRANE", new ResourceType("CRANE", "Grue", "🏗️"));
    resourceTypes.put("TRUCK", new ResourceType("TRUCK", "Camion", "🚚"));
    resourceTypes.put("PLATFORM", new ResourceType("PLATFORM", "Nacelle", "🛠️"));
    resourceTypes.put("GENERIC", new ResourceType("GENERIC", "Ressource", "🏷️"));
    if (resources.isEmpty()){
      Resource r1 = new Resource(UUID.randomUUID(), "Grue A"); r1.setType(resourceTypes.get("CRANE"));
      r1.setUnitPriceHt(new BigDecimal("130.00"));
      Resource r2 = new Resource(UUID.randomUUID(), "Grue B"); r2.setType(resourceTypes.get("CRANE"));
      r2.setUnitPriceHt(new BigDecimal("120.00"));
      Resource r3 = new Resource(UUID.randomUUID(), "Nacelle 18m"); r3.setType(resourceTypes.get("PLATFORM"));
      r3.setUnitPriceHt(new BigDecimal("95.00"));
      // === CRM-INJECT BEGIN: resource-mock-defaults ===
      r1.setCapacity(2); r1.setTags("grue,90t"); r1.setWeeklyUnavailability("MON 08:00-12:00; THU 13:00-17:00");
      r2.setCapacity(1); r2.setTags("grue,60t"); r2.setWeeklyUnavailability("TUE 08:00-12:00");
      r3.setCapacity(1); r3.setTags("nacelle"); r3.setWeeklyUnavailability("FRI 14:00-18:00");
      // === CRM-INJECT END ===
      r1.getUnavailabilities().add(new Unavailability(UUID.randomUUID(),
          LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0),
          LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0),
          "Maintenance"));
      resources.put(r1.getId(), r1); resources.put(r2.getId(), r2); resources.put(r3.getId(), r3);
      LocalDate base = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
      add(attachPrimary(fillCard(new Intervention(UUID.randomUUID(), r1.getId(), "Chantier Alpha", base.plusDays(0), base.plusDays(2), "#5E81AC"),
          "Durand BTP","Pont Anne-de-Bretagne","GMK4100L-1","Actros 26t","Bernard","Agence 1","PLANNED",true,
          "Q-2025-014","C-2025-003",null,null,
          LocalDateTime.of(base.plusDays(0), java.time.LocalTime.of(8,0)),
          LocalDateTime.of(base.plusDays(0), java.time.LocalTime.of(12,30))), r1));
      add(attachPrimary(fillCard(new Intervention(UUID.randomUUID(), r1.getId(), "Charpente Beta", base.plusDays(1), base.plusDays(3), "#A3BE8C"),
          "BTP Construction","Hall logistique","LTM 1050","Actros 26t","Bruno","Agence 1","CONFIRMED",false,
          "Q-2025-018",null,null,null,
          LocalDateTime.of(base.plusDays(1), java.time.LocalTime.of(9,0)),
          LocalDateTime.of(base.plusDays(1), java.time.LocalTime.of(17,0))), r1));
      add(attachPrimary(fillCard(new Intervention(UUID.randomUUID(), r1.getId(), "Levage Gamma", base.plusDays(2), base.plusDays(2), "#EBCB8B"),
          "Trx Publics","Passerelle Ouest","AC 100","—","Camille","Agence 2","PLANNED",false,
          null,null,"BL-1023",null,
          LocalDateTime.of(base.plusDays(2), java.time.LocalTime.of(7,30)),
          LocalDateTime.of(base.plusDays(2), java.time.LocalTime.of(15,45))), r1));
      Intervention i4 = new Intervention(UUID.randomUUID(), r2.getId(), "Usine Delta", base.plusDays(0), base.plusDays(0), "#88C0D0");
      attachPrimary(i4, r2);
      add(i4);
      Intervention i5 = new Intervention(UUID.randomUUID(), r2.getId(), "Entretien", base.plusDays(4), base.plusDays(5), "#BF616A");
      attachPrimary(i5, r2);
      add(i5);
    }
  }
  private void add(Intervention i){ interventions.put(i.getId(), i); }
  private Intervention fillCard(Intervention i, String client, String site, String crane, String truck, String driver, String agency,
                                String status, boolean fav, String quote, String order, String dn, String inv,
                                LocalDateTime deb, LocalDateTime fin){
    i.setClientName(client); i.setSiteLabel(site); i.setCraneName(crane); i.setTruckName(truck);
    i.setDriverName(driver); i.setAgency(agency); i.setStatus(status); i.setFavorite(fav);
    i.setQuoteNumber(quote); i.setOrderNumber(order); i.setDeliveryNumber(dn); i.setInvoiceNumber(inv);
    i.setDateHeureDebut(deb); i.setDateHeureFin(fin);
    return i;
  }

  private Intervention attachPrimary(Intervention i, Resource r){
    if (i!=null && r!=null){
      i.setResources(List.of(new ResourceRef(r.getId(), r.getName(), iconOf(r))));
    }
    return i;
  }

  @Override public List<Resource> listResources(){ return new ArrayList<>(resources.values()); }
  @Override public Resource saveResource(Resource r){
    if(r.getId()==null) r.setId(UUID.randomUUID());
    resources.put(r.getId(), r);
    return r;
  }
  @Override public void deleteResource(UUID id){ resources.remove(id); }
  @Override public List<ResourceType> listResourceTypes(){ return new ArrayList<>(resourceTypes.values()); }
  @Override public ResourceType createResourceType(ResourceType type){
    ResourceType copy = copyType(type);
    resourceTypes.put(copy.getCode(), copy);
    return copy;
  }
  @Override public ResourceType updateResourceType(ResourceType type){
    ResourceType copy = copyType(type);
    resourceTypes.put(copy.getCode(), copy);
    return copy;
  }
  @Override public void deleteResourceType(String code){
    if (code==null || code.isBlank()) return;
    resourceTypes.remove(code);
    ResourceType fallback = resourceTypes.get("GENERIC");
    for (Resource r : resources.values()){
      ResourceType t = r.getType();
      if (t!=null && code.equals(t.getCode())){
        r.setType(fallback);
      }
    }
  }
  @Override public List<Unavailability> listResourceUnavailabilities(UUID resourceId){
    Resource r = resources.get(resourceId);
    return r==null? List.of() : new ArrayList<>(r.getUnavailabilities());
  }
  @Override public Unavailability addUnavailability(UUID resourceId, Unavailability u){
    Resource r = resources.get(resourceId);
    if (r==null) throw new NoSuchElementException("resource not found");
    Unavailability copy = new Unavailability(
        u.getId()!=null? u.getId():UUID.randomUUID(),
        u.getStart(),
        u.getEnd(),
        u.getReason()
    );
    r.getUnavailabilities().add(copy);
    return copy;
  }
  @Override public void deleteUnavailability(UUID resourceId, UUID unavailabilityId){
    Resource r = resources.get(resourceId);
    if (r==null) return;
    if (unavailabilityId==null) return;
    r.getUnavailabilities().removeIf(u -> unavailabilityId.equals(u.getId()));
  }

  @Override public List<Intervention> listInterventions(LocalDate from, LocalDate to){
    List<Intervention> list = new ArrayList<>();
    for (var i : interventions.values()){
      if (!(i.getDateFin().isBefore(from) || i.getDateDebut().isAfter(to))) list.add(i);
    }
    list.sort(Comparator.comparing(Intervention::getResourceId).thenComparing(Intervention::getDateDebut));
    return list;
  }
  @Override public Intervention saveIntervention(Intervention i){
    if(i.getId()==null) i.setId(UUID.randomUUID());
    if (i.getResourceId()!=null && i.getResources().isEmpty()){
      Resource r = resources.get(i.getResourceId());
      if (r!=null){
        i.setResources(List.of(new ResourceRef(r.getId(), r.getName(), iconOf(r))));
      }
    }
    interventions.put(i.getId(), i);
    return i;
  }
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
    Resource target = resources.get(resourceId);
    if (target!=null){
      i.setResources(List.of(new ResourceRef(target.getId(), target.getName(), iconOf(target))));
    } else {
      i.setResources(List.of());
    }
    return true;
  }
  @Override public boolean resolveSplit(UUID id, LocalDateTime splitAt){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    if (!splitAt.isAfter(i.getDateHeureDebut()) || !i.getDateHeureFin().isAfter(splitAt)) return false;
    Intervention tail = new Intervention(UUID.randomUUID(), i.getResourceId(), i.getLabel()+" (suite)",
        splitAt.toLocalDate(), i.getDateHeureFin().toLocalDate(), i.getColor());
    tail.setResources(i.getResources());
    tail.setDateHeureDebut(splitAt);
    tail.setDateHeureFin(i.getDateHeureFin());
    interventions.put(tail.getId(), tail);
    i.setDateHeureFin(splitAt.minusMinutes(1));
    return true;
  }

  private ResourceType copyType(ResourceType type){
    if (type==null || type.getCode()==null || type.getCode().isBlank()){
      throw new IllegalArgumentException("code requis");
    }
    String code = type.getCode();
    ResourceType existing = resourceTypes.get(code);
    String label = type.getLabel();
    if (label==null || label.isBlank()){
      if (existing!=null && existing.getLabel()!=null && !existing.getLabel().isBlank()) label = existing.getLabel();
      else label = code;
    }
    String icon = type.getIcon();
    if (icon==null || icon.isBlank()){
      if (existing!=null && existing.getIcon()!=null && !existing.getIcon().isBlank()) icon = existing.getIcon();
    }
    return new ResourceType(code, label, icon);
  }

  private String iconOf(Resource r){
    if (r==null) return null;
    ResourceType t = r.getType();
    return t!=null? t.getIcon() : null;
  }

  @Override public PlanningValidation validate(Intervention i){
    PlanningValidation v = new PlanningValidation();
    LocalDateTime s = i.getDateHeureDebut();
    LocalDateTime e = i.getDateHeureFin();
    if (s==null || e==null){ v.ok = true; return v; }
    for (var other : interventions.values()){
      if (i.getId()!=null && i.getId().equals(other.getId())) continue;
      if (!Objects.equals(i.getResourceId(), other.getResourceId())) continue;
      LocalDateTime os = other.getDateHeureDebut();
      LocalDateTime oe = other.getDateHeureFin();
      boolean overlap = !e.isBefore(os) && !oe.isBefore(s);
      if (overlap){
        v.ok = false;
        PlanningValidation.Suggestion sug = new PlanningValidation.Suggestion();
        sug.resourceId = i.getResourceId();
        Duration dur = Duration.between(s,e);
        sug.startDateTime = oe;
        sug.endDateTime = oe.plus(dur);
        sug.label = "Décaler après le créneau suivant";
        v.suggestions.add(sug);
      }
    }
    if (v.suggestions.isEmpty()) v.ok = true;
    return v;
  }
}
