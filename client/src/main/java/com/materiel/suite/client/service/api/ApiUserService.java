package com.materiel.suite.client.service.api;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.Role;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.users.UserAccount;
import com.materiel.suite.client.users.UserService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Implémentation REST pour la gestion des comptes utilisateurs. */
public class ApiUserService implements UserService {
  private final RestClient rc;
  private final UserService fallback;

  public ApiUserService(RestClient rc, UserService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public List<UserAccount> list(){
    if (rc == null){
      return fallback != null ? fallback.list() : List.of();
    }
    try {
      String body = rc.get("/api/v2/users");
      List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<UserAccount> accounts = new ArrayList<>();
      for (Object item : arr){
        Map<String, Object> map = SimpleJson.asObj(item);
        UserAccount account = toAccount(map);
        if (account != null){
          accounts.add(account);
        }
      }
      return accounts;
    } catch (InterruptedException ex){
      Thread.currentThread().interrupt();
      return fallback != null ? fallback.list() : List.of();
    } catch (Exception ex){
      return fallback != null ? fallback.list() : List.of();
    }
  }

  @Override
  public UserAccount create(UserAccount account, String password){
    if (rc == null){
      return fallback != null ? fallback.create(account, password) : account;
    }
    try {
      String payload = buildCreatePayload(account, password);
      String response = rc.post("/api/v2/users", payload);
      Map<String, Object> map = SimpleJson.asObj(SimpleJson.parse(response));
      UserAccount created = toAccount(map);
      return created != null ? created : account;
    } catch (InterruptedException ex){
      Thread.currentThread().interrupt();
      return fallback != null ? fallback.create(account, password) : account;
    } catch (Exception ex){
      return fallback != null ? fallback.create(account, password) : account;
    }
  }

  @Override
  public UserAccount update(UserAccount account){
    if (rc == null){
      return fallback != null ? fallback.update(account) : account;
    }
    try {
      String payload = buildUserPayload(account);
      String response = rc.put("/api/v2/users/" + encode(account.getId()), payload);
      Map<String, Object> map = SimpleJson.asObj(SimpleJson.parse(response));
      UserAccount updated = toAccount(map);
      return updated != null ? updated : account;
    } catch (InterruptedException ex){
      Thread.currentThread().interrupt();
      return fallback != null ? fallback.update(account) : account;
    } catch (Exception ex){
      return fallback != null ? fallback.update(account) : account;
    }
  }

  @Override
  public void delete(String id){
    if (rc == null){
      if (fallback != null){
        fallback.delete(id);
      }
      return;
    }
    try {
      rc.delete("/api/v2/users/" + encode(id));
    } catch (InterruptedException ex){
      Thread.currentThread().interrupt();
      if (fallback != null){
        fallback.delete(id);
      }
    } catch (Exception ex){
      if (fallback != null){
        fallback.delete(id);
      }
    }
  }

  @Override
  public void updatePassword(String id, String newPassword){
    if (rc == null){
      if (fallback != null){
        fallback.updatePassword(id, newPassword);
      }
      return;
    }
    try {
      String payload = buildPasswordPayload(newPassword);
      rc.post("/api/v2/users/" + encode(id) + "/password", payload);
    } catch (InterruptedException ex){
      Thread.currentThread().interrupt();
      if (fallback != null){
        fallback.updatePassword(id, newPassword);
      }
    } catch (Exception ex){
      if (fallback != null){
        fallback.updatePassword(id, newPassword);
      }
    }
  }

  private UserAccount toAccount(Map<String, Object> map){
    if (map == null){
      return null;
    }
    UserAccount account = new UserAccount();
    account.setId(SimpleJson.str(map.get("id")));
    account.setUsername(SimpleJson.str(map.get("username")));
    account.setDisplayName(SimpleJson.str(map.get("displayName")));
    account.setRole(parseRole(SimpleJson.str(map.get("role"))));
    Object agencyObj = map.get("agency");
    if (agencyObj instanceof Map<?,?> raw){
      @SuppressWarnings("unchecked")
      Map<String, Object> agencyMap = (Map<String, Object>) raw;
      account.setAgency(toAgency(agencyMap));
    }
    return account;
  }

  private Agency toAgency(Map<String, Object> map){
    if (map == null){
      return null;
    }
    Agency agency = new Agency();
    agency.setId(SimpleJson.str(map.get("id")));
    agency.setName(SimpleJson.str(map.get("name")));
    return agency;
  }

  private Role parseRole(String value){
    if (value == null){
      return null;
    }
    try {
      return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex){
      return null;
    }
  }

  private String buildCreatePayload(UserAccount account, String password){
    String userJson = buildUserPayload(account);
    return new StringBuilder("{")
        .append("\"user\":").append(userJson == null ? "null" : userJson)
        .append(',')
        .append("\"password\":").append(stringOrNull(password))
        .append('}')
        .toString();
  }

  private String buildUserPayload(UserAccount account){
    if (account == null){
      return "null";
    }
    StringBuilder sb = new StringBuilder("{");
    sb.append("\"id\":").append(stringOrNull(account.getId())).append(',');
    sb.append("\"username\":").append(stringOrNull(account.getUsername())).append(',');
    sb.append("\"displayName\":").append(stringOrNull(account.getDisplayName())).append(',');
    sb.append("\"role\":").append(stringOrNull(account.getRole() != null ? account.getRole().name() : null)).append(',');
    sb.append("\"agency\":").append(buildAgencyPayload(account.getAgency()));
    sb.append('}');
    return sb.toString();
  }

  private String buildAgencyPayload(Agency agency){
    if (agency == null){
      return "null";
    }
    return new StringBuilder("{")
        .append("\"id\":").append(stringOrNull(agency.getId())).append(',')
        .append("\"name\":").append(stringOrNull(agency.getName()))
        .append('}')
        .toString();
  }

  private String buildPasswordPayload(String password){
    return new StringBuilder("{")
        .append("\"newPassword\":").append(stringOrNull(password))
        .append('}')
        .toString();
  }

  private String stringOrNull(String value){
    if (value == null){
      return "null";
    }
    return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }

  private String encode(String value){
    if (value == null){
      return "";
    }
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
