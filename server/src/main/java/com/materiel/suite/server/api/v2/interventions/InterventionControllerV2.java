package com.materiel.suite.server.api.v2.interventions;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v2/interventions")
public class InterventionControllerV2 {
  public static class InterventionV2 {
    public String id;
    public String title;
    public String clientId;
    public String type;         // ex. Livraison, Levage…
    public String status;       // ex. PLANIFIEE / TERMINEE
    public Date plannedStart;
    public Date plannedEnd;
    public Date actualStart;
    public Date actualEnd;
    public List<String> resourceIds = new ArrayList<>();
    public boolean quoteGenerated;
  }

  private static final Map<String, Map<String, InterventionV2>> STORE = new ConcurrentHashMap<>();

  private static String keyOf(String agency){
    return (agency == null || agency.isBlank()) ? "_default" : agency;
  }

  private static Map<String, InterventionV2> bucket(String agency){
    return STORE.computeIfAbsent(keyOf(agency), k -> new ConcurrentHashMap<>());
  }

  public static Map<String, InterventionV2> _bucket(String agency){
    return bucket(agency);
  }

  @GetMapping
  public List<InterventionV2> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    return new ArrayList<>(bucket(agencyId).values());
  }

  @PostMapping
  public InterventionV2 upsert(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                               @RequestBody InterventionV2 it){
    if (it == null){
      return null;
    }
    if (it.id == null){
      it.id = UUID.randomUUID().toString();
    }
    bucket(agencyId).put(it.id, it);
    return it;
  }

  @GetMapping("/{id}")
  public InterventionV2 get(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                            @PathVariable String id){
    return bucket(agencyId).get(id);
  }

  @DeleteMapping("/{id}")
  public void delete(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                     @PathVariable String id){
    bucket(agencyId).remove(id);
  }
}
