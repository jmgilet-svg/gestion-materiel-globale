package com.materiel.suite.backend.planning;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlanningService {
  List<ResourceDto> listResources();
  ResourceDto saveResource(ResourceDto r);
  void deleteResource(UUID id);

  List<InterventionDto> listInterventions(LocalDate from, LocalDate to);
  InterventionDto saveIntervention(InterventionDto i);
  void deleteIntervention(UUID id);
}

