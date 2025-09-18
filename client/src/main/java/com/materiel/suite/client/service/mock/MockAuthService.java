package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.auth.Role;
import com.materiel.suite.client.auth.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Jeu de données local pour le mode mock. */
public class MockAuthService implements AuthService {
  private final List<Agency> agencies = List.of(
      agency("A1", "Agence Lyon"),
      agency("A2", "Agence Paris")
  );
  private final Map<String, User> users = new HashMap<>();

  public MockAuthService(){
    users.put("admin", user("1", "admin", "Administrateur", Role.ADMIN, "A1"));
    users.put("sales", user("2", "sales", "Commercial", Role.SALES, "A1"));
    users.put("config", user("3", "config", "Configurateur", Role.CONFIG, "A2"));
  }

  @Override
  public List<Agency> listAgencies(){
    return new ArrayList<>(agencies);
  }

  @Override
  public User login(String agencyId, String username, String password){
    User base = users.get(username);
    if (base == null || password == null || !Objects.equals(password.trim(), username)){ // simple mot de passe = identifiant
      throw new RuntimeException("Identifiants invalides");
    }
    Agency selected = agencies.stream()
        .filter(agency -> Objects.equals(agency.getId(), agencyId))
        .findFirst()
        .orElse(agencies.get(0));
    User copy = clone(base);
    copy.setAgency(clone(selected));
    AuthContext.set(copy);
    return copy;
  }

  @Override
  public User current(){
    return AuthContext.get();
  }

  @Override
  public void logout(){
    AuthContext.clear();
  }

  private Agency agency(String id, String name){
    Agency agency = new Agency();
    agency.setId(id);
    agency.setName(name);
    return agency;
  }

  private Agency clone(Agency source){
    if (source == null){
      return null;
    }
    Agency copy = new Agency();
    copy.setId(source.getId());
    copy.setName(source.getName());
    return copy;
  }

  private User user(String id, String username, String displayName, Role role, String agencyId){
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setRole(role);
    user.setAgency(agency(agencyId, ""));
    return user;
  }

  private User clone(User source){
    if (source == null){
      return null;
    }
    User copy = new User();
    copy.setId(source.getId());
    copy.setUsername(source.getUsername());
    copy.setDisplayName(source.getDisplayName());
    copy.setRole(source.getRole());
    copy.setAgency(clone(source.getAgency()));
    return copy;
  }
}
