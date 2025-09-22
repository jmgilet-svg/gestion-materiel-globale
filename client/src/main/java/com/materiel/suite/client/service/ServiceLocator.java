package com.materiel.suite.client.service;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.DocumentTemplateService;
import com.materiel.suite.client.service.MailService;
import com.materiel.suite.client.service.PdfService;
import com.materiel.suite.client.service.impl.LocalSettingsService;
import com.materiel.suite.client.service.TimelineService;
import com.materiel.suite.client.settings.EmailSettings;
import com.materiel.suite.client.users.UserService;

import java.util.List;
import java.util.UUID;

/**
 * Simple static accessors to the underlying services while keeping UI code decoupled
 * from {@link ServiceFactory} internals.
 */
public final class ServiceLocator {
  private static final ResourcesGateway RESOURCES = new ResourcesGateway();
  private static final InterventionTypesGateway INTERVENTION_TYPES = new InterventionTypesGateway();

  private static SettingsService SETTINGS;

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

  public static UserService users(){
    return ServiceFactory.users();
  }

  public static SalesService sales(){
    return ServiceFactory.sales();
  }

  public static SettingsService settings(){
    if (SETTINGS == null){
      SETTINGS = new LocalSettingsService();
    }
    return SETTINGS;
  }

  public static EmailSettings emailSettings(){
    return settings().getEmail();
  }

  public static void saveEmailSettings(EmailSettings emailSettings){
    settings().saveEmail(emailSettings);
  }

  public static TimelineService timeline(){
    return ServiceFactory.timeline();
  }

  public static MailService mail(){
    return ServiceFactory.mail();
  }

  public static DocumentTemplateService documentTemplates(){
    return ServiceFactory.documentTemplates();
  }

  public static PdfService pdf(){
    return ServiceFactory.pdf();
  }

  /** Identifiant d'agence actuellement sélectionné, peut être {@code null}. */
  public static String agencyId(){
    return AgencyContext.agencyId();
  }

  /** Libellé d'agence actuellement sélectionné, peut être {@code null}. */
  public static String agencyLabel(){
    return AgencyContext.agencyLabel();
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

    public Resource get(UUID id){
      PlanningService svc = ServiceFactory.planning();
      return svc != null ? svc.getResource(id) : null;
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
