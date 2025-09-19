package com.materiel.suite.backend.intervention.dto;

import java.math.BigDecimal;
import java.util.List;

/** Simple DTO exposant les modèles d'intervention (v2). */
public class InterventionTemplateV2Dto {
  public static class TemplateLine {
    private String designation;
    private String unit;
    private BigDecimal unitPriceHt;
    private BigDecimal quantity;

    public String getDesignation(){
      return designation;
    }

    public void setDesignation(String designation){
      this.designation = designation;
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

    public BigDecimal getQuantity(){
      return quantity;
    }

    public void setQuantity(BigDecimal quantity){
      this.quantity = quantity;
    }
  }

  private String id;
  private String name;
  private String defaultTypeId;
  private Integer defaultDurationMinutes;
  private List<String> suggestedResourceTypeIds;
  private List<TemplateLine> defaultLines;

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

  public String getDefaultTypeId(){
    return defaultTypeId;
  }

  public void setDefaultTypeId(String defaultTypeId){
    this.defaultTypeId = defaultTypeId;
  }

  public Integer getDefaultDurationMinutes(){
    return defaultDurationMinutes;
  }

  public void setDefaultDurationMinutes(Integer defaultDurationMinutes){
    this.defaultDurationMinutes = defaultDurationMinutes;
  }

  public List<String> getSuggestedResourceTypeIds(){
    return suggestedResourceTypeIds;
  }

  public void setSuggestedResourceTypeIds(List<String> suggestedResourceTypeIds){
    this.suggestedResourceTypeIds = suggestedResourceTypeIds;
  }

  public List<TemplateLine> getDefaultLines(){
    return defaultLines;
  }

  public void setDefaultLines(List<TemplateLine> defaultLines){
    this.defaultLines = defaultLines;
  }
}
