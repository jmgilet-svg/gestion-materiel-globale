package com.materiel.suite.client.service;

import com.materiel.suite.client.settings.GeneralSettings;

public interface SettingsService {
  GeneralSettings getGeneral();

  void saveGeneral(GeneralSettings settings);
}
