package com.materiel.suite.backend.auth;

import com.materiel.suite.backend.auth.dto.AgencyV2Dto;
import com.materiel.suite.backend.auth.dto.LoginV2Request;
import com.materiel.suite.backend.auth.dto.UserV2Dto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthCatalogService {
  private final Map<String, UserV2Dto> users = new ConcurrentHashMap<>();
  private final Map<String, AgencyV2Dto> agencies = new ConcurrentHashMap<>();

  @PostConstruct
  public void seed(){
    if (agencies.isEmpty()){
      agencies.put("A1", agency("A1", "Agence Lyon"));
      agencies.put("A2", agency("A2", "Agence Paris"));
    }
    if (users.isEmpty()){
      users.put("admin", user("1", "admin", "Administrateur", "ADMIN", "A1"));
      users.put("sales", user("2", "sales", "Commercial", "SALES", "A1"));
      users.put("config", user("3", "config", "Configurateur", "CONFIG", "A2"));
    }
  }

  public List<AgencyV2Dto> listAgencies(){
    return agencies.values().stream()
        .map(this::copy)
        .sorted(Comparator.comparing(AgencyV2Dto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
        .toList();
  }

  public Optional<UserV2Dto> login(LoginV2Request request){
    if (request == null || request.getUsername() == null){
      return Optional.empty();
    }
    UserV2Dto base = users.get(request.getUsername());
    if (base == null){
      return Optional.empty();
    }
    String password = request.getPassword();
    if (password == null || !Objects.equals(password.trim(), request.getUsername())){
      return Optional.empty();
    }
    AgencyV2Dto selected = agencies.get(request.getAgencyId());
    if (selected == null && !agencies.isEmpty()){
      selected = agencies.values().iterator().next();
    }
    if (selected == null){
      return Optional.empty();
    }
    UserV2Dto copy = copy(base);
    copy.setAgency(copy(selected));
    return Optional.of(copy);
  }

  private AgencyV2Dto agency(String id, String name){
    AgencyV2Dto dto = new AgencyV2Dto();
    dto.setId(id);
    dto.setName(name);
    return dto;
  }

  private UserV2Dto user(String id, String username, String displayName, String role, String agencyId){
    UserV2Dto dto = new UserV2Dto();
    dto.setId(id);
    dto.setUsername(username);
    dto.setDisplayName(displayName);
    dto.setRole(role);
    dto.setAgency(agency(agencyId, ""));
    return dto;
  }

  private AgencyV2Dto copy(AgencyV2Dto source){
    if (source == null){
      return null;
    }
    AgencyV2Dto copy = new AgencyV2Dto();
    copy.setId(source.getId());
    copy.setName(source.getName());
    return copy;
  }

  private UserV2Dto copy(UserV2Dto source){
    if (source == null){
      return null;
    }
    UserV2Dto copy = new UserV2Dto();
    copy.setId(source.getId());
    copy.setUsername(source.getUsername());
    copy.setDisplayName(source.getDisplayName());
    copy.setRole(source.getRole());
    copy.setAgency(copy(source.getAgency()));
    return copy;
  }
}
