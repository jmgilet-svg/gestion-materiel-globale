package com.materiel.suite.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Resource {
  private UUID id;
  private String name;
  private String icon;
  private BigDecimal unitPriceHt;
  public Resource(){}
  public Resource(UUID id, String name){ this.id=id; this.name=name; }
  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name=name; }
  public String getIcon(){ return icon; }
  public void setIcon(String icon){ this.icon=icon; }
  public BigDecimal getUnitPriceHt(){ return unitPriceHt; }
  public void setUnitPriceHt(BigDecimal unitPriceHt){ this.unitPriceHt = unitPriceHt; }
}
