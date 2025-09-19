package com.materiel.suite.client.model;

import java.math.BigDecimal;

/** Représentation légère d'un devis retourné par l'API v2. */
public class QuoteV2 {
  private String id;
  private String reference;
  private String status;
  private BigDecimal totalHt;
  private BigDecimal totalTtc;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getReference(){
    return reference;
  }

  public void setReference(String reference){
    this.reference = reference;
  }

  public String getStatus(){
    return status;
  }

  public void setStatus(String status){
    this.status = status;
  }

  public BigDecimal getTotalHt(){
    return totalHt;
  }

  public void setTotalHt(BigDecimal totalHt){
    this.totalHt = totalHt;
  }

  public BigDecimal getTotalTtc(){
    return totalTtc;
  }

  public void setTotalTtc(BigDecimal totalTtc){
    this.totalTtc = totalTtc;
  }
}
