package com.materiel.suite.client.model;

import java.util.Objects;

public class ResourceType {
  private String id;
  private String name;
  private String iconKey;

  public ResourceType(){}
  public ResourceType(String code, String label){ this.id = code; this.name = label; }
  public ResourceType(String code, String label, String icon){ this.id = code; this.name = label; this.iconKey = icon; }

  public String getId(){ return id; }
  public void setId(String id){ this.id = id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name = name; }
  public String getIconKey(){ return iconKey; }
  public void setIconKey(String iconKey){ this.iconKey = iconKey; }

  public String getCode(){ return id; }
  public void setCode(String code){ this.id = code; }
  public String getLabel(){ return name; }
  public void setLabel(String label){ this.name = label; }
  public String getIcon(){ return iconKey; }
  public void setIcon(String icon){ this.iconKey = icon; }

  @Override public String toString(){
    if (name!=null && !name.isBlank()) return name;
    return id;
  }

  @Override public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof ResourceType)) return false;
    ResourceType that = (ResourceType) o;
    return Objects.equals(getCode(), that.getCode());
  }

  @Override public int hashCode(){
    return Objects.hash(getCode());
  }
}
