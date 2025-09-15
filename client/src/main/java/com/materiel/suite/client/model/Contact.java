package com.materiel.suite.client.model;

import java.util.UUID;

public class Contact {
  private UUID id;
  private UUID clientId;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private String role;
  private boolean archived;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public UUID getClientId(){ return clientId; }
  public void setClientId(UUID clientId){ this.clientId=clientId; }
  public String getFirstName(){ return firstName; }
  public void setFirstName(String v){ this.firstName=v; }
  public String getLastName(){ return lastName; }
  public void setLastName(String v){ this.lastName=v; }
  public String getEmail(){ return email; }
  public void setEmail(String v){ this.email=v; }
  public String getPhone(){ return phone; }
  public void setPhone(String v){ this.phone=v; }
  public String getRole(){ return role; }
  public void setRole(String v){ this.role=v; }
  public boolean isArchived(){ return archived; }
  public void setArchived(boolean b){ this.archived=b; }
  @Override public String toString(){ return (firstName!=null? firstName+" ":"") + (lastName!=null? lastName:""); }
}

