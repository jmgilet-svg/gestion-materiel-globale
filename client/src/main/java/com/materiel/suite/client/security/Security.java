package com.materiel.suite.client.security;

import com.materiel.suite.client.ui.planning.KanbanPanel;

/**
 * Utilitaires de sécurité côté client.
 * S'appuie si présent sur ServiceLocator.auth() ou ServiceLocator.session() via réflexion,
 * sinon retombe prudemment sur une autorisation minimale (ADMIN-only si détectable).
 */
public final class Security {
  private Security(){
  }

  /**
   * Indique si l'utilisateur courant possède le rôle demandé (ex. "ADMIN").
   */
  public static boolean hasRole(String role){
    if (role == null || role.isBlank()){
      return false;
    }
    // Tentative via ServiceLocator.auth().hasRole(String)
    try {
      Class<?> locator = Class.forName("com.materiel.suite.client.service.ServiceLocator");
      Object auth = locator.getMethod("auth").invoke(null);
      if (auth != null){
        try {
          Object result = auth.getClass().getMethod("hasRole", String.class).invoke(auth, role);
          if (result instanceof Boolean bool){
            return bool;
          }
        } catch (NoSuchMethodException ignored){
          // Alternative: getCurrentUserRole() retourne String/Enum
          Object current = auth.getClass().getMethod("getCurrentUserRole").invoke(auth);
          if (current != null){
            return roleEquals(current, role);
          }
        }
      }
    } catch (Exception ignored){
      // Fallback below.
    }
    // Second essai : ServiceLocator.session().hasRole(String)
    try {
      Class<?> locator = Class.forName("com.materiel.suite.client.service.ServiceLocator");
      Object session = locator.getMethod("session").invoke(null);
      if (session != null){
        try {
          Object result = session.getClass().getMethod("hasRole", String.class).invoke(session, role);
          if (result instanceof Boolean bool){
            return bool;
          }
        } catch (NoSuchMethodException ignored){
          // nothing else to attempt here
        }
      }
    } catch (Exception ignored){
      // Aucun service disponible -> refus par défaut.
    }
    // Par défaut : on ne sait pas -> on refuse (principe de moindre privilège)
    return false;
  }

  /**
   * true si l'utilisateur a au moins un des rôles.
   */
  public static boolean hasAnyRole(String... roles){
    if (roles == null){
      return false;
    }
    for (String role : roles){
      if (hasRole(role)){
        return true;
      }
    }
    return false;
  }

  /**
   * Politique DnD : qui peut déposer vers telle colonne.
   */
  public static boolean canDropTo(KanbanPanel.Stage stage){
    if (stage == null){
      return false;
    }
    if (stage == KanbanPanel.Stage.INVOICED){
      return hasAnyRole("ADMIN", "SALES");
    }
    // autres colonnes: autorisées par défaut
    return true;
  }

  private static boolean roleEquals(Object currentRole, String expected){
    if (currentRole == null || expected == null){
      return false;
    }
    String current = String.valueOf(currentRole).trim();
    return current.equalsIgnoreCase(expected);
  }
}
