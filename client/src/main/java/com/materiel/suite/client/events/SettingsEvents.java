package com.materiel.suite.client.events;

/** Événements liés aux paramètres. */
public final class SettingsEvents {
  private SettingsEvents(){
  }

  /** Diffusé après enregistrement des paramètres généraux. */
  public static final class GeneralSaved {
    public final int sessionTimeoutMinutes;
    public final int autosaveIntervalSeconds;

    public GeneralSaved(int sessionTimeoutMinutes, int autosaveIntervalSeconds){
      this.sessionTimeoutMinutes = sessionTimeoutMinutes;
      this.autosaveIntervalSeconds = autosaveIntervalSeconds;
    }
  }
}
