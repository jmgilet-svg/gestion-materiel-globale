package com.materiel.suite.client.model;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.UUID;

public class Intervention {
  private UUID id;
  private UUID resourceId;
  private String label;
  private LocalDateTime dateHeureDebut;
  private LocalDateTime dateHeureFin;
  private String color; // hex

  public Intervention(){}
  public Intervention(UUID id, UUID resourceId, String label, LocalDate start, LocalDate end, String color){
    this(id, resourceId, label, start.atStartOfDay(), end.atTime(LocalTime.of(18,0)), color);
  }
  public Intervention(UUID id, UUID resourceId, String label, LocalDateTime start, LocalDateTime end, String color){
    this.id=id; this.resourceId=resourceId; this.label=label; this.dateHeureDebut=start; this.dateHeureFin=end; this.color=color;
  }

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id = id; }
  public UUID getResourceId(){ return resourceId; }
  public void setResourceId(UUID resourceId){ this.resourceId = resourceId; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label = label; }

  // API horaire
  public LocalDateTime getDateHeureDebut(){ return dateHeureDebut; }
  public void setDateHeureDebut(LocalDateTime v){ this.dateHeureDebut = v; }
  public LocalDateTime getDateHeureFin(){ return dateHeureFin; }
  public void setDateHeureFin(LocalDateTime v){ this.dateHeureFin = v; }

  // Compat legacy (jour)
  public LocalDate getDateDebut(){ return dateHeureDebut==null? null : dateHeureDebut.toLocalDate(); }
  public void setDateDebut(LocalDate d){ if (d!=null){ this.dateHeureDebut = d.atTime(this.dateHeureDebut!=null? this.dateHeureDebut.toLocalTime() : LocalTime.of(8,0)); } }
  public LocalDate getDateFin(){ return dateHeureFin==null? null : dateHeureFin.toLocalDate(); }
  public void setDateFin(LocalDate d){ if (d!=null){ this.dateHeureFin = d.atTime(this.dateHeureFin!=null? this.dateHeureFin.toLocalTime() : LocalTime.of(17,0)); } }

  public String getColor(){ return color; }
  public void setColor(String color){ this.color = color; }
}
