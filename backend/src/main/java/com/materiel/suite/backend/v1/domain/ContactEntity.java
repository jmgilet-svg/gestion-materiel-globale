package com.materiel.suite.backend.v1.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="contact")
public class ContactEntity {
  @Id
  private UUID id;
  @ManyToOne(optional=false)
  @JoinColumn(name="client_id")
  private ClientEntity client;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private String role;
  private boolean archived;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public ClientEntity getClient(){ return client; }
  public void setClient(ClientEntity c){ this.client=c; }
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
}

