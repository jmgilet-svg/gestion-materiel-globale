package com.materiel.suite.client.model;

import java.util.UUID;

public class Resource {
  private UUID id;
  private String name;
  private String type;
  private String color;
  private String notes;
  
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
  @Override public String toString(){ return name; }
}
