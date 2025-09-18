package com.materiel.suite.backend.auth;

import com.materiel.suite.backend.auth.dto.AgencyV2Dto;
import com.materiel.suite.backend.auth.dto.LoginV2Request;
import com.materiel.suite.backend.auth.dto.UserCreateRequest;
import com.materiel.suite.backend.auth.dto.UserV2Dto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthCatalogService {
  private final Map<String, UserV2Dto> users = new ConcurrentHashMap<>();
  private final Map<String, AgencyV2Dto> agencies = new ConcurrentHashMap<>();
  private final Map<String, String> passwords = new ConcurrentHashMap<>();

  @PostConstruct
  public void seed(){
    if (agencies.isEmpty()){
      agencies.put("A1", agency("A1", "Agence Lyon"));
      agencies.put("A2", agency("A2", "Agence Paris"));
    }
    if (users.isEmpty()){
      UserV2Dto admin = user("1", "admin", "Administrateur", "ADMIN", "A1");
      UserV2Dto sales = user("2", "sales", "Commercial", "SALES", "A1");
      UserV2Dto config = user("3", "config", "Configurateur", "CONFIG", "A2");
      users.put(admin.getUsername(), admin); passwords.put(admin.getId(), "admin");
      users.put(sales.getUsername(), sales); passwords.put(sales.getId(), "sales");
      users.put(config.getUsername(), config); passwords.put(config.getId(), "config");
    }
  }

  public List<AgencyV2Dto> listAgencies(){
    return agencies.values().stream()
        .map(this::copy)
        .sorted(Comparator.comparing(AgencyV2Dto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
        .toList();
  }

  public List<UserV2Dto> listUsers(){
    return users.values().stream()
        .map(this::copy)
        .sorted(Comparator.comparing(UserV2Dto::getUsername, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
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
    String storedPassword = passwords.get(base.getId());
    if (password == null || storedPassword == null || !Objects.equals(password.trim(), storedPassword)){
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
    copy.setToken("dev-" + UUID.randomUUID());
    return Optional.of(copy);
  }

  public UserV2Dto createUser(UserCreateRequest request){
    if (request == null || request.getUser() == null){
      throw new IllegalArgumentException("Utilisateur requis");
    }
    UserV2Dto incoming = copy(request.getUser());
    if (incoming.getUsername() == null || incoming.getUsername().isBlank()){
      throw new IllegalArgumentException("Identifiant requis");
    }
    if (incoming.getId() == null || incoming.getId().isBlank()){
      incoming.setId(UUID.randomUUID().toString());
    }
    normalizeAgency(incoming);
    users.entrySet().removeIf(entry -> Objects.equals(entry.getValue().getId(), incoming.getId()));
    users.put(incoming.getUsername(), incoming);
    String pwd = request.getPassword();
    passwords.put(incoming.getId(), pwd == null || pwd.isBlank() ? "changeme" : pwd);
    return copy(incoming);
  }

  public UserV2Dto updateUser(String id, UserV2Dto body){
    if (id == null || body == null){
      throw new IllegalArgumentException("Paramètres requis");
    }
    UserV2Dto incoming = copy(body);
    if (incoming.getUsername() == null || incoming.getUsername().isBlank()){
      throw new IllegalArgumentException("Identifiant requis");
    }
    incoming.setId(id);
    normalizeAgency(incoming);
    users.entrySet().removeIf(entry -> Objects.equals(entry.getValue().getId(), id));
    users.put(incoming.getUsername(), incoming);
    return copy(incoming);
  }

  public void deleteUser(String id){
    if (id == null){
      return;
    }
    users.entrySet().removeIf(entry -> Objects.equals(entry.getValue().getId(), id));
    passwords.remove(id);
  }

  public void updatePassword(String id, String newPassword){
    if (id == null){
      return;
    }
    passwords.put(id, newPassword == null || newPassword.isBlank() ? "changeme" : newPassword);
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
    copy.setToken(source.getToken());
    return copy;
  }

  private void normalizeAgency(UserV2Dto user){
    if (user == null || user.getAgency() == null || user.getAgency().getId() == null){
      return;
    }
    AgencyV2Dto known = agencies.get(user.getAgency().getId());
    if (known != null){
      user.setAgency(copy(known));
    }
  }
}
