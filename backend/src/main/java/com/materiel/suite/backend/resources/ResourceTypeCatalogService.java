package com.materiel.suite.backend.resources;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResourceTypeCatalogService {
  private final Map<String, ResourceTypeV2Dto> store = new ConcurrentHashMap<>();

  @PostConstruct
  public void seed(){
    if (!store.isEmpty()){
      return;
    }
    put("CRANE", "Grue", "crane", new BigDecimal("120.00"));
    put("TRUCK", "Camion", "truck", new BigDecimal("85.00"));
    put("FORKLIFT", "Chariot", "forklift", new BigDecimal("55.00"));
    put("CONTAINER", "Nacelle", "container", new BigDecimal("95.00"));
  }

  private void put(String id, String name, String icon, BigDecimal price){
    ResourceTypeV2Dto dto = new ResourceTypeV2Dto();
    dto.setId(id);
    dto.setName(name);
    dto.setIconKey(icon);
    dto.setUnitPriceHt(price);
    store.put(id, dto);
  }

  public List<ResourceTypeV2Dto> list(){
    List<ResourceTypeV2Dto> list = new ArrayList<>();
    for (ResourceTypeV2Dto dto : store.values()){
      list.add(copy(dto));
    }
    return list;
  }

  public Optional<ResourceTypeV2Dto> get(String id){
    if (id == null || id.isBlank()){
      return Optional.empty();
    }
    return Optional.ofNullable(store.get(id)).map(this::copy);
  }

  public ResourceTypeV2Dto create(ResourceTypeV2Dto dto){
    if (dto == null){
      return null;
    }
    ResourceTypeV2Dto copy = copy(dto);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    store.put(copy.getId(), copy);
    return copy(copy);
  }

  public ResourceTypeV2Dto update(String id, ResourceTypeV2Dto dto){
    if (id == null || id.isBlank()){
      throw new IllegalArgumentException("id manquant");
    }
    ResourceTypeV2Dto copy = copy(dto);
    copy.setId(id);
    store.put(id, copy);
    return copy(copy);
  }

  public void delete(String id){
    if (id == null || id.isBlank()){
      return;
    }
    store.remove(id);
  }

  private ResourceTypeV2Dto copy(ResourceTypeV2Dto dto){
    if (dto == null){
      return new ResourceTypeV2Dto();
    }
    ResourceTypeV2Dto copy = new ResourceTypeV2Dto();
    copy.setId(dto.getId());
    copy.setName(dto.getName());
    copy.setIconKey(dto.getIconKey());
    copy.setUnitPriceHt(dto.getUnitPriceHt());
    return copy;
  }
}
