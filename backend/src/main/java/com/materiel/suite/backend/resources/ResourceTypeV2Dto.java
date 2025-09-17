package com.materiel.suite.backend.resources;

import java.math.BigDecimal;
import java.util.Objects;

public class ResourceTypeV2Dto {
  private String id;
  private String name;
  private String iconKey;
  private BigDecimal unitPriceHt;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getName(){
    return name;
  }

  public void setName(String name){
    this.name = name;
  }

  public String getIconKey(){
    return iconKey;
  }

  public void setIconKey(String iconKey){
    this.iconKey = iconKey;
  }

  public BigDecimal getUnitPriceHt(){
    return unitPriceHt;
  }

  public void setUnitPriceHt(BigDecimal unitPriceHt){
    this.unitPriceHt = unitPriceHt;
  }

  @Override
  public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof ResourceTypeV2Dto other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode(){
    return Objects.hash(id);
  }
}
