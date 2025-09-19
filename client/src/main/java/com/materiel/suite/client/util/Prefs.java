package com.materiel.suite.client.util;

import java.util.prefs.Preferences;

/** Accès simplifié aux préférences utilisateur côté client. */
public final class Prefs {
  private static final Preferences PREFS = Preferences.userRoot().node("gestion-materiel");

  private Prefs(){
  }

  public static int getSessionTimeoutMinutes(){
    return Math.max(1, PREFS.getInt("session.timeout.minutes", 30));
  }

  public static void setSessionTimeoutMinutes(int minutes){
    PREFS.putInt("session.timeout.minutes", Math.max(1, minutes));
  }

  public static int getAutosaveIntervalSeconds(){
    return Math.max(5, PREFS.getInt("autosave.interval.seconds", 30));
  }

  public static void setAutosaveIntervalSeconds(int seconds){
    PREFS.putInt("autosave.interval.seconds", Math.max(5, seconds));
  }

  public static String getAgencyLogoPngBase64(){
    return readTrimmed("agency.logo.png.base64");
  }

  public static void setAgencyLogoPngBase64(String base64){
    writeTrimmed("agency.logo.png.base64", base64);
  }

  public static String getAgencyName(){
    return readTrimmed("agency.name");
  }

  public static void setAgencyName(String value){
    writeTrimmed("agency.name", value);
  }

  public static String getAgencyPhone(){
    return readTrimmed("agency.phone");
  }

  public static void setAgencyPhone(String value){
    writeTrimmed("agency.phone", value);
  }

  public static String getAgencyAddress(){
    return readTrimmed("agency.address");
  }

  public static void setAgencyAddress(String value){
    writeTrimmed("agency.address", value);
  }

  public static String getCgvPdfBase64(){
    return readTrimmed("agency.cgv.pdf.base64");
  }

  public static void setCgvPdfBase64(String base64){
    writeTrimmed("agency.cgv.pdf.base64", base64);
  }

  public static String getCgvText(){
    return readTrimmed("agency.cgv.text");
  }

  public static void setCgvText(String value){
    writeTrimmed("agency.cgv.text", value);
  }

  private static String readTrimmed(String key){
    String value = PREFS.get(key, null);
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static void writeTrimmed(String key, String value){
    if (value == null){
      PREFS.remove(key);
      return;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()){
      PREFS.remove(key);
      return;
    }
    PREFS.put(key, trimmed);
  }
}
