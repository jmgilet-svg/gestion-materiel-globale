package com.materiel.suite.backend.auth.dto;

public class LoginV2Request {
  private String agencyId;
  private String username;
  private String password;

  public String getAgencyId(){
    return agencyId;
  }

  public void setAgencyId(String agencyId){
    this.agencyId = agencyId;
  }

  public String getUsername(){
    return username;
  }

  public void setUsername(String username){
    this.username = username;
  }

  public String getPassword(){
    return password;
  }

  public void setPassword(String password){
    this.password = password;
  }
}
