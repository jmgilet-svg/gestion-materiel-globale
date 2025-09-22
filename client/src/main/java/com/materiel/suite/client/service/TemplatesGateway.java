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
