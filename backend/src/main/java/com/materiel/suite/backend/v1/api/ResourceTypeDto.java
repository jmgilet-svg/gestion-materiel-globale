package com.materiel.suite.backend.v1.api;

import java.util.Objects;

public class ResourceTypeDto {
  private String code;
  private String label;
  private String icon;

  public ResourceTypeDto(){}
  public ResourceTypeDto(String code, String label, String icon){
    this.code = code;
    this.label = label;
    this.icon = icon;
  }

  public String getCode(){ return code; }
  public void setCode(String code){ this.code = code; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label = label; }
  public String getIcon(){ return icon; }
  public void setIcon(String icon){ this.icon = icon; }

  @Override public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof ResourceTypeDto other)) return false;
    return Objects.equals(code, other.code);
  }

  @Override public int hashCode(){ return Objects.hash(code); }
}
