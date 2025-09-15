package com.materiel.suite.backend.v1.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;
import java.util.List;

@Entity
@Table(name="resource")
public class ResourceEntity {
  @Id
  private UUID id;
  @Column(nullable=false)
  private String name;
  @JsonIgnore
  @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InterventionEntity> interventions;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name=name; }
}
