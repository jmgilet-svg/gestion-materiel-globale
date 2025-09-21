package com.materiel.suite.client.util;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.User;
import com.materiel.suite.client.settings.GeneralSettings;

import java.util.Locale;
import java.util.prefs.Preferences;

/** Accès simplifié aux préférences utilisateur côté client. */
public final class Prefs {
  private static final String ROOT_NODE = "gestion-materiel";
  private static final int MAX_NODE_SEGMENT = 64;

  private Prefs(){
  }

  public static int getSessionTimeoutMinutes(){
    return Math.max(1, readInt("session.timeout.minutes", 30));
  }

  public static void setSessionTimeoutMinutes(int minutes){
    prefs().putInt("session.timeout.minutes", Math.max(1, minutes));
  }

  public static int getAutosaveIntervalSeconds(){
    return Math.max(5, readInt("autosave.interval.seconds", 30));
  }

  public static void setAutosaveIntervalSeconds(int seconds){
    prefs().putInt("autosave.interval.seconds", Math.max(5, seconds));
  }

  public static double getDefaultVatPercent(){
    return readDouble("billing.vat.default.percent", 20.0d);
  }

  public static void setDefaultVatPercent(Double value){
    if (value == null){
      prefs().remove("billing.vat.default.percent");
      return;
    }
    double sanitized = Math.max(0d, Math.min(100d, value));
    prefs().putDouble("billing.vat.default.percent", sanitized);
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
      prefs().remove("billing.rounding.mode");
      return;
    }
    String trimmed = mode.trim();
    if (trimmed.isEmpty()){
      prefs().remove("billing.rounding.mode");
      return;
    }
    String normalized = trimmed.toUpperCase(Locale.ROOT);
    switch (normalized){
      case "HALF_DOWN":
      case "HALF_EVEN":
      case "HALF_UP":
        prefs().put("billing.rounding.mode", normalized);
        break;
      default:
        prefs().put("billing.rounding.mode", "HALF_UP");
        break;
    }
  }

  public static int getRoundingScale(){
    int stored = readInt("billing.rounding.scale", 2);
    if (stored < 0){
      return 0;
    }
    return Math.min(stored, 6);
  }

  public static void setRoundingScale(int scale){
    int sanitized = Math.max(0, Math.min(scale, 6));
    prefs().putInt("billing.rounding.scale", sanitized);
  }

  public static int getUiScalePercent(){
    int stored = readInt("ui.scale.percent", 100);
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
    prefs().putInt("ui.scale.percent", sanitized);
  }

  public static boolean isUiHighContrast(){
    return readBoolean("ui.high.contrast", false);
  }

  public static void setUiHighContrast(boolean enabled){
    prefs().putBoolean("ui.high.contrast", enabled);
  }

  public static boolean isUiDyslexiaMode(){
    return readBoolean("ui.dyslexia.mode", false);
  }

  public static void setUiDyslexiaMode(boolean enabled){
    prefs().putBoolean("ui.dyslexia.mode", enabled);
  }

  public static String getUiBrandPrimaryHex(){
    String stored = readTrimmed("ui.brand.primary.hex");
    return stored != null ? stored : GeneralSettings.DEFAULT_BRAND_PRIMARY_HEX;
  }

  public static void setUiBrandPrimaryHex(String value){
    if (value == null || value.isBlank()){
      prefs().remove("ui.brand.primary.hex");
    } else {
      prefs().put("ui.brand.primary.hex", value.trim());
    }
  }

  public static String getUiBrandSecondaryHex(){
    String stored = readTrimmed("ui.brand.secondary.hex");
    return stored != null ? stored : GeneralSettings.DEFAULT_BRAND_SECONDARY_HEX;
  }

  public static void setUiBrandSecondaryHex(String value){
    if (value == null || value.isBlank()){
      prefs().remove("ui.brand.secondary.hex");
    } else {
      prefs().put("ui.brand.secondary.hex", value.trim());
    }
  }

  public static int getUiFontExtraPoints(){
    int stored = readInt("ui.font.extra.points", 0);
    if (stored < 0){
      return 0;
    }
    if (stored > 4){
      return 4;
    }
    return stored;
  }

  public static void setUiFontExtraPoints(int value){
    int sanitized = Math.max(0, Math.min(value, 4));
    prefs().putInt("ui.font.extra.points", sanitized);
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
    int port = readInt("mail.smtp.port", 587);
    return port > 0 ? port : 587;
  }

  public static void setMailPort(int port){
    prefs().putInt("mail.smtp.port", port > 0 ? port : 587);
  }

  public static boolean isMailStarttls(){
    return readBoolean("mail.smtp.starttls", true);
  }

  public static void setMailStarttls(boolean enabled){
    prefs().putBoolean("mail.smtp.starttls", enabled);
  }

  public static boolean isMailAuth(){
    return readBoolean("mail.smtp.auth", true);
  }

  public static void setMailAuth(boolean enabled){
    prefs().putBoolean("mail.smtp.auth", enabled);
  }

  public static String getMailUsername(){
    return readTrimmed("mail.smtp.username");
  }

  public static void setMailUsername(String value){
    writeTrimmed("mail.smtp.username", value);
  }

  public static String getMailPassword(){
    String value = readRaw("mail.smtp.password");
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static void setMailPassword(String value){
    if (value == null || value.isBlank()){
      prefs().remove("mail.smtp.password");
    } else {
      prefs().put("mail.smtp.password", value);
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
    String value = readRaw("mail.subject.template");
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : value;
  }

  public static void setMailSubjectTemplate(String value){
    if (value == null || value.isBlank()){
      prefs().remove("mail.subject.template");
    } else {
      prefs().put("mail.subject.template", value);
    }
  }

  public static String getMailBodyTemplate(){
    String value = readRaw("mail.body.template");
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : value;
  }

  public static void setMailBodyTemplate(String value){
    if (value == null || value.isBlank()){
      prefs().remove("mail.body.template");
    } else {
      prefs().put("mail.body.template", value);
    }
  }

  public static String getMailHtmlTemplate(){
    String value = readRaw("mail.body.html.template");
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : value;
  }

  public static void setMailHtmlTemplate(String value){
    if (value == null || value.isBlank()){
      prefs().remove("mail.body.html.template");
    } else {
      prefs().put("mail.body.html.template", value);
    }
  }

  public static boolean isMailHtmlEnabled(){
    return readBoolean("mail.body.html.enabled", true);
  }

  public static void setMailHtmlEnabled(boolean enabled){
    prefs().putBoolean("mail.body.html.enabled", enabled);
  }

  public static boolean isMailTrackingEnabled(){
    return readBoolean("mail.tracking.enabled", true);
  }

  public static void setMailTrackingEnabled(boolean enabled){
    prefs().putBoolean("mail.tracking.enabled", enabled);
  }

  public static String getMailTrackingBaseUrl(){
    return readTrimmed("mail.tracking.base");
  }

  public static void setMailTrackingBaseUrl(String value){
    writeTrimmed("mail.tracking.base", value);
  }

  private static Preferences prefs(){
    return Preferences.userRoot().node(currentNodePath());
  }

  private static Preferences basePrefs(){
    return Preferences.userRoot().node(ROOT_NODE);
  }

  private static String currentNodePath(){
    String agencyId = sanitizeNodeSegment(currentAgencyId());
    if (agencyId == null){
      return ROOT_NODE;
    }
    return ROOT_NODE + "/agency/" + agencyId;
  }

  private static String currentAgencyId(){
    User user = AuthContext.get();
    if (user == null){
      return null;
    }
    Agency agency = user.getAgency();
    return agency != null ? agency.getId() : null;
  }

  private static String sanitizeNodeSegment(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()){
      return null;
    }
    StringBuilder sb = new StringBuilder(trimmed.length());
    for (int i = 0; i < trimmed.length(); i++){
      char ch = trimmed.charAt(i);
      if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.'){
        sb.append(ch);
      } else {
        sb.append('_');
      }
      if (sb.length() >= MAX_NODE_SEGMENT){
        break;
      }
    }
    String sanitized = sb.toString();
    return sanitized.isEmpty() ? null : sanitized;
  }

  private static int readInt(String key, int defaultValue){
    String raw = readRaw(key);
    if (raw == null){
      return defaultValue;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException ignore){
      return defaultValue;
    }
  }

  private static double readDouble(String key, double defaultValue){
    String raw = readRaw(key);
    if (raw == null){
      return defaultValue;
    }
    try {
      return Double.parseDouble(raw.trim());
    } catch (NumberFormatException ignore){
      return defaultValue;
    }
  }

  private static boolean readBoolean(String key, boolean defaultValue){
    String raw = readRaw(key);
    if (raw == null){
      return defaultValue;
    }
    return Boolean.parseBoolean(raw.trim());
  }

  private static String readRaw(String key){
    Preferences current = prefs();
    String value = current.get(key, null);
    if (value != null){
      return value;
    }
    Preferences fallback = basePrefs();
    if (fallback == current){
      return null;
    }
    return fallback.get(key, null);
  }

  private static String readTrimmed(String key){
    String value = readRaw(key);
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static void writeTrimmed(String key, String value){
    if (value == null){
      prefs().remove(key);
      return;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()){
      prefs().remove(key);
    } else {
      prefs().put(key, trimmed);
    }
  }
}
