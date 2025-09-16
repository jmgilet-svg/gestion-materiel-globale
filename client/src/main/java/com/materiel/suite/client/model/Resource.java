package com.materiel.suite.client.model;

import java.util.UUID;

public class Resource {
  private UUID id;
  private String name;
  private String type;
  private String color;
  private String notes;
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
  public String getType(){ return type; }
  public void setType(String type){ this.type=type; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
  public String getNotes(){ return notes; }
  public void setNotes(String notes){ this.notes=notes; }
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
