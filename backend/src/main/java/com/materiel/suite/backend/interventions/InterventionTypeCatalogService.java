package com.materiel.suite.backend.interventions;

import com.materiel.suite.backend.interventions.dto.InterventionTypeV2Dto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      put("levage", "Levage", "crane", 0);
      put("transport", "Transport", "truck", 1);
      put("manutention", "Manutention", "forklift", 2);
    }
  }

  private void put(String id, String name, String icon, Integer order){
    InterventionTypeV2Dto dto = new InterventionTypeV2Dto();
    dto.setId(id);
    dto.setName(name);
    dto.setIconKey(icon);
    dto.setOrderIndex(order);
    store.put(id, dto);
  }

  public List<InterventionTypeV2Dto> list(){
    return store.values().stream()
        .map(this::copy)
        .sorted(Comparator
            .comparing((InterventionTypeV2Dto dto) -> dto.getOrderIndex() == null ? Integer.MAX_VALUE : dto.getOrderIndex())
            .thenComparing(dto -> dto.getName() == null ? "" : dto.getName(), String.CASE_INSENSITIVE_ORDER))
        .toList();
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
    InterventionTypeV2Dto copy = copy(dto);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    if (copy.getOrderIndex() == null){
      copy.setOrderIndex(nextOrderIndex());
    }
    store.put(copy.getId(), copy(copy));
    return store.get(copy.getId());
  }

  public InterventionTypeV2Dto update(String id, InterventionTypeV2Dto dto){
    if (id == null || id.isBlank()){
      throw new IllegalArgumentException("id manquant");
    }
    InterventionTypeV2Dto copy = copy(dto);
    copy.setId(id);
    if (copy.getOrderIndex() == null){
      InterventionTypeV2Dto existing = store.get(id);
      if (existing != null && existing.getOrderIndex() != null){
        copy.setOrderIndex(existing.getOrderIndex());
      } else {
        copy.setOrderIndex(nextOrderIndex());
      }
    }
    store.put(id, copy(copy));
    return store.get(id);
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
    copy.setOrderIndex(dto.getOrderIndex());
    return copy;
  }

  private int nextOrderIndex(){
    return store.values().stream()
        .map(InterventionTypeV2Dto::getOrderIndex)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(-1) + 1;
  }
}
