package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.service.InterventionTypeService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Fournit un catalogue statique pour l'utilisation hors-ligne. */
public class MockInterventionTypeService implements InterventionTypeService {
  private final Map<String, InterventionType> store = new ConcurrentHashMap<>();

  public MockInterventionTypeService(){
    seedDefaults();
  }

  @Override
  public List<InterventionType> list(){
    return store.values().stream()
        .map(this::copy)
        .sorted(Comparator.comparing(type -> type.getLabel() == null ? "" : type.getLabel(), String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Override
  public InterventionType save(InterventionType type){
    if (type == null){
      return null;
    }
    InterventionType copy = copy(type);
    if (copy.getCode() == null || copy.getCode().isBlank()){
      copy.setCode(generateCode(copy.getLabel()));
    }
    store.put(copy.getCode(), copy(copy));
    return copy(copy);
  }

  @Override
  public void delete(String code){
    if (code == null || code.isBlank()){
      return;
    }
    store.remove(code);
  }

  private void seedDefaults(){
    if (!store.isEmpty()){
      return;
    }
    put(new InterventionType("LIFT", "Levage", "crane"));
    put(new InterventionType("TRANSPORT", "Transport", "truck"));
    put(new InterventionType("MANUT", "Manutention", "forklift"));
  }

  private void put(InterventionType type){
    if (type == null || type.getCode() == null){
      return;
    }
    store.put(type.getCode(), copy(type));
  }

  private InterventionType copy(InterventionType type){
    if (type == null){
      return null;
    }
    InterventionType copy = new InterventionType();
    copy.setCode(type.getCode());
    copy.setLabel(type.getLabel());
    copy.setIconKey(type.getIconKey());
    return copy;
  }

  private String generateCode(String label){
    String base = label == null ? "TYPE" : label.trim();
    if (base.isBlank()){
      base = "TYPE";
    }
    base = base.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT);
    if (base.isBlank()){
      base = "TYPE";
    }
    String code = base;
    int i = 1;
    while (store.containsKey(code)){
      code = base + "_" + i++;
    }
    return code;
  }
}
