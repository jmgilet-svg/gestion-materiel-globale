package com.materiel.suite.client.service;

import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;

import java.util.List;
import java.util.UUID;

/**
 * Simple static accessors to the underlying services while keeping UI code decoupled
 * from {@link ServiceFactory} internals.
 */
public final class ServiceLocator {
  private static final ResourcesGateway RESOURCES = new ResourcesGateway();
  private static final InterventionTypesGateway INTERVENTION_TYPES = new InterventionTypesGateway();

  private ServiceLocator(){
  }

  public static ResourcesGateway resources(){
    return RESOURCES;
  }

  public static InterventionTypesGateway interventionTypes(){
    return INTERVENTION_TYPES;
  }

  public static AuthService auth(){
    return ServiceFactory.auth();
  }

  public static final class ResourcesGateway {
    public List<Resource> listAll(){
      PlanningService svc = ServiceFactory.planning();
      return svc != null ? svc.listResources() : List.of();
    }

    public Resource save(Resource resource){
      if (resource == null){
        return null;
      }
      PlanningService svc = ServiceFactory.planning();
      return svc != null ? svc.saveResource(resource) : resource;
    }

    public void delete(UUID id){
      PlanningService svc = ServiceFactory.planning();
      if (svc != null && id != null){
        svc.deleteResource(id);
      }
    }

    public boolean isAvailable(){
      return ServiceFactory.planning() != null;
    }
  }

  public static final class InterventionTypesGateway {
    public List<InterventionType> list(){
      InterventionTypeService svc = ServiceFactory.interventionTypes();
      return svc != null ? svc.list() : List.of();
    }

    public InterventionType save(InterventionType type){
      if (type == null){
        return null;
      }
      InterventionTypeService svc = ServiceFactory.interventionTypes();
      return svc != null ? svc.save(type) : type;
    }

    public void delete(String code){
      if (code == null || code.isBlank()){
        return;
      }
      InterventionTypeService svc = ServiceFactory.interventionTypes();
      if (svc != null){
        svc.delete(code);
      }
    }
  }
}
