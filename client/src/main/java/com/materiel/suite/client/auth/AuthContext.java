package com.materiel.suite.client.auth;

import com.materiel.suite.client.agency.AgencyContext;

/** Conserve l'utilisateur courant pour les contrôles d'accès côté client. */
public final class AuthContext {
  private static volatile User current;

  private AuthContext(){
  }

  public static User get(){
    return current;
  }

  public static void set(User user){
    current = user;
    if (user != null && user.getAgency() != null){
      AgencyContext.setAgency(user.getAgency());
    } else {
      AgencyContext.clear();
    }
  }

  public static boolean isLogged(){
    return current != null;
  }

  public static void clear(){
    current = null;
    AgencyContext.clear();
  }
}
