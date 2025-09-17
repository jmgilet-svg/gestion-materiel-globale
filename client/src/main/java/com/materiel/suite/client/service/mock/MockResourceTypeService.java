package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.service.ResourceTypeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MockResourceTypeService implements ResourceTypeService {
  private final Map<String, ResourceType> store = new ConcurrentHashMap<>();

  public MockResourceTypeService(){
    seed();
  }

  @Override
  public List<ResourceType> listAll(){
    List<ResourceType> list = new ArrayList<>();
    for (ResourceType type : store.values()){
      list.add(copyOf(type));
    }
    return list;
  }

  @Override
  public ResourceType save(ResourceType type){
    if (type == null){
      return null;
    }
    ResourceType copy = copyOf(type);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    store.put(copy.getId(), copy);
    return copyOf(copy);
  }

  @Override
  public void delete(String id){
    if (id != null){
      store.remove(id);
    }
  }

  private void seed(){
    if (!store.isEmpty()){
      return;
    }
    put(create("CRANE", "Grue", "crane"));
    put(create("TRUCK", "Camion", "truck"));
    put(create("FORKLIFT", "Chariot", "forklift"));
    put(create("CONTAINER", "Nacelle", "container"));
  }

  private void put(ResourceType type){
    store.put(type.getId(), copyOf(type));
  }

  private ResourceType create(String id, String name, String icon){
    ResourceType type = new ResourceType();
    type.setId(id);
    type.setName(name);
    type.setIconKey(icon);
    return type;
  }

  private ResourceType copyOf(ResourceType src){
    if (src == null){
      return null;
    }
    ResourceType copy = new ResourceType();
    copy.setId(src.getId());
    copy.setName(src.getName());
    copy.setIconKey(src.getIconKey());
    return copy;
  }
}
