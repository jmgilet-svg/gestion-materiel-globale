package com.materiel.suite.client.auth;

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
  }

  public static boolean isLogged(){
    return current != null;
  }

  public static void clear(){
    current = null;
  }
}
