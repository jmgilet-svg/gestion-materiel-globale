package com.materiel.suite.client.settings;

/** Paramètres généraux côté client. */
public class GeneralSettings {
  private int sessionTimeoutMinutes = 30;
  private int autosaveIntervalSeconds = 30;
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

  private static String trimToNull(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
