package com.materiel.suite.backend.model;

import java.util.UUID;

public class ResourceRef {
  private UUID id;
  private String name;
  private String icon;

  public ResourceRef(){}
  public ResourceRef(UUID id, String name, String icon){ this.id=id; this.name=name; this.icon=icon; }

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name=name; }
  public String getIcon(){ return icon; }
  public void setIcon(String icon){ this.icon=icon; }
}
