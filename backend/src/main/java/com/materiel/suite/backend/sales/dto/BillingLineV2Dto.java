package com.materiel.suite.backend.sales.dto;

import java.math.BigDecimal;

/** Ligne de facturation simplifiée pour la génération de devis v2. */
public class BillingLineV2Dto {
  private String id;
  private String designation;
  private BigDecimal quantity;
  private String unit;
  private BigDecimal unitPriceHt;
  private BigDecimal totalHt;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getDesignation(){
    return designation;
  }

  public void setDesignation(String designation){
    this.designation = designation;
  }

  public BigDecimal getQuantity(){
    return quantity;
  }

  public void setQuantity(BigDecimal quantity){
    this.quantity = quantity;
  }

  public String getUnit(){
    return unit;
  }

  public void setUnit(String unit){
    this.unit = unit;
  }

  public BigDecimal getUnitPriceHt(){
    return unitPriceHt;
  }

  public void setUnitPriceHt(BigDecimal unitPriceHt){
    this.unitPriceHt = unitPriceHt;
  }

  public BigDecimal getTotalHt(){
    return totalHt;
  }

  public void setTotalHt(BigDecimal totalHt){
    this.totalHt = totalHt;
  }
}
