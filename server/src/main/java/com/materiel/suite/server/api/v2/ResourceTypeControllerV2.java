package com.materiel.suite.server.api.v2;

import com.materiel.suite.client.model.ResourceType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v2/resource-types")
public class ResourceTypeControllerV2 {
  private static final ResourceType[] DEFAULT_TYPES = {
      prototype("CRANE", "Grue", "crane"),
      prototype("TRUCK", "Camion", "truck"),
      prototype("FORKLIFT", "Manutention", "forklift")
  };
  private static final Map<String, Map<String, ResourceType>> STORE = new ConcurrentHashMap<>();

  private static String keyOf(String agency){
    return agency == null || agency.isBlank() ? "_default" : agency;
  }

  private static Map<String, ResourceType> bucket(String agency){
    return STORE.computeIfAbsent(keyOf(agency), k -> new ConcurrentHashMap<>());
  }

  static Map<String, ResourceType> _bucket(String agency){
    return bucket(agency);
  }

  @GetMapping
  public List<ResourceType> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    Map<String, ResourceType> map = bucket(agencyId);
    if (map.isEmpty()){
      seedDefaults(map);
    }
    List<ResourceType> out = new ArrayList<>();
    for (ResourceType type : map.values()){
      ResourceType copy = copyOf(type);
      if (copy != null){
        out.add(copy);
      }
    }
    out.sort(Comparator.comparing(rt -> normalize(rt.getName())));
    return out;
  }

  @PostMapping
  public ResourceType create(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                             @RequestBody ResourceType type){
    ResourceType saved = saveInternal(agencyId, type, null);
    return copyOf(saved);
  }

  @PutMapping("/{id}")
  public ResourceType update(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                             @PathVariable String id,
                             @RequestBody ResourceType type){
    if (type == null){
      return null;
    }
    ResourceType saved = saveInternal(agencyId, type, id);
    return copyOf(saved);
  }

  @DeleteMapping("/{id}")
  public void delete(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                     @PathVariable String id){
    if (id == null || id.isBlank()){
      return;
    }
    bucket(agencyId).remove(id);
  }

  private ResourceType saveInternal(String agencyId, ResourceType type, String overrideId){
    if (type == null){
      return null;
    }
    ResourceType copy = copyOf(type);
    if (overrideId != null && !overrideId.isBlank()){
      copy.setId(overrideId);
    }
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    if (copy.getName() == null || copy.getName().isBlank()){
      copy.setName(copy.getId());
    }
    if (copy.getIconKey() == null || copy.getIconKey().isBlank()){
      copy.setIconKey("cube");
    }
    Map<String, ResourceType> map = bucket(agencyId);
    map.put(copy.getId(), copyOf(copy));
    return map.get(copy.getId());
  }

  private static void seedDefaults(Map<String, ResourceType> map){
    for (ResourceType prototype : DEFAULT_TYPES){
      if (prototype == null){
        continue;
      }
      map.putIfAbsent(prototype.getId(), copyOf(prototype));
    }
  }

  private static ResourceType prototype(String id, String name, String iconKey){
    ResourceType type = new ResourceType();
    type.setId(id);
    type.setName(name);
    type.setIconKey(iconKey != null && !iconKey.isBlank() ? iconKey : "cube");
    return type;
  }

  private static ResourceType copyOf(ResourceType src){
    if (src == null){
      return null;
    }
    ResourceType copy = new ResourceType();
    copy.setId(src.getId());
    copy.setName(src.getName());
    copy.setIconKey(src.getIconKey());
    return copy;
  }

  private static String normalize(String value){
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }
}
