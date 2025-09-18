package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.Role;
import com.materiel.suite.client.users.UserAccount;
import com.materiel.suite.client.users.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stockage en mémoire des comptes utilisateurs pour le mode mock. */
public class MockUserService implements UserService {
  private final Map<String, UserAccount> byId = new ConcurrentHashMap<>();
  private final Map<String, String> byUsername = new ConcurrentHashMap<>();
  private final Map<String, String> passwords = new ConcurrentHashMap<>();

  public MockUserService(){
    seed("1", "admin", "Administrateur", Role.ADMIN, "A1", "Agence Lyon", "admin");
    seed("2", "sales", "Commercial", Role.SALES, "A1", "Agence Lyon", "sales");
    seed("3", "config", "Configurateur", Role.CONFIG, "A2", "Agence Paris", "config");
  }

  private void seed(String id, String username, String displayName, Role role, String agencyId, String agencyName, String password){
    UserAccount account = new UserAccount();
    account.setId(id);
    account.setUsername(username);
    account.setDisplayName(displayName);
    account.setRole(role);
    Agency agency = new Agency();
    agency.setId(agencyId);
    agency.setName(agencyName);
    account.setAgency(agency);
    create(account, password);
  }

  @Override
  public synchronized List<UserAccount> list(){
    List<UserAccount> accounts = new ArrayList<>();
    for (UserAccount account : byId.values()){
      accounts.add(clone(account));
    }
    accounts.sort(Comparator.comparing(account -> account.getUsername() == null ? "" : account.getUsername(),
        String.CASE_INSENSITIVE_ORDER));
    return accounts;
  }

  @Override
  public synchronized UserAccount create(UserAccount account, String password){
    if (account == null){
      return null;
    }
    UserAccount copy = clone(account);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    store(copy);
    passwords.put(copy.getId(), password == null || password.isBlank() ? "changeme" : password);
    return clone(copy);
  }

  @Override
  public synchronized UserAccount update(UserAccount account){
    if (account == null || account.getId() == null){
      return account;
    }
    UserAccount copy = clone(account);
    UserAccount previous = byId.get(copy.getId());
    if (previous != null && previous.getUsername() != null){
      byUsername.remove(normalize(previous.getUsername()));
    }
    store(copy);
    return clone(copy);
  }

  @Override
  public synchronized void delete(String id){
    if (id == null){
      return;
    }
    UserAccount removed = byId.remove(id);
    if (removed != null && removed.getUsername() != null){
      byUsername.remove(normalize(removed.getUsername()));
    }
    passwords.remove(id);
  }

  @Override
  public synchronized void updatePassword(String id, String newPassword){
    if (id == null){
      return;
    }
    passwords.put(id, newPassword == null || newPassword.isBlank() ? "changeme" : newPassword);
  }

  public synchronized UserAccount findByUsername(String username){
    if (username == null){
      return null;
    }
    String id = byUsername.get(normalize(username));
    if (id == null){
      return null;
    }
    UserAccount account = byId.get(id);
    return account != null ? clone(account) : null;
  }

  public synchronized boolean matchesPassword(String id, String password){
    if (id == null){
      return false;
    }
    String expected = passwords.get(id);
    return Objects.equals(expected, password);
  }

  private void store(UserAccount account){
    byId.put(account.getId(), clone(account));
    if (account.getUsername() != null){
      byUsername.put(normalize(account.getUsername()), account.getId());
    }
  }

  private String normalize(String username){
    return username == null ? "" : username.trim().toLowerCase();
  }

  private UserAccount clone(UserAccount source){
    if (source == null){
      return null;
    }
    UserAccount copy = new UserAccount();
    copy.setId(source.getId());
    copy.setUsername(source.getUsername());
    copy.setDisplayName(source.getDisplayName());
    copy.setRole(source.getRole());
    if (source.getAgency() != null){
      Agency agency = new Agency();
      agency.setId(source.getAgency().getId());
      agency.setName(source.getAgency().getName());
      copy.setAgency(agency);
    }
    return copy;
  }
}
