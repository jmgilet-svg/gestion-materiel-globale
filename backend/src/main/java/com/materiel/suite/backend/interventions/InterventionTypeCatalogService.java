package com.materiel.suite.backend.interventions;

import com.materiel.suite.backend.interventions.dto.InterventionTypeV2Dto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Catalogue en mémoire pour les types d'intervention exposés en v2. */
@Service
public class InterventionTypeCatalogService {
  private final Map<String, InterventionTypeV2Dto> store = new ConcurrentHashMap<>();

  @PostConstruct
  public void seed(){
    if (store.isEmpty()){
      put("levage", "Levage", "crane");
      put("transport", "Transport", "truck");
      put("manutention", "Manutention", "forklift");
    }
  }

  private void put(String id, String name, String icon){
    InterventionTypeV2Dto dto = new InterventionTypeV2Dto();
    dto.setId(id);
    dto.setName(name);
    dto.setIconKey(icon);
    store.put(id, dto);
  }

  public List<InterventionTypeV2Dto> list(){
    return new ArrayList<>(store.values());
  }

  public Optional<InterventionTypeV2Dto> get(String id){
    if (id == null || id.isBlank()){
      return Optional.empty();
    }
    return Optional.ofNullable(store.get(id));
  }

  public InterventionTypeV2Dto create(InterventionTypeV2Dto dto){
    if (dto == null){
      return null;
    }
    if (dto.getId() == null || dto.getId().isBlank()){
      dto.setId(UUID.randomUUID().toString());
    }
    store.put(dto.getId(), copy(dto));
    return store.get(dto.getId());
  }

  public InterventionTypeV2Dto update(String id, InterventionTypeV2Dto dto){
    if (id == null || id.isBlank()){
      throw new IllegalArgumentException("id manquant");
    }
    InterventionTypeV2Dto copy = copy(dto);
    copy.setId(id);
    store.put(id, copy);
    return copy;
  }

  public void delete(String id){
    if (id == null || id.isBlank()){
      return;
    }
    store.remove(id);
  }

  private InterventionTypeV2Dto copy(InterventionTypeV2Dto dto){
    if (dto == null){
      return new InterventionTypeV2Dto();
    }
    InterventionTypeV2Dto copy = new InterventionTypeV2Dto();
    copy.setId(dto.getId());
    copy.setName(dto.getName());
    copy.setIconKey(dto.getIconKey());
    return copy;
  }
}
