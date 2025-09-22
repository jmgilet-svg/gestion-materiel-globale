package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.service.DocumentTemplateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stockage en mémoire de templates HTML avec valeurs par défaut. */
public class MockDocumentTemplateService implements DocumentTemplateService {
  private final Map<String, Template> store = new ConcurrentHashMap<>();

  public MockDocumentTemplateService(){
    saveInternal(defaultTemplate("QUOTE", "default", "Modèle devis"));
    saveInternal(defaultTemplate("INVOICE", "default", "Modèle facture"));
  }

  @Override
  public List<Template> list(String type){
    List<Template> result = new ArrayList<>();
    for (Template t : store.values()){
      if (type == null || type.equalsIgnoreCase(t.getType())){
        result.add(copy(t));
      }
    }
    return result;
  }

  @Override
  public Template save(Template template){
    Template copy = template == null ? new Template() : copy(template);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    saveInternal(copy);
    return copy(copy);
  }

  @Override
  public void delete(String id){
    if (id != null){
      store.remove(id);
    }
  }

  private void saveInternal(Template template){
    store.put(template.getId(), copy(template));
  }

  private Template copy(Template src){
    Template t = new Template();
    t.setId(src.getId());
    t.setAgencyId(src.getAgencyId());
    t.setType(src.getType());
    t.setKey(src.getKey());
    t.setName(src.getName());
    t.setContent(src.getContent());
    return t;
  }

  private Template defaultTemplate(String type, String key, String name){
    Template t = new Template();
    t.setId(type.toLowerCase() + "-" + key);
    t.setType(type);
    t.setKey(key);
    t.setName(name);
    t.setContent("<html><body><h1>" + name + "</h1><p>{{agency.name}}</p></body></html>");
    return t;
  }
}
