package com.materiel.suite.client.service;

/** Service minimal pour l'envoi d'emails avec pièce jointe. */
public interface MailService {
  void sendWithAttachment(String to, String subject, String body,
                          String attachmentName, byte[] attachmentBytes, String contentType);
}
