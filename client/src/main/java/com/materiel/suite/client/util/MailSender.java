package com.materiel.suite.client.util;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.EmailSettings;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Envoi SMTP (texte + HTML) basé sur les préférences locales. */
public final class MailSender {
  private MailSender(){
  }

  public static void send(String to, String subject, String body, List<File> attachments) throws Exception {
    send(to, null, subject, body, null, sanitizeAttachments(attachments));
  }

  public static void send(String to, String cc, String subject, String body, List<File> attachments) throws Exception {
    send(to, cc, subject, body, null, sanitizeAttachments(attachments));
  }

  public static void send(String to, String cc, String subject, String bodyPlain, String bodyHtml, List<File> attachments)
      throws Exception {
    send(to, cc, subject, bodyPlain, bodyHtml, sanitizeAttachments(attachments));
  }

  public static void send(String toEmail, String ccEmail, String subject, String bodyPlain, String bodyHtml,
                          File... attachments) throws Exception {
    EmailSettings settings = ServiceLocator.emailSettings();
    if (settings.getSmtpHost() == null || settings.getFromAddress() == null){
      throw new IllegalStateException("SMTP ou expéditeur non configuré (Paramètres > Email).");
    }
    if (toEmail == null || toEmail.isBlank()){
      throw new IllegalArgumentException("Adresse destinataire manquante");
    }

    Properties props = new Properties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.host", settings.getSmtpHost());
    props.put("mail.smtp.port", Integer.toString(settings.getSmtpPort()));
    props.put("mail.smtp.starttls.enable", Boolean.toString(settings.isStarttls()));
    props.put("mail.smtp.auth", Boolean.toString(settings.isAuth()));

    Session session;
    if (settings.isAuth()){
      String username = settings.getUsername();
      String password = settings.getPassword();
      if (username == null || password == null){
        throw new IllegalStateException("Identifiants SMTP requis");
      }
      session = Session.getInstance(props, new Authenticator(){
        @Override
        protected PasswordAuthentication getPasswordAuthentication(){
          return new PasswordAuthentication(username, password);
        }
      });
    } else {
      session = Session.getInstance(props);
    }

    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(settings.getFromAddress(),
        settings.getFromName() == null ? "" : settings.getFromName()));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
    String ccToUse = chooseCc(ccEmail, settings.getCcAddress());
    if (ccToUse != null && !ccToUse.isBlank()){
      message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccToUse));
    }
    message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());

    MimeMultipart mixed = new MimeMultipart("mixed");
    message.setContent(mixed);

    MimeBodyPart alternativeWrapper = new MimeBodyPart();
    mixed.addBodyPart(alternativeWrapper);
    MimeMultipart alternative = new MimeMultipart("alternative");
    alternativeWrapper.setContent(alternative);

    String plain = bodyPlain == null ? "" : bodyPlain;
    String plainWithPixel = substitutePixel(plain, toEmail, false, settings);
    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText(plainWithPixel, StandardCharsets.UTF_8.name());
    alternative.addBodyPart(textPart);

    if (settings.isEnableHtml()){
      String htmlCandidate = bodyHtml == null || bodyHtml.isBlank() ? htmlFromPlain(plain) : bodyHtml;
      String htmlWithPixel = substitutePixel(htmlCandidate, toEmail, true, settings);
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setContent(htmlWithPixel, "text/html; charset=UTF-8");
      alternative.addBodyPart(htmlPart);
    }

    if (attachments != null){
      for (File attachment : attachments){
        if (attachment == null){
          continue;
        }
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(attachment);
        mixed.addBodyPart(attachmentPart);
      }
    }

    Transport.send(message);
  }

  public static void testSend(String to) throws Exception {
    send(to, null, "Test SMTP — Gestion Matériel", "Ceci est un test d'envoi.", "<p>Ceci est un test d'envoi.</p>");
  }

  private static File[] sanitizeAttachments(List<File> attachments){
    if (attachments == null || attachments.isEmpty()){
      return new File[0];
    }
    List<File> safe = new ArrayList<>();
    for (File file : attachments){
      if (file != null){
        safe.add(file);
      }
    }
    return safe.toArray(File[]::new);
  }

  private static String chooseCc(String preferred, String fallback){
    if (preferred != null && !preferred.isBlank()){
      return preferred;
    }
    if (fallback != null && !fallback.isBlank()){
      return fallback;
    }
    return null;
  }

  private static String htmlFromPlain(String plain){
    if (plain == null){
      return "";
    }
    String escaped = StringEscapeUtils.escapeHtml4(plain);
    return escaped.replace("\r", "").replace("\n", "<br/>");
  }

  private static String substitutePixel(String body, String toEmail, boolean html, EmailSettings settings){
    if (body == null || body.isEmpty()){
      return body == null ? "" : body;
    }
    if (!html){
      return removePixelMarkers(body);
    }
    if (!settings.isEnableOpenTracking()){
      return removePixelMarkers(body);
    }
    String base = settings.getTrackingBaseUrl();
    if (base == null || base.isBlank()){
      return removePixelMarkers(body);
    }
    String url = resolvePixelUrl(body, base, toEmail);
    String replacement = "<img src=\"" + url + "\" width=\"1\" height=\"1\" style=\"display:none\" alt=\"\"/>";
    String withExplicit = replaceExplicitPixel(body, replacement);
    return withExplicit.replace("${pixel}", replacement);
  }

  private static String removePixelMarkers(String body){
    if (body == null || body.isEmpty()){
      return body == null ? "" : body;
    }
    String withoutSimple = body.replace("${pixel}", "");
    return replaceExplicitPixel(withoutSimple, "");
  }

  private static String replaceExplicitPixel(String body, String replacement){
    if (body == null || body.isEmpty()){
      return body == null ? "" : body;
    }
    StringBuilder out = new StringBuilder();
    int index = 0;
    while (true){
      int start = body.indexOf("${pixel(", index);
      if (start < 0){
        out.append(body.substring(index));
        break;
      }
      out.append(body, index, start);
      int end = body.indexOf(")}", start);
      if (end < 0){
        index = start + 8;
        continue;
      }
      out.append(replacement);
      index = end + 2;
    }
    return out.toString();
  }

  private static String resolvePixelUrl(String body, String base, String defaultTo){
    int start = body == null ? -1 : body.indexOf("${pixel(");
    if (start >= 0){
      int end = body.indexOf(")}", start);
      if (end > start){
        String inside = body.substring(start + 8, end);
        String ids = null;
        String to = defaultTo;
        for (String part : inside.split(",")){
          String[] kv = part.split("=", 2);
          if (kv.length < 2){
            continue;
          }
          String key = kv[0].trim();
          String value = kv[1].trim();
          if (key.equalsIgnoreCase("ids")){
            ids = value.replace('|', ',');
          } else if (key.equalsIgnoreCase("to")){
            to = value;
          }
        }
        return buildPixelUrl(base, to, ids);
      }
    }
    return buildPixelUrl(base, defaultTo, null);
  }

  private static String buildPixelUrl(String base, String to, String ids){
    String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    StringBuilder url = new StringBuilder(normalizedBase).append("/api/v2/mail/open");
    url.append("?to=").append(urlEncode(to));
    if (ids != null && !ids.isBlank()){
      url.append("&ids=").append(urlEncode(ids));
    }
    return url.toString();
  }

  private static String urlEncode(String value){
    if (value == null){
      return "";
    }
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
