package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlanningService {
  List<Resource> listResources();
  Resource saveResource(Resource r);
  void deleteResource(UUID id);

  List<Intervention> listInterventions(LocalDate from, LocalDate to);
  Intervention saveIntervention(Intervention i);
  void deleteIntervention(UUID id);
}
