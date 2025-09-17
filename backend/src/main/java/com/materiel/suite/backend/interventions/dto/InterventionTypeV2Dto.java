package com.materiel.suite.backend.interventions.dto;

/** DTO simple pour exposer les types d'intervention via l'API v2. */
public class InterventionTypeV2Dto {
  private String id;
  private String name;
  private String iconKey;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getName(){
    return name;
  }

  public void setName(String name){
    this.name = name;
  }

  public String getIconKey(){
    return iconKey;
  }

  public void setIconKey(String iconKey){
    this.iconKey = iconKey;
  }
}
