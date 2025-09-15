package com.materiel.suite.backend.v1.domain;

import java.math.BigDecimal;

public class DocLine {
  private String designation;
  private BigDecimal qty;
  private String unit;
  private BigDecimal unitPrice; // HT
  private BigDecimal discountPct; // 0..100
  private BigDecimal vatPct; // 0..100

  // computed (renvoyé au client)
  private BigDecimal lineHt;
  private BigDecimal lineVat;
  private BigDecimal lineTtc;

  public String getDesignation() { return designation; }
  public void setDesignation(String designation) { this.designation = designation; }
  public BigDecimal getQty() { return qty; }
  public void setQty(BigDecimal qty) { this.qty = qty; }
  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public BigDecimal getDiscountPct() { return discountPct; }
  public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
  public BigDecimal getVatPct() { return vatPct; }
  public void setVatPct(BigDecimal vatPct) { this.vatPct = vatPct; }
  public BigDecimal getLineHt() { return lineHt; }
  public void setLineHt(BigDecimal lineHt) { this.lineHt = lineHt; }
  public BigDecimal getLineVat() { return lineVat; }
  public void setLineVat(BigDecimal lineVat) { this.lineVat = lineVat; }
  public BigDecimal getLineTtc() { return lineTtc; }
  public void setLineTtc(BigDecimal lineTtc) { this.lineTtc = lineTtc; }
}
