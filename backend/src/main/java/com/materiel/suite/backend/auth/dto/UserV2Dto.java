package com.materiel.suite.backend.auth.dto;

public class UserV2Dto {
  private String id;
  private String username;
  private String displayName;
  private String role;
  private AgencyV2Dto agency;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getUsername(){
    return username;
  }

  public void setUsername(String username){
    this.username = username;
  }

  public String getDisplayName(){
    return displayName;
  }

  public void setDisplayName(String displayName){
    this.displayName = displayName;
  }

  public String getRole(){
    return role;
  }

  public void setRole(String role){
    this.role = role;
  }

  public AgencyV2Dto getAgency(){
    return agency;
  }

  public void setAgency(AgencyV2Dto agency){
    this.agency = agency;
  }
}
