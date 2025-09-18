package com.materiel.suite.client.auth;

/** Agence / site auquel un utilisateur est rattaché. */
public class Agency {
  private String id;
  private String name;

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

  @Override
  public String toString(){
    return name == null || name.isBlank() ? id : name;
  }
}
