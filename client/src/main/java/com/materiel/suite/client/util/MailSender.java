package com.materiel.suite.client.util;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.EmailSettings;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Envoi SMTP simple basé sur les préférences locales. */
public final class MailSender {
  private MailSender(){
  }

  public static void send(String to, String subject, String body, List<File> attachments) throws Exception {
    send(to, null, subject, body, attachments);
  }

  public static void send(String to, String cc, String subject, String body, List<File> attachments) throws Exception {
    EmailSettings settings = ServiceLocator.emailSettings();
    if (settings.getSmtpHost() == null || settings.getFromAddress() == null){
      throw new IllegalStateException("SMTP ou expéditeur non configuré (Paramètres > Email).");
    }
    if (to == null || to.isBlank()){
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
    try {
      message.setFrom(new InternetAddress(settings.getFromAddress(),
          settings.getFromName() == null ? "" : settings.getFromName()));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
      String ccToUse = chooseCc(cc, settings.getCcAddress());
      if (ccToUse != null && !ccToUse.isBlank()){
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccToUse));
      }
      message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());

      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(body == null ? "" : body, StandardCharsets.UTF_8.name());

      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(textPart);

      for (File file : safeList(attachments)){
        if (file == null){
          continue;
        }
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.attachFile(file);
        multipart.addBodyPart(attachment);
      }

      message.setContent(multipart);
      Transport.send(message);
    } catch (MessagingException ex){
      throw ex;
    }
  }

  public static void testSend(String to) throws Exception {
    send(to, null, "Test SMTP — Gestion Matériel", "Ceci est un test d'envoi.", List.of());
  }

  private static List<File> safeList(List<File> files){
    if (files == null || files.isEmpty()){
      return Collections.emptyList();
    }
    List<File> copy = new ArrayList<>();
    for (File file : files){
      if (file != null){
        copy.add(file);
      }
    }
    return copy;
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
}
