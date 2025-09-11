package com.materiel.suite.backend.model;

import java.util.UUID;

public class Conflict {
  private UUID a;
  private UUID b;
  private UUID resourceId;
  public Conflict(){}
  public Conflict(UUID a, UUID b, UUID r){ this.a=a; this.b=b; this.resourceId=r; }
  public UUID getA(){ return a; }
  public UUID getB(){ return b; }
  public UUID getResourceId(){ return resourceId; }
  public void setA(UUID v){ this.a=v; }
  public void setB(UUID v){ this.b=v; }
  public void setResourceId(UUID v){ this.resourceId=v; }
}
