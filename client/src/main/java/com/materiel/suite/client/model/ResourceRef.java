package com.materiel.suite.client.model;

import java.util.Objects;
import java.util.UUID;

/** Référence légère à une {@link Resource} pour embarquer nom et icône sans cycle complet. */
public class ResourceRef {
  private UUID id;
  private String name;
  private String icon;

  public ResourceRef(){}
  public ResourceRef(UUID id, String name, String icon){ this.id = id; this.name = name; this.icon = icon; }

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id = id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name = name; }
  public String getIcon(){ return icon; }
  public void setIcon(String icon){ this.icon = icon; }

  @Override public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof ResourceRef other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override public int hashCode(){ return Objects.hash(id); }
}
