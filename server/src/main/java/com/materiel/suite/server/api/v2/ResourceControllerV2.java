package com.materiel.suite.server.api.v2;

import com.materiel.suite.client.model.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v2/resources")
public class ResourceControllerV2 {
  private static final Map<String, Map<String, Resource>> STORE = new ConcurrentHashMap<>();

  private static String keyOf(String agency){
    return (agency == null || agency.isBlank()) ? "_default" : agency;
  }

  private static Map<String, Resource> bucket(String agency){
    return STORE.computeIfAbsent(keyOf(agency), k -> new ConcurrentHashMap<>());
  }

  /** Expose un accès package-private pour les seeders. */
  static Map<String, Resource> _bucket(String agency){
    return bucket(agency);
  }

  @GetMapping
  public List<Resource> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    return new ArrayList<>(bucket(agencyId).values());
  }

  @PostMapping
  public Resource save(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                       @RequestBody Resource resource){
    if (resource == null){
      return null;
    }
    if (resource.getId() == null){
      resource.setId(UUID.randomUUID());
    }
    bucket(agencyId).put(resource.getId().toString(), resource);
    return resource;
  }

  @GetMapping("/{id}")
  public Resource get(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                      @PathVariable String id){
    return bucket(agencyId).get(id);
  }

  @DeleteMapping("/{id}")
  public void delete(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                     @PathVariable String id){
    bucket(agencyId).remove(id);
  }
}
