package com.materiel.suite.server.api.v2;

import com.materiel.suite.client.model.Client;
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
@RequestMapping("/api/v2/clients")
public class ClientControllerV2 {
  private static final Map<String, Map<String, Client>> STORE = new ConcurrentHashMap<>();

  private static String keyOf(String agency){
    return (agency == null || agency.isBlank()) ? "_default" : agency;
  }

  private static Map<String, Client> bucket(String agency){
    return STORE.computeIfAbsent(keyOf(agency), k -> new ConcurrentHashMap<>());
  }

  static Map<String, Client> _bucket(String agency){
    return bucket(agency);
  }

  @GetMapping
  public List<Client> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    return new ArrayList<>(bucket(agencyId).values());
  }

  @GetMapping("/{id}")
  public Client get(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                    @PathVariable String id){
    return bucket(agencyId).get(id);
  }

  @PostMapping
  public Client save(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                     @RequestBody Client client){
    if (client == null){
      return null;
    }
    if (client.getId() == null){
      client.setId(UUID.randomUUID());
    }
    bucket(agencyId).put(client.getId().toString(), client);
    return client;
  }

  @DeleteMapping("/{id}")
  public void delete(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                     @PathVariable String id){
    bucket(agencyId).remove(id);
  }
}
