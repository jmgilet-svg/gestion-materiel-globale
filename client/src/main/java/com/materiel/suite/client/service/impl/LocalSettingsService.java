package com.materiel.suite.client.service.impl;

import com.materiel.suite.client.service.SettingsService;
import com.materiel.suite.client.settings.GeneralSettings;
import com.materiel.suite.client.util.Prefs;

/** Stocke les paramètres localement via {@link Prefs}. */
public class LocalSettingsService implements SettingsService {
  @Override
  public GeneralSettings getGeneral(){
    GeneralSettings settings = new GeneralSettings();
    settings.setSessionTimeoutMinutes(Prefs.getSessionTimeoutMinutes());
    settings.setAutosaveIntervalSeconds(Prefs.getAutosaveIntervalSeconds());
    settings.setAgencyLogoPngBase64(Prefs.getAgencyLogoPngBase64());
    return settings;
  }

  @Override
  public void saveGeneral(GeneralSettings settings){
    if (settings == null){
      return;
    }
    Prefs.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
    Prefs.setAutosaveIntervalSeconds(settings.getAutosaveIntervalSeconds());
    Prefs.setAgencyLogoPngBase64(settings.getAgencyLogoPngBase64());
  }
}
