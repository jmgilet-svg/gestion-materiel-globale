package com.materiel.suite.backend.auth.dto;

/** Requête de création d'utilisateur (mode administrateur). */
public class UserCreateRequest {
  private UserV2Dto user;
  private String password;

  public UserV2Dto getUser(){
    return user;
  }

  public void setUser(UserV2Dto user){
    this.user = user;
  }

  public String getPassword(){
    return password;
  }

  public void setPassword(String password){
    this.password = password;
  }
}
