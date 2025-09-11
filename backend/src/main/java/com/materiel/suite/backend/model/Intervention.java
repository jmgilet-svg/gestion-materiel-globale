package com.materiel.suite.backend.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Intervention {
  private UUID id;
  private UUID resourceId;
  private String label;
  private LocalDateTime dateHeureDebut;
  private LocalDateTime dateHeureFin;
  private String color;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public UUID getResourceId(){ return resourceId; }
  public void setResourceId(UUID resourceId){ this.resourceId=resourceId; }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label=label; }
  public LocalDateTime getDateHeureDebut(){ return dateHeureDebut; }
  public void setDateHeureDebut(LocalDateTime v){ this.dateHeureDebut=v; }
  public LocalDateTime getDateHeureFin(){ return dateHeureFin; }
  public void setDateHeureFin(LocalDateTime v){ this.dateHeureFin=v; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
}
