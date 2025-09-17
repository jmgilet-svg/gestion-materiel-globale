package com.materiel.suite.client.model;

import java.util.Objects;

/** Type d'intervention paramétrable (nom + icône optionnelle). */
public class InterventionType {
  private String code;
  private String label;
  private String iconKey;

  public InterventionType(){}

  public InterventionType(String code, String label){
    this(code, label, null);
  }

  public InterventionType(String code, String label, String iconKey){
    this.code = code;
    this.label = label;
    this.iconKey = iconKey;
  }

  public String getCode(){ return code; }
  public void setCode(String code){ this.code = code; }

  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label = label; }

  public String getIconKey(){ return iconKey; }
  public void setIconKey(String iconKey){ this.iconKey = iconKey; }

  @Override public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof InterventionType other)) return false;
    return Objects.equals(code, other.code);
  }

  @Override public int hashCode(){
    return Objects.hash(code);
  }

  @Override public String toString(){
    if (label != null && !label.isBlank()){
      return label;
    }
    return code != null ? code : "";
  }
}
