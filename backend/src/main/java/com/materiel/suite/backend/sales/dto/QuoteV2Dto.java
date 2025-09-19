package com.materiel.suite.backend.sales.dto;

import java.math.BigDecimal;

/** DTO minimal pour exposer un devis v2 via l'API mock. */
public class QuoteV2Dto {
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
