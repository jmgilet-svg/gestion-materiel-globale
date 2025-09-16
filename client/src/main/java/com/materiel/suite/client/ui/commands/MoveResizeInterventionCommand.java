package com.materiel.suite.client.ui.commands;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.net.ServiceFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

public class MoveResizeInterventionCommand implements Command {
  private final UUID id;
  private final UUID oldRes, newRes;
  private final LocalDateTime oldStart, oldEnd, newStart, newEnd;
  private final List<ResourceRef> beforeResources;
  private final List<ResourceRef> afterResources;

  public MoveResizeInterventionCommand(Intervention target,
                                       UUID newRes,
                                       LocalDateTime newStart,
                                       LocalDateTime newEnd){
    this.id = target.getId();
    this.oldRes = target.getResourceId();
    this.oldStart = target.getDateHeureDebut();
    this.oldEnd = target.getDateHeureFin();
    this.newRes = newRes;
    this.newStart = newStart;
    this.newEnd = newEnd;
    this.beforeResources = cloneResources(target.getResources());
    this.afterResources = adjustResources(beforeResources, newRes);
  }

  @Override public void execute(){
    var it = skeleton(newRes, newStart, newEnd, afterResources);
    ServiceFactory.planning().saveIntervention(it);
  }

  @Override public void undo(){
    var it = skeleton(oldRes, oldStart, oldEnd, beforeResources);
    ServiceFactory.planning().saveIntervention(it);
  }

  private Intervention skeleton(UUID resId, LocalDateTime start, LocalDateTime end, List<ResourceRef> resources){
    var it = new Intervention();
    it.setId(id);
    it.setResources(resources);
    it.setResourceId(resId);
    it.setDateHeureDebut(start);
    it.setDateHeureFin(end);
    return it;
  }

  private List<ResourceRef> cloneResources(List<ResourceRef> list){
    if (list==null || list.isEmpty()) return List.of();
    return list.stream()
        .map(r -> new ResourceRef(r.getId(), r.getName(), r.getIcon()))
        .collect(Collectors.toList());
  }

  private List<ResourceRef> adjustResources(List<ResourceRef> base, UUID resourceId){
    List<ResourceRef> copy = cloneResources(base);
    if (resourceId==null){
      return copy;
    }
    if (copy.isEmpty()){
      return List.of(new ResourceRef(resourceId, null, null));
    }
    ResourceRef first = copy.get(0);
    copy.set(0, new ResourceRef(resourceId, first.getName(), first.getIcon()));
    return copy;
  }
}
