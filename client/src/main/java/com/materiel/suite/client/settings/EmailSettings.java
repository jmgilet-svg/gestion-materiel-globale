package com.materiel.suite.client.settings;

/** Paramètres SMTP pour l'envoi des ordres de mission par email. */
public class EmailSettings {
  private String smtpHost;
  private int smtpPort = 587;
  private boolean starttls = true;
  private boolean auth = true;
  private String username;
  private String password;
  private String fromAddress;
  private String fromName;
  private String ccAddress;
  private String subjectTemplate = "Ordre de mission — ${date} — ${client}";
  private String bodyTemplate = """
Bonjour,

Veuillez trouver ci-joint votre ordre de mission pour ${date} (${timeRange})${clientLine}.
Adresse: ${address}
Intervention: ${title}

Ressources associées: ${resourceList}

— Ce message a été généré automatiquement par la planification.
${pixel}
""";
  private String htmlTemplate = """
<p>Bonjour,</p>
<p>Veuillez trouver ci-joint votre ordre de mission pour <strong>${date}</strong> (<em>${timeRange}</em>)${clientLine}.<br/>
Adresse : ${address}<br/>
Intervention : ${title}</p>
<p>Ressources associées : ${resourceList}</p>
<p style="color:#666;font-size:12px">— Ce message a été généré automatiquement par la planification.</p>
${pixel}
""";
  private boolean enableHtml = true;
  private boolean enableOpenTracking = true;
  private String trackingBaseUrl;

  public String getSmtpHost(){
    return smtpHost;
  }

  public void setSmtpHost(String host){
    smtpHost = trimToNull(host);
  }

  public int getSmtpPort(){
    return smtpPort;
  }

  public void setSmtpPort(int port){
    smtpPort = port > 0 ? port : 587;
  }

  public boolean isStarttls(){
    return starttls;
  }

  public void setStarttls(boolean enabled){
    starttls = enabled;
  }

  public boolean isAuth(){
    return auth;
  }

  public void setAuth(boolean enabled){
    auth = enabled;
  }

  public String getUsername(){
    return username;
  }

  public void setUsername(String value){
    username = trimToNull(value);
  }

  public String getPassword(){
    return password;
  }

  public void setPassword(String value){
    password = value == null || value.isEmpty() ? null : value;
  }

  public String getFromAddress(){
    return fromAddress;
  }

  public void setFromAddress(String value){
    fromAddress = trimToNull(value);
  }

  public String getFromName(){
    return fromName;
  }

  public void setFromName(String value){
    fromName = trimToNull(value);
  }

  public String getCcAddress(){
    return ccAddress;
  }

  public void setCcAddress(String value){
    ccAddress = trimToNull(value);
  }

  public String getSubjectTemplate(){
    return subjectTemplate;
  }

  public void setSubjectTemplate(String template){
    subjectTemplate = template == null || template.isBlank() ? defaultSubject() : template;
  }

  public String getBodyTemplate(){
    return bodyTemplate;
  }

  public void setBodyTemplate(String template){
    bodyTemplate = template == null || template.isBlank() ? defaultBody() : template;
  }

  public String getHtmlTemplate(){
    return htmlTemplate;
  }

  public void setHtmlTemplate(String template){
    htmlTemplate = template == null || template.isBlank() ? defaultHtml() : template;
  }

  public boolean isEnableHtml(){
    return enableHtml;
  }

  public void setEnableHtml(boolean enable){
    enableHtml = enable;
  }

  public boolean isEnableOpenTracking(){
    return enableOpenTracking;
  }

  public void setEnableOpenTracking(boolean enable){
    enableOpenTracking = enable;
  }

  public String getTrackingBaseUrl(){
    return trackingBaseUrl;
  }

  public void setTrackingBaseUrl(String value){
    trackingBaseUrl = trimToNull(value);
  }

  private static String trimToNull(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String defaultSubject(){
    return "Ordre de mission — ${date} — ${client}";
  }

  private static String defaultBody(){
    return """
Bonjour,

Veuillez trouver ci-joint votre ordre de mission pour ${date} (${timeRange})${clientLine}.
Adresse: ${address}
Intervention: ${title}

Ressources associées: ${resourceList}

— Ce message a été généré automatiquement par la planification.
${pixel}
""";
  }

  private static String defaultHtml(){
    return """
<p>Bonjour,</p>
<p>Veuillez trouver ci-joint votre ordre de mission pour <strong>${date}</strong> (<em>${timeRange}</em>)${clientLine}.<br/>
Adresse : ${address}<br/>
Intervention : ${title}</p>
<p>Ressources associées : ${resourceList}</p>
<p style="color:#666;font-size:12px">— Ce message a été généré automatiquement par la planification.</p>
${pixel}
""";
  }
}
