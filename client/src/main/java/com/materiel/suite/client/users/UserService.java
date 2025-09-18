package com.materiel.suite.client.users;

import java.util.List;

/** Contrat minimal pour gérer les comptes utilisateurs côté client. */
public interface UserService {
  List<UserAccount> list();

  UserAccount create(UserAccount account, String password);

  UserAccount update(UserAccount account);

  void delete(String id);

  void updatePassword(String id, String newPassword);
}
