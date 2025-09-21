package com.materiel.suite.client.model;

import java.util.UUID;

public class Client {
  private UUID id;
  private String name;
  private String code;
  private String email;
  private String phone;
  private String vatNumber;
  private String billingAddress;
  private String shippingAddress;
  private String notes;
  private String agencyId;

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
  public String getAgencyId(){ return agencyId; }
  public void setAgencyId(String agencyId){ this.agencyId = agencyId; }
  @Override public String toString(){ return name; }
}

