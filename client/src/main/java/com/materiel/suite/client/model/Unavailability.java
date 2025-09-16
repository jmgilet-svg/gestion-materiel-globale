package com.materiel.suite.client.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Unavailability {
  private UUID id;
  private LocalDateTime start;
  private LocalDateTime end;
  private String reason;

  public Unavailability(){}
  public Unavailability(LocalDateTime start, LocalDateTime end, String reason){
    this(null, start, end, reason);
  }
  public Unavailability(UUID id, LocalDateTime start, LocalDateTime end, String reason){
    this.id = id;
    this.start = start;
    this.end = end;
    this.reason = reason;
  }

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id = id; }
  public LocalDateTime getStart(){ return start; }
  public void setStart(LocalDateTime start){ this.start = start; }
  public LocalDateTime getEnd(){ return end; }
  public void setEnd(LocalDateTime end){ this.end = end; }
  public String getReason(){ return reason; }
  public void setReason(String reason){ this.reason = reason; }

  @Override public boolean equals(Object o){
    if (this == o) return true;
    if (!(o instanceof Unavailability)) return false;
    Unavailability that = (Unavailability) o;
    if (id!=null && that.id!=null) return Objects.equals(id, that.id);
    return Objects.equals(start, that.start) && Objects.equals(end, that.end) && Objects.equals(reason, that.reason);
  }

  @Override public int hashCode(){
    return id!=null? id.hashCode() : Objects.hash(start, end, reason);
  }
}
