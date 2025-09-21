package com.materiel.suite.client.settings;

import java.util.Locale;

/** Paramètres généraux côté client. */
public class GeneralSettings {
  public static final String DEFAULT_BRAND_PRIMARY_HEX = "#1E88E5";
  public static final String DEFAULT_BRAND_SECONDARY_HEX = "#F4511E";
  private int sessionTimeoutMinutes = 30;
  private int autosaveIntervalSeconds = 30;
  /** TVA par défaut (%) appliquée si aucune valeur spécifique n'est fournie. */
  private Double defaultVatPercent = 20.0;
  /** Mode d'arrondi utilisé pour les montants monétaires. */
  private String roundingMode = "HALF_UP";
  /** Précision d'arrondi en nombre de décimales. */
  private int roundingScale = 2;
  /** Échelle d'interface (en %) comprise entre 80 et 130. */
  private int uiScalePercent = 100;
  /** Active un contraste renforcé (focus et couleurs plus marqués). */
  private boolean highContrast;
  /** Police adaptée à la dyslexie (OpenDyslexic) si disponible. */
  private boolean dyslexiaMode;
  /** Couleur primaire personnalisée (branding agence). */
  private String brandPrimaryHex = DEFAULT_BRAND_PRIMARY_HEX;
  /** Couleur secondaire utilisée pour les accents UI. */
  private String brandSecondaryHex = DEFAULT_BRAND_SECONDARY_HEX;
  /** Points supplémentaires appliqués sur la taille des polices (0 à 4). */
  private int fontExtraPoints;
  /** PNG encodé en Base64 (optionnel) utilisé en en-tête PDF (logo d’agence). */
  private String agencyLogoPngBase64;
  private String agencyName;
  private String agencyPhone;
  private String agencyAddress;
  /** CGV : soit un PDF encodé base64, soit un texte brut (fallback si PDF absent). */
  private String cgvPdfBase64;
  private String cgvText;

  public int getSessionTimeoutMinutes(){
    return sessionTimeoutMinutes;
  }

  public void setSessionTimeoutMinutes(int minutes){
    sessionTimeoutMinutes = Math.max(1, minutes);
  }

  public int getAutosaveIntervalSeconds(){
    return autosaveIntervalSeconds;
  }

  public void setAutosaveIntervalSeconds(int seconds){
    autosaveIntervalSeconds = Math.max(5, seconds);
  }

  public Double getDefaultVatPercent(){
    return defaultVatPercent;
  }

  public void setDefaultVatPercent(Double percent){
    if (percent == null){
      defaultVatPercent = null;
      return;
    }
    double value = Math.max(0d, Math.min(100d, percent));
    defaultVatPercent = value;
  }

  public String getRoundingMode(){
    return roundingMode;
  }

  public void setRoundingMode(String mode){
    if (mode == null){
      roundingMode = "HALF_UP";
      return;
    }
    String normalized = mode.trim();
    if (normalized.isEmpty()){
      roundingMode = "HALF_UP";
      return;
    }
    normalized = normalized.toUpperCase(Locale.ROOT);
    switch (normalized){
      case "HALF_DOWN":
      case "HALF_EVEN":
      case "HALF_UP":
        roundingMode = normalized;
        break;
      default:
        roundingMode = "HALF_UP";
        break;
    }
  }

  public int getRoundingScale(){
    return roundingScale;
  }

  public void setRoundingScale(int scale){
    if (scale < 0){
      roundingScale = 0;
    } else if (scale > 6){
      roundingScale = 6;
    } else {
      roundingScale = scale;
    }
  }

  public int getUiScalePercent(){
    return uiScalePercent;
  }

  public void setUiScalePercent(int percent){
    int sanitized = Math.max(80, Math.min(130, percent));
    uiScalePercent = sanitized;
  }

  public boolean isHighContrast(){
    return highContrast;
  }

  public void setHighContrast(boolean highContrast){
    this.highContrast = highContrast;
  }

  public boolean isDyslexiaMode(){
    return dyslexiaMode;
  }

  public void setDyslexiaMode(boolean dyslexiaMode){
    this.dyslexiaMode = dyslexiaMode;
  }

  public String getBrandPrimaryHex(){
    return brandPrimaryHex != null ? brandPrimaryHex : DEFAULT_BRAND_PRIMARY_HEX;
  }

  public void setBrandPrimaryHex(String value){
    brandPrimaryHex = sanitizeHexColor(value, DEFAULT_BRAND_PRIMARY_HEX);
  }

  public String getBrandSecondaryHex(){
    return brandSecondaryHex != null ? brandSecondaryHex : DEFAULT_BRAND_SECONDARY_HEX;
  }

  public void setBrandSecondaryHex(String value){
    brandSecondaryHex = sanitizeHexColor(value, DEFAULT_BRAND_SECONDARY_HEX);
  }

  public int getFontExtraPoints(){
    return clampFontExtra(fontExtraPoints);
  }

  public void setFontExtraPoints(int value){
    fontExtraPoints = clampFontExtra(value);
  }

  public String getAgencyLogoPngBase64(){
    return agencyLogoPngBase64;
  }

  public void setAgencyLogoPngBase64(String b64){
    agencyLogoPngBase64 = trimToNull(b64);
  }

  public String getAgencyName(){
    return agencyName;
  }

  public void setAgencyName(String value){
    agencyName = trimToNull(value);
  }

  public String getAgencyPhone(){
    return agencyPhone;
  }

  public void setAgencyPhone(String value){
    agencyPhone = trimToNull(value);
  }

  public String getAgencyAddress(){
    return agencyAddress;
  }

  public void setAgencyAddress(String value){
    agencyAddress = trimToNull(value);
  }

  public String getCgvPdfBase64(){
    return cgvPdfBase64;
  }

  public void setCgvPdfBase64(String value){
    cgvPdfBase64 = trimToNull(value);
  }

  public String getCgvText(){
    return cgvText;
  }

  public void setCgvText(String value){
    cgvText = trimToNull(value);
  }

  private static String sanitizeHexColor(String value, String defaultHex){
    if (value == null){
      return defaultHex;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()){
      return defaultHex;
    }
    String normalized = trimmed.startsWith("#") ? trimmed : "#" + trimmed;
    String upper = normalized.toUpperCase(Locale.ROOT);
    if (upper.matches("#([0-9A-F]{6}|[0-9A-F]{8})")){
      return upper;
    }
    return defaultHex;
  }

  private static int clampFontExtra(int value){
    if (value < 0){
      return 0;
    }
    return Math.min(value, 4);
  }

  private static String trimToNull(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
