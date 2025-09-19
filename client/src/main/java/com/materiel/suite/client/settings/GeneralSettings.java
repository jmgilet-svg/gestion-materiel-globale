package com.materiel.suite.client.settings;

/** Paramètres généraux côté client. */
public class GeneralSettings {
  private int sessionTimeoutMinutes = 30;
  private int autosaveIntervalSeconds = 30;

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
}
