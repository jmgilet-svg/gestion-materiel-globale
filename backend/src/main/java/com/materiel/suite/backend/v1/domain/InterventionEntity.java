package com.materiel.suite.backend.v1.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="intervention")
public class InterventionEntity {
  @Id
  private UUID id;
  @ManyToOne(optional=false)
  @JoinColumn(name="resource_id")
  private ResourceEntity resource;
  @Column(nullable=false)
  private String label;
  private String color;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  @Enumerated(EnumType.STRING)
  private InterventionStatus status = InterventionStatus.PLANNED;

  @Transient
  public LocalDate getDateDebut(){
    return startDateTime!=null? startDateTime.toLocalDate() : null;
  }
  @Transient
  public LocalDate getDateFin(){
    return endDateTime!=null? endDateTime.toLocalDate() : getDateDebut();
  }

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public ResourceEntity getResource(){ return resource; }
  public void setResource(ResourceEntity resource){ this.resource=resource; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label=label; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
  public LocalDateTime getStartDateTime(){ return startDateTime; }
  public void setStartDateTime(LocalDateTime s){ this.startDateTime=s; }
  public LocalDateTime getEndDateTime(){ return endDateTime; }
  public void setEndDateTime(LocalDateTime e){ this.endDateTime=e; }
  public InterventionStatus getStatus(){ return status; }
  public void setStatus(InterventionStatus s){ this.status=s; }
}
