package com.materiel.suite.client.model;

import java.util.Objects;

public class ResourceType {
  private String code;
  private String label;
  private String icon;

  public ResourceType(){}
  public ResourceType(String code, String label){ this.code = code; this.label = label; }
  public ResourceType(String code, String label, String icon){ this.code = code; this.label = label; this.icon = icon; }

  public String getCode(){ return code; }
  public void setCode(String code){ this.code = code; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label = label; }
  public String getIcon(){ return icon; }
  public void setIcon(String icon){ this.icon = icon; }

  @Override public String toString(){
    if (label!=null && !label.isBlank()) return label;
    return code;
  }

  @Override public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof ResourceType)) return false;
    ResourceType that = (ResourceType) o;
    return Objects.equals(code, that.code);
  }

  @Override public int hashCode(){
    return Objects.hash(code);
  }
}
