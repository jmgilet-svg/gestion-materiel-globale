package com.materiel.suite.backend.v1.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="resource")
public class ResourceEntity {
  @Id
  private UUID id;
  @Column(nullable=false)
  private String name;
  private String type;  // libre : "Grue", "Nacelle", etc.
  private String color; // hex optionnel
  @Column(length=2000)
  private String notes;
  @Column(precision = 13, scale = 2)
  private BigDecimal unitPriceHt;
  private Integer capacity; // nb unités (1 par défaut)
  @Column(length=1000)
  private String tags; // CSV simple "grue, lourde, 80t"
  @Column(length=4000)
  private String weeklyUnavailability; // ex: "MON 08:00-12:00; TUE 13:00-17:00"
  @JsonIgnore
  @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InterventionEntity> interventions;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public String getName(){ return name; }
  public void setName(String name){ this.name=name; }
  public String getType(){ return type; }
  public void setType(String type){ this.type=type; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
  public String getNotes(){ return notes; }
  public void setNotes(String notes){ this.notes=notes; }
  public BigDecimal getUnitPriceHt(){ return unitPriceHt; }
  public void setUnitPriceHt(BigDecimal unitPriceHt){ this.unitPriceHt = unitPriceHt; }
  public Integer getCapacity(){ return capacity; }
  public void setCapacity(Integer c){ this.capacity=c; }
  public String getTags(){ return tags; }
  public void setTags(String t){ this.tags=t; }
  public String getWeeklyUnavailability(){ return weeklyUnavailability; }
  public void setWeeklyUnavailability(String w){ this.weeklyUnavailability=w; }
}
