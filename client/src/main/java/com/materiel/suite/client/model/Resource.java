package com.materiel.suite.client.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Resource {
  private UUID id;
  private String name;
  private ResourceType type;
  private BigDecimal unitPriceHt;
  private String color;
  private String notes;
  private String state;
  private String email;
  private String agencyId;
  private final List<Unavailability> unavailabilities = new ArrayList<>();
  // === CRM-INJECT BEGIN: resource-advanced-fields ===
  private Integer capacity = 1;
  private String tags;
  private String weeklyUnavailability;
  // === CRM-INJECT END ===

  public Resource(){}
  public Resource(UUID id, String name){ this.id=id; this.name=name; }
  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name=name; }
  public ResourceType getType(){ return type; }
  public void setType(ResourceType type){ this.type=type; }
  public BigDecimal getUnitPriceHt(){ return unitPriceHt; }
  public void setUnitPriceHt(BigDecimal unitPriceHt){ this.unitPriceHt = unitPriceHt; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
  public String getNotes(){ return notes; }
  public void setNotes(String notes){ this.notes=notes; }
  public String getState(){ return state; }
  public void setState(String state){ this.state=state; }
  public String getEmail(){ return email; }
  public void setEmail(String email){ this.email=email; }
  public String getAgencyId(){ return agencyId; }
  public void setAgencyId(String agencyId){ this.agencyId = agencyId; }
  public List<Unavailability> getUnavailabilities(){ return unavailabilities; }
  public void setUnavailabilities(List<Unavailability> list){
    unavailabilities.clear();
    if (list!=null) unavailabilities.addAll(list);
  }
  public String getTypeCode(){ return type!=null? type.getCode():null; }
  public String getTypeLabel(){ return type!=null? type.getLabel():null; }
  // === CRM-INJECT BEGIN: resource-advanced-accessors ===
  public Integer getCapacity(){ return capacity; }
  public void setCapacity(Integer capacity){ this.capacity = (capacity==null || capacity<1)? 1 : capacity; }
  public String getTags(){ return tags; }
  public void setTags(String tags){ this.tags=tags; }
  public String getWeeklyUnavailability(){ return weeklyUnavailability; }
  public void setWeeklyUnavailability(String weeklyUnavailability){ this.weeklyUnavailability=weeklyUnavailability; }
  // === CRM-INJECT END ===
  @Override public String toString(){ return name; }
}
