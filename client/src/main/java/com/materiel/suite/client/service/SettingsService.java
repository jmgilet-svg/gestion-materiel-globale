package com.materiel.suite.client.service;

import com.materiel.suite.client.settings.EmailSettings;
import com.materiel.suite.client.settings.GeneralSettings;

public interface SettingsService {
  GeneralSettings getGeneral();

  void saveGeneral(GeneralSettings settings);

  EmailSettings getEmail();

  void saveEmail(EmailSettings settings);
}
