package com.materiel.suite.client.auth;

/** Rôles utilisateurs pour les droits applicatifs côté client. */
public enum Role {
  ADMIN, // tous les droits
  SALES, // lecture planning + droits devis/BC/BL/factures, pas de config
  CONFIG // lecture générale, édition ressources + paramètres
}
