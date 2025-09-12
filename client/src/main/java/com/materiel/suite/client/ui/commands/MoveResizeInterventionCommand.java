package com.materiel.suite.client.ui.commands;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.net.ServiceFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public class MoveResizeInterventionCommand implements Command {
  private final UUID id;
  private final UUID oldRes, newRes;
  private final LocalDateTime oldStart, oldEnd, newStart, newEnd;

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
  }

  @Override public void execute(){
    var it = find();
    it.setResourceId(newRes);
    it.setDateHeureDebut(newStart);
    it.setDateHeureFin(newEnd);
    ServiceFactory.planning().saveIntervention(it);
  }

  @Override public void undo(){
    var it = find();
    it.setResourceId(oldRes);
    it.setDateHeureDebut(oldStart);
    it.setDateHeureFin(oldEnd);
    ServiceFactory.planning().saveIntervention(it);
  }

  private Intervention find(){
    // sans repo local, on reconstitue un squelette mutable
    var it = new Intervention();
    it.setId(id);
    it.setResourceId(newRes);
    it.setDateHeureDebut(newStart);
    it.setDateHeureFin(newEnd);
    return it;
  }
}
