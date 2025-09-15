package com.materiel.suite.backend.v1.domain;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="client")
public class ClientEntity {
  @Id
  private UUID id;
  @Column(nullable=false)
  private String name;
  private String code;
  private String email;
  private String phone;
  private String vatNumber;
  @Column(length=2000)
  private String billingAddress;
  @Column(length=2000)
  private String shippingAddress;
  @Column(length=4000)
  private String notes;

  @OneToMany(mappedBy="client", cascade=CascadeType.ALL, orphanRemoval = true)
  private List<ContactEntity> contacts;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name=name; }
  public String getCode(){ return code; }
  public void setCode(String code){ this.code=code; }
  public String getEmail(){ return email; }
  public void setEmail(String email){ this.email=email; }
  public String getPhone(){ return phone; }
  public void setPhone(String phone){ this.phone=phone; }
  public String getVatNumber(){ return vatNumber; }
  public void setVatNumber(String vatNumber){ this.vatNumber=vatNumber; }
  public String getBillingAddress(){ return billingAddress; }
  public void setBillingAddress(String a){ this.billingAddress=a; }
  public String getShippingAddress(){ return shippingAddress; }
  public void setShippingAddress(String a){ this.shippingAddress=a; }
  public String getNotes(){ return notes; }
  public void setNotes(String notes){ this.notes=notes; }
  public List<ContactEntity> getContacts(){ return contacts; }
  public void setContacts(List<ContactEntity> contacts){ this.contacts=contacts; }
}

