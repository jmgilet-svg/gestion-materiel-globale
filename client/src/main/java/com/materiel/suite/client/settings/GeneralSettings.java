package com.materiel.suite.client.settings;

/** Paramètres généraux côté client. */
public class GeneralSettings {
  private int sessionTimeoutMinutes = 30;
  private int autosaveIntervalSeconds = 30;
  /** PNG encodé en Base64 (optionnel) utilisé en en-tête PDF (logo d’agence). */
  private String agencyLogoPngBase64;

  public int getSessionTimeoutMinutes(){
    return sessionTimeoutMinutes;
  }

  public void setSessionTimeoutMinutes(int minutes){
    sessionTimeoutMinutes = Math.max(1, minutes);
  }

  public int getAutosaveIntervalSeconds(){
    return autosaveIntervalSeconds;
  }

  public void setAutosaveIntervalSeconds(int seconds){
    autosaveIntervalSeconds = Math.max(5, seconds);
  }

  public String getAgencyLogoPngBase64(){
    return agencyLogoPngBase64;
  }

  public void setAgencyLogoPngBase64(String b64){
    agencyLogoPngBase64 = b64;
  }
}
