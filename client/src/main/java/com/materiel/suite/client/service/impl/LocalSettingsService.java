package com.materiel.suite.client.service.impl;

import com.materiel.suite.client.service.SettingsService;
import com.materiel.suite.client.settings.EmailSettings;
import com.materiel.suite.client.settings.GeneralSettings;
import com.materiel.suite.client.util.Prefs;

/** Stocke les paramètres localement via {@link Prefs}. */
public class LocalSettingsService implements SettingsService {
  @Override
  public GeneralSettings getGeneral(){
    GeneralSettings settings = new GeneralSettings();
    settings.setSessionTimeoutMinutes(Prefs.getSessionTimeoutMinutes());
    settings.setAutosaveIntervalSeconds(Prefs.getAutosaveIntervalSeconds());
    settings.setDefaultVatPercent(Prefs.getDefaultVatPercent());
    settings.setRoundingMode(Prefs.getRoundingMode());
    settings.setRoundingScale(Prefs.getRoundingScale());
    settings.setAgencyLogoPngBase64(Prefs.getAgencyLogoPngBase64());
    settings.setAgencyName(Prefs.getAgencyName());
    settings.setAgencyPhone(Prefs.getAgencyPhone());
    settings.setAgencyAddress(Prefs.getAgencyAddress());
    settings.setCgvPdfBase64(Prefs.getCgvPdfBase64());
    settings.setCgvText(Prefs.getCgvText());
    return settings;
  }

  @Override
  public void saveGeneral(GeneralSettings settings){
    if (settings == null){
      return;
    }
    Prefs.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
    Prefs.setAutosaveIntervalSeconds(settings.getAutosaveIntervalSeconds());
    Prefs.setDefaultVatPercent(settings.getDefaultVatPercent());
    Prefs.setRoundingMode(settings.getRoundingMode());
    Prefs.setRoundingScale(settings.getRoundingScale());
    Prefs.setAgencyLogoPngBase64(settings.getAgencyLogoPngBase64());
    Prefs.setAgencyName(settings.getAgencyName());
    Prefs.setAgencyPhone(settings.getAgencyPhone());
    Prefs.setAgencyAddress(settings.getAgencyAddress());
    Prefs.setCgvPdfBase64(settings.getCgvPdfBase64());
    Prefs.setCgvText(settings.getCgvText());
  }

  @Override
  public EmailSettings getEmail(){
    EmailSettings settings = new EmailSettings();
    settings.setSmtpHost(Prefs.getMailHost());
    settings.setSmtpPort(Prefs.getMailPort());
    settings.setStarttls(Prefs.isMailStarttls());
    settings.setAuth(Prefs.isMailAuth());
    settings.setUsername(Prefs.getMailUsername());
    settings.setPassword(Prefs.getMailPassword());
    settings.setFromAddress(Prefs.getMailFromAddress());
    settings.setFromName(Prefs.getMailFromName());
    settings.setCcAddress(Prefs.getMailCcAddress());
    String subject = Prefs.getMailSubjectTemplate();
    if (subject != null){
      settings.setSubjectTemplate(subject);
    }
    String body = Prefs.getMailBodyTemplate();
    if (body != null){
      settings.setBodyTemplate(body);
    }
    String html = Prefs.getMailHtmlTemplate();
    if (html != null){
      settings.setHtmlTemplate(html);
    }
    settings.setEnableHtml(Prefs.isMailHtmlEnabled());
    settings.setEnableOpenTracking(Prefs.isMailTrackingEnabled());
    settings.setTrackingBaseUrl(Prefs.getMailTrackingBaseUrl());
    return settings;
  }

  @Override
  public void saveEmail(EmailSettings settings){
    if (settings == null){
      return;
    }
    Prefs.setMailHost(settings.getSmtpHost());
    Prefs.setMailPort(settings.getSmtpPort());
    Prefs.setMailStarttls(settings.isStarttls());
    Prefs.setMailAuth(settings.isAuth());
    Prefs.setMailUsername(settings.getUsername());
    Prefs.setMailPassword(settings.getPassword());
    Prefs.setMailFromAddress(settings.getFromAddress());
    Prefs.setMailFromName(settings.getFromName());
    Prefs.setMailCcAddress(settings.getCcAddress());
    Prefs.setMailSubjectTemplate(settings.getSubjectTemplate());
    Prefs.setMailBodyTemplate(settings.getBodyTemplate());
    Prefs.setMailHtmlTemplate(settings.getHtmlTemplate());
    Prefs.setMailHtmlEnabled(settings.isEnableHtml());
    Prefs.setMailTrackingEnabled(settings.isEnableOpenTracking());
    Prefs.setMailTrackingBaseUrl(settings.getTrackingBaseUrl());
  }
}
