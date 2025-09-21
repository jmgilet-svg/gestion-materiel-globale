package com.materiel.suite.client.events;

/** Événements liés aux paramètres. */
public final class SettingsEvents {
  private SettingsEvents(){
  }

  /** Diffusé après enregistrement des paramètres généraux. */
  public static final class GeneralSaved {
    public final int sessionTimeoutMinutes;
    public final int autosaveIntervalSeconds;
    public final int uiScalePercent;
    public final boolean highContrast;

    public GeneralSaved(int sessionTimeoutMinutes, int autosaveIntervalSeconds,
                        int uiScalePercent, boolean highContrast){
      this.sessionTimeoutMinutes = sessionTimeoutMinutes;
      this.autosaveIntervalSeconds = autosaveIntervalSeconds;
      this.uiScalePercent = uiScalePercent;
      this.highContrast = highContrast;
    }
  }
}
