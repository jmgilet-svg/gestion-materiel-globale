package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.Conflict;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

public interface PlanningService {
  List<Resource> listResources();
  Resource saveResource(Resource r);
  void deleteResource(UUID id);

  List<Intervention> listInterventions(LocalDate from, LocalDate to);
  Intervention saveIntervention(Intervention i);
  void deleteIntervention(UUID id);

  List<Conflict> listConflicts(LocalDate from, LocalDate to);
  // Sprint 2 — auto-résolutions
  boolean resolveShift(UUID id, int minutes);
  boolean resolveReassign(UUID id, UUID resourceId);
  boolean resolveSplit(UUID id, LocalDateTime splitAt);

}
