package com.materiel.suite.client.util;

import java.util.Locale;
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

  public static double getDefaultVatPercent(){
    return PREFS.getDouble("billing.vat.default.percent", 20.0d);
  }

  public static void setDefaultVatPercent(Double value){
    if (value == null){
      PREFS.remove("billing.vat.default.percent");
      return;
    }
    double sanitized = Math.max(0d, Math.min(100d, value));
    PREFS.putDouble("billing.vat.default.percent", sanitized);
  }

  public static String getRoundingMode(){
    String stored = readTrimmed("billing.rounding.mode");
    if (stored == null){
      return "HALF_UP";
    }
    String normalized = stored.toUpperCase(Locale.ROOT);
    return switch (normalized){
      case "HALF_DOWN", "HALF_EVEN", "HALF_UP" -> normalized;
      default -> "HALF_UP";
    };
  }

  public static void setRoundingMode(String mode){
    if (mode == null){
      PREFS.remove("billing.rounding.mode");
      return;
    }
    String trimmed = mode.trim();
    if (trimmed.isEmpty()){
      PREFS.remove("billing.rounding.mode");
      return;
    }
    String normalized = trimmed.toUpperCase(Locale.ROOT);
    switch (normalized){
      case "HALF_DOWN":
      case "HALF_EVEN":
      case "HALF_UP":
        PREFS.put("billing.rounding.mode", normalized);
        break;
      default:
        PREFS.put("billing.rounding.mode", "HALF_UP");
        break;
    }
  }

  public static int getRoundingScale(){
    int stored = PREFS.getInt("billing.rounding.scale", 2);
    if (stored < 0){
      return 0;
    }
    return Math.min(stored, 6);
  }

  public static void setRoundingScale(int scale){
    int sanitized = Math.max(0, Math.min(scale, 6));
    PREFS.putInt("billing.rounding.scale", sanitized);
  }

  public static int getUiScalePercent(){
    int stored = PREFS.getInt("ui.scale.percent", 100);
    if (stored < 80){
      return 80;
    }
    if (stored > 130){
      return 130;
    }
    return stored;
  }

  public static void setUiScalePercent(int percent){
    int sanitized = Math.max(80, Math.min(percent, 130));
    PREFS.putInt("ui.scale.percent", sanitized);
  }

  public static boolean isUiHighContrast(){
    return PREFS.getBoolean("ui.high.contrast", false);
  }

  public static void setUiHighContrast(boolean enabled){
    PREFS.putBoolean("ui.high.contrast", enabled);
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

  public static String getMailHost(){
    return readTrimmed("mail.smtp.host");
  }

  public static void setMailHost(String value){
    writeTrimmed("mail.smtp.host", value);
  }

  public static int getMailPort(){
    int port = PREFS.getInt("mail.smtp.port", 587);
    return port > 0 ? port : 587;
  }

  public static void setMailPort(int port){
    PREFS.putInt("mail.smtp.port", port > 0 ? port : 587);
  }

  public static boolean isMailStarttls(){
    return PREFS.getBoolean("mail.smtp.starttls", true);
  }

  public static void setMailStarttls(boolean enabled){
    PREFS.putBoolean("mail.smtp.starttls", enabled);
  }

  public static boolean isMailAuth(){
    return PREFS.getBoolean("mail.smtp.auth", true);
  }

  public static void setMailAuth(boolean enabled){
    PREFS.putBoolean("mail.smtp.auth", enabled);
  }

  public static String getMailUsername(){
    return readTrimmed("mail.smtp.username");
  }

  public static void setMailUsername(String value){
    writeTrimmed("mail.smtp.username", value);
  }

  public static String getMailPassword(){
    String value = PREFS.get("mail.smtp.password", null);
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static void setMailPassword(String value){
    if (value == null || value.isBlank()){
      PREFS.remove("mail.smtp.password");
    } else {
      PREFS.put("mail.smtp.password", value);
    }
  }

  public static String getMailFromAddress(){
    return readTrimmed("mail.from.address");
  }

  public static void setMailFromAddress(String value){
    writeTrimmed("mail.from.address", value);
  }

  public static String getMailFromName(){
    return readTrimmed("mail.from.name");
  }

  public static void setMailFromName(String value){
    writeTrimmed("mail.from.name", value);
  }

  public static String getMailCcAddress(){
    return readTrimmed("mail.cc.address");
  }

  public static void setMailCcAddress(String value){
    writeTrimmed("mail.cc.address", value);
  }

  public static String getMailSubjectTemplate(){
    String value = PREFS.get("mail.subject.template", null);
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static void setMailSubjectTemplate(String value){
    if (value == null || value.isBlank()){
      PREFS.remove("mail.subject.template");
    } else {
      PREFS.put("mail.subject.template", value);
    }
  }

  public static String getMailBodyTemplate(){
    String value = PREFS.get("mail.body.template", null);
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : value;
  }

  public static void setMailBodyTemplate(String value){
    if (value == null || value.isBlank()){
      PREFS.remove("mail.body.template");
    } else {
      PREFS.put("mail.body.template", value);
    }
  }

  public static String getMailHtmlTemplate(){
    String value = PREFS.get("mail.body.html.template", null);
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : value;
  }

  public static void setMailHtmlTemplate(String value){
    if (value == null || value.isBlank()){
      PREFS.remove("mail.body.html.template");
    } else {
      PREFS.put("mail.body.html.template", value);
    }
  }

  public static boolean isMailHtmlEnabled(){
    return PREFS.getBoolean("mail.body.html.enabled", true);
  }

  public static void setMailHtmlEnabled(boolean enabled){
    PREFS.putBoolean("mail.body.html.enabled", enabled);
  }

  public static boolean isMailTrackingEnabled(){
    return PREFS.getBoolean("mail.tracking.enabled", true);
  }

  public static void setMailTrackingEnabled(boolean enabled){
    PREFS.putBoolean("mail.tracking.enabled", enabled);
  }

  public static String getMailTrackingBaseUrl(){
    return readTrimmed("mail.tracking.base");
  }

  public static void setMailTrackingBaseUrl(String value){
    writeTrimmed("mail.tracking.base", value);
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
