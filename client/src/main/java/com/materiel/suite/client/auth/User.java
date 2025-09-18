package com.materiel.suite.client.auth;

/** Informations utilisateur basiques pour le client lourd. */
public class User {
  private String id;
  private String username;
  private String displayName;
  private Role role;
  private Agency agency;

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

  public Role getRole(){
    return role;
  }

  public void setRole(Role role){
    this.role = role;
  }

  public Agency getAgency(){
    return agency;
  }

  public void setAgency(Agency agency){
    this.agency = agency;
  }
}
