package com.materiel.suite.client.model;

import java.util.Objects;

public class ResourceType {
  private String code;
  private String label;

  public ResourceType(){}
  public ResourceType(String code, String label){ this.code = code; this.label = label; }

  public String getCode(){ return code; }
  public void setCode(String code){ this.code = code; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label = label; }

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
