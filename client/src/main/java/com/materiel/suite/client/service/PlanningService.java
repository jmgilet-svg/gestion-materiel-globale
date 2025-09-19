package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.service.PlanningValidation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlanningService {
  List<Resource> listResources();
  Resource saveResource(Resource r);
  /**
   * Returns the {@link Resource} identified by the given id when available.
   * The default implementation falls back to {@link #listResources()} to
   * preserve compatibility with existing mock services.
   */
  default Resource getResource(UUID id){
    if (id == null){
      return null;
    }
    for (Resource resource : listResources()){
      if (id.equals(resource.getId())){
        return resource;
      }
    }
    return null;
  }
  void deleteResource(UUID id);
  default List<ResourceType> listResourceTypes(){ return List.of(); }
  default ResourceType createResourceType(ResourceType type){ throw new UnsupportedOperationException(); }
  default ResourceType updateResourceType(ResourceType type){ throw new UnsupportedOperationException(); }
  default void deleteResourceType(String code){}
  default List<Unavailability> listResourceUnavailabilities(UUID resourceId){ return List.of(); }
  default Unavailability addUnavailability(UUID resourceId, Unavailability u){ throw new UnsupportedOperationException(); }
  default void deleteUnavailability(UUID resourceId, UUID unavailabilityId){}

  List<Intervention> listInterventions(LocalDate from, LocalDate to);
  Intervention saveIntervention(Intervention i);
  void deleteIntervention(UUID id);

  List<Conflict> listConflicts(LocalDate from, LocalDate to);

  // Sprint 2 — auto-résolutions
  boolean resolveShift(UUID id, int minutes);
  boolean resolveReassign(UUID id, UUID resourceId);
  boolean resolveSplit(UUID id, LocalDateTime splitAt);

  /**
   * Optional server-side validation before saving an intervention.
   * Default implementation always returns {@link PlanningValidation#ok()}.
   */
  default PlanningValidation validate(Intervention it){
    return PlanningValidation.ok();
  }
}
