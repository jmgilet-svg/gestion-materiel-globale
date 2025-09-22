package com.materiel.suite.client.service;

import java.util.ArrayList;
import java.util.List;

/** Accès simplifié aux templates HTML depuis l'interface. */
public final class TemplatesGateway {

  public List<Template> list(String type){
    DocumentTemplateService svc = ServiceLocator.documentTemplates();
    if (svc == null){
      return List.of();
    }
    try {
      List<DocumentTemplateService.Template> templates = svc.list(type);
      if (templates == null || templates.isEmpty()){
        return List.of();
      }
      List<Template> out = new ArrayList<>();
      for (DocumentTemplateService.Template template : templates){
        Template copy = copy(template);
        if (copy != null){
          out.add(copy);
        }
      }
      return out.isEmpty() ? List.of() : List.copyOf(out);
    } catch (Exception ignore){
      return List.of();
    }
  }

  private Template copy(DocumentTemplateService.Template template){
    if (template == null){
      return null;
    }
    return new Template(
        template.getId(),
        template.getAgencyId(),
        template.getType(),
        template.getKey(),
        template.getName(),
        template.getContent()
    );
  }

  public Template save(Template template){
    DocumentTemplateService svc = ServiceLocator.documentTemplates();
    if (svc == null){
      return template;
    }
    try {
      DocumentTemplateService.Template dto = toDocumentTemplate(template);
      DocumentTemplateService.Template saved = svc.save(dto);
      return saved == null ? template : copy(saved);
    } catch (Exception ex){
      throw new RuntimeException(ex);
    }
  }

  public void delete(Template template){
    if (template == null){
      return;
    }
    delete(template.id());
  }

  public void delete(String id){
    if (id == null || id.isBlank()){
      return;
    }
    DocumentTemplateService svc = ServiceLocator.documentTemplates();
    if (svc == null){
      return;
    }
    svc.delete(id);
  }

  private DocumentTemplateService.Template toDocumentTemplate(Template template){
    DocumentTemplateService.Template dto = new DocumentTemplateService.Template();
    if (template == null){
      dto.setAgencyId(ServiceLocator.agencyId());
      return dto;
    }
    dto.setId(template.id());
    String agency = template.agencyId();
    if (agency == null || agency.isBlank()){
      agency = ServiceLocator.agencyId();
    }
    dto.setAgencyId(agency);
    dto.setType(template.type());
    dto.setKey(template.key());
    dto.setName(template.name());
    dto.setContent(template.content());
    return dto;
  }

  public record Template(String id, String agencyId, String type, String key, String name, String content) {
    @Override
    public String toString(){
      if (name != null && !name.isBlank()){
        return name;
      }
      if (key != null && !key.isBlank()){
        return key;
      }
      return id == null ? "" : id;
    }
  }
}
