package com.materiel.suite.client.service;

/** Service minimal pour l'envoi d'emails avec pièce jointe. */
public interface MailService {
  void sendWithAttachments(java.util.List<String> to,
                           java.util.List<String> cc,
                           java.util.List<String> bcc,
                           String subject,
                           String body,
                           java.util.List<Attachment> attachments);

  record Attachment(String name, String contentType, byte[] bytes){}
}
