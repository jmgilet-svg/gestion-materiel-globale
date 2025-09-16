package com.materiel.suite.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Intervention {
  private UUID id;
  private UUID resourceId;
  private final List<ResourceRef> resources = new ArrayList<>();
  private String label;
  private LocalDateTime dateHeureDebut;
  private LocalDateTime dateHeureFin;
  private String color;

  public UUID getId(){ return id; }
  public void setId(UUID id){ this.id=id; }
  public UUID getResourceId(){ return resourceId; }
  public void setResourceId(UUID resourceId){
    this.resourceId = resourceId;
    if (resourceId==null){
      if (!resources.isEmpty()){
        ResourceRef ref = resources.get(0);
        if (ref!=null){
          ref.setId(null);
          if (ref.getName()==null && ref.getIcon()==null){
            resources.remove(0);
          }
        }
      }
    } else {
      if (resources.isEmpty()){
        resources.add(new ResourceRef(resourceId, null, null));
      } else {
        ResourceRef ref = resources.get(0);
        if (ref==null){
          resources.set(0, new ResourceRef(resourceId, null, null));
        } else {
          ref.setId(resourceId);
        }
      }
    }
  }
  public List<ResourceRef> getResources(){ return new ArrayList<>(resources); }
  public void setResources(List<ResourceRef> refs){
    resources.clear();
    if (refs!=null){
      for (ResourceRef ref : refs){
        if (ref==null) continue;
        resources.add(new ResourceRef(ref.getId(), ref.getName(), ref.getIcon()));
      }
    }
    if (resources.isEmpty()){
      resourceId = null;
    } else {
      ResourceRef first = resources.get(0);
      resourceId = first!=null? first.getId() : null;
    }
  }
  public Intervention addResource(ResourceRef ref){
    if (ref!=null) resources.add(new ResourceRef(ref.getId(), ref.getName(), ref.getIcon()));
    if (!resources.isEmpty()){
      ResourceRef first = resources.get(0);
      resourceId = first!=null? first.getId() : null;
    }
    return this;
  }
  public ResourceRef primaryResource(){ return resources.isEmpty()? null : resources.get(0); }
  public String getLabel(){ return label; }
  public void setLabel(String label){ this.label=label; }
  public LocalDateTime getDateHeureDebut(){ return dateHeureDebut; }
  public void setDateHeureDebut(LocalDateTime v){ this.dateHeureDebut=v; }
  public LocalDateTime getDateHeureFin(){ return dateHeureFin; }
  public void setDateHeureFin(LocalDateTime v){ this.dateHeureFin=v; }
  public String getColor(){ return color; }
  public void setColor(String color){ this.color=color; }
}
