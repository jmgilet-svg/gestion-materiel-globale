package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.service.MailService;

/** Implémentation mock qui trace l'envoi d'email dans la console. */
public class MockMailService implements MailService {
  @Override
  public void sendWithAttachment(String to, String subject, String body,
                                 String attachmentName, byte[] attachmentBytes, String contentType){
    int size = attachmentBytes == null ? 0 : attachmentBytes.length;
    System.out.println("[MOCK MAIL] to=" + to + " subject=" + subject + " attachment=" + attachmentName + " size=" + size);
  }
}
