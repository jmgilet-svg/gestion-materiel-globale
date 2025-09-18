package com.materiel.suite.backend.auth.dto;

/** Requête pour changer le mot de passe d'un utilisateur. */
public class PasswordUpdateRequest {
  private String newPassword;

  public String getNewPassword(){
    return newPassword;
  }

  public void setNewPassword(String newPassword){
    this.newPassword = newPassword;
  }
}
