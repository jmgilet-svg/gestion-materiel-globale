package com.materiel.suite.client.auth;

/** Règles centralisées pour activer/désactiver des fonctionnalités selon le rôle. */
public final class AccessControl {
  private AccessControl(){
  }

  private static Role currentRole(){
    User user = AuthContext.get();
    return user != null ? user.getRole() : null;
  }

  public static boolean isAdmin(){
    return currentRole() == Role.ADMIN;
  }

  public static boolean canViewPlanning(){
    return currentRole() != null;
  }

  public static boolean canEditInterventions(){
    return currentRole() == Role.ADMIN;
  }

  public static boolean canViewSales(){
    Role role = currentRole();
    return role == Role.ADMIN || role == Role.SALES;
  }

  public static boolean canEditSales(){
    Role role = currentRole();
    return role == Role.ADMIN || role == Role.SALES;
  }

  public static boolean canViewResources(){
    return currentRole() != null;
  }

  public static boolean canEditResources(){
    Role role = currentRole();
    return role == Role.ADMIN || role == Role.CONFIG;
  }

  public static boolean canViewSettings(){
    Role role = currentRole();
    return role == Role.ADMIN || role == Role.CONFIG;
  }

  public static boolean canEditSettings(){
    Role role = currentRole();
    return role == Role.ADMIN || role == Role.CONFIG;
  }

  public static boolean canManageUsers(){
    return currentRole() == Role.ADMIN;
  }

  public static boolean canChangeOwnPassword(){
    return currentRole() != null;
  }
}
