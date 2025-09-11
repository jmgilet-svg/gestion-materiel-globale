package com.materiel.suite.client.model;

import java.time.LocalDate;
import java.util.UUID;

public class Intervention {
  private UUID id;
  private UUID resourceId;
  private String label;
  private LocalDate dateDebut;
  private LocalDate dateFin;
  private String color;

  public Intervention(){}
  public Intervention(UUID id, UUID resourceId, String label, LocalDate start, LocalDate end, String color){
    this.id=id; this.resourceId=resourceId; this.label=label; this.dateDebut=start; this.dateFin=end; this.color=color;
  }
  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public UUID getResourceId(){ return resourceId; }
  public void setResourceId(UUID resourceId){ this.resourceId=resourceId; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label=label; }
  public LocalDate getDateDebut(){ return dateDebut; }
  public void setDateDebut(LocalDate dateDebut){ this.dateDebut=dateDebut; }
  public LocalDate getDateFin(){ return dateFin; }
  public void setDateFin(LocalDate dateFin){ this.dateFin=dateFin; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
}
