package com.materiel.suite.client.service.api;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.auth.Role;
import com.materiel.suite.client.auth.User;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Implémentation API des opérations d'authentification (v2). */
public class ApiAuthService implements AuthService {
  private final RestClient rc;
  private final AuthService fallback;

  public ApiAuthService(RestClient rc, AuthService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public List<Agency> listAgencies(){
    if (rc == null){
      return fallback != null ? fallback.listAgencies() : List.of();
    }
    try {
      String body = rc.get("/api/v2/agencies");
      List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Agency> agencies = new ArrayList<>();
      for (Object item : arr){
        Map<String, Object> map = SimpleJson.asObj(item);
        Agency agency = toAgency(map);
        if (agency != null){
          agencies.add(agency);
        }
      }
      return agencies;
    } catch (Exception ex){
      return fallback != null ? fallback.listAgencies() : List.of();
    }
  }

  @Override
  public User login(String agencyId, String username, String password){
    if (rc == null){
      return fallback != null ? fallback.login(agencyId, username, password) : null;
    }
    try {
      String payload = buildLoginPayload(agencyId, username, password);
      String response = rc.post("/api/v2/auth/login", payload);
      Map<String, Object> map = SimpleJson.asObj(SimpleJson.parse(response));
      User user = toUser(map);
      if (user == null){
        throw new RuntimeException("Réponse de connexion invalide");
      }
      AuthContext.set(user);
      return user;
    } catch (IOException ex){
      if (isHttpUnauthorized(ex)){ // ne pas retomber sur le mock si le mot de passe est incorrect
        throw new RuntimeException("Identifiants invalides", ex);
      }
      return fallbackLogin(agencyId, username, password);
    } catch (InterruptedException ex){
      Thread.currentThread().interrupt();
      return fallbackLogin(agencyId, username, password);
    } catch (RuntimeException ex){
      throw ex;
    } catch (Exception ex){
      return fallbackLogin(agencyId, username, password);
    }
  }

  @Override
  public User current(){
    return AuthContext.get();
  }

  @Override
  public void logout(){
    AuthContext.clear();
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

  private User toUser(Map<String, Object> map){
    if (map == null){
      return null;
    }
    User user = new User();
    user.setId(SimpleJson.str(map.get("id")));
    user.setUsername(SimpleJson.str(map.get("username")));
    user.setDisplayName(SimpleJson.str(map.get("displayName")));
    user.setRole(parseRole(SimpleJson.str(map.get("role"))));
    Object agencyObj = map.get("agency");
    if (agencyObj instanceof Map<?,?> agencyMap){
      @SuppressWarnings("unchecked")
      Agency agency = toAgency((Map<String, Object>) agencyMap);
      user.setAgency(agency);
    }
    return user;
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

  private String buildLoginPayload(String agencyId, String username, String password){
    return new StringBuilder("{")
        .append("\"agencyId\":").append(stringOrNull(agencyId)).append(',')
        .append("\"username\":").append(stringOrNull(username)).append(',')
        .append("\"password\":").append(stringOrNull(password))
        .append('}')
        .toString();
  }

  private String stringOrNull(String value){
    if (value == null){
      return "null";
    }
    String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
    return '"' + escaped + '"';
  }

  private boolean isHttpUnauthorized(IOException ex){
    String message = ex.getMessage();
    return message != null && message.startsWith("HTTP 401");
  }

  private User fallbackLogin(String agencyId, String username, String password){
    if (fallback != null){
      return fallback.login(agencyId, username, password);
    }
    throw new RuntimeException("Service d'authentification indisponible");
  }
}
