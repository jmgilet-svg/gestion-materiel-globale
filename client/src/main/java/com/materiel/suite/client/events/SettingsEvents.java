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
    public final boolean dyslexiaMode;
    public final String brandPrimaryHex;

    public GeneralSaved(int sessionTimeoutMinutes, int autosaveIntervalSeconds,
                        int uiScalePercent, boolean highContrast,
                        boolean dyslexiaMode, String brandPrimaryHex){
      this.sessionTimeoutMinutes = sessionTimeoutMinutes;
      this.autosaveIntervalSeconds = autosaveIntervalSeconds;
      this.uiScalePercent = uiScalePercent;
      this.highContrast = highContrast;
      this.dyslexiaMode = dyslexiaMode;
      this.brandPrimaryHex = brandPrimaryHex;
    }
  }
}
