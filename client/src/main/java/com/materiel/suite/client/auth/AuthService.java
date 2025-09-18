package com.materiel.suite.client.auth;

import java.util.List;

/** Contrat minimal pour gérer la connexion côté client. */
public interface AuthService {
  List<Agency> listAgencies();

  User login(String agencyId, String username, String password);

  User current();

  void logout();
}
