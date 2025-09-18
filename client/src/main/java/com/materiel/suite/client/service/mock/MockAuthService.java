package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.auth.User;
import com.materiel.suite.client.users.UserAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Jeu de données local pour le mode mock. */
public class MockAuthService implements AuthService {
  private final List<Agency> agencies = List.of(
      agency("A1", "Agence Lyon"),
      agency("A2", "Agence Paris")
  );
  private final MockUserService users;

  public MockAuthService(){
    this(new MockUserService());
  }

  public MockAuthService(MockUserService users){
    this.users = users;
  }

  @Override
  public List<Agency> listAgencies(){
    return new ArrayList<>(agencies);
  }

  @Override
  public User login(String agencyId, String username, String password){
    UserAccount account = users.findByUsername(username);
    if (account == null || password == null || password.isBlank()){ // simple validation basique
      throw new RuntimeException("Identifiants invalides");
    }
    if (!users.matchesPassword(account.getId(), password.trim())){
      throw new RuntimeException("Identifiants invalides");
    }
    Agency selected = agencies.stream()
        .filter(agency -> Objects.equals(agency.getId(), agencyId))
        .findFirst()
        .orElse(agencies.get(0));
    User copy = toUser(account);
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

  private User toUser(UserAccount account){
    if (account == null){
      return null;
    }
    User user = new User();
    user.setId(account.getId());
    user.setUsername(account.getUsername());
    user.setDisplayName(account.getDisplayName());
    user.setRole(account.getRole());
    if (account.getAgency() != null){
      user.setAgency(clone(account.getAgency()));
    }
    return user;
  }
}
