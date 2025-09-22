package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.service.MailService;

import java.util.List;

/** Implémentation mock qui trace l'envoi d'email dans la console. */
public class MockMailService implements MailService {
  @Override
  public void sendWithAttachments(List<String> to,
                                  List<String> cc,
                                  List<String> bcc,
                                  String subject,
                                  String body,
                                  List<Attachment> attachments){
    int size = 0;
    if (attachments != null){
      for (Attachment att : attachments){
        if (att != null && att.bytes() != null){
          size += att.bytes().length;
        }
      }
    }
    System.out.println("[MOCK MAIL] to=" + to + " cc=" + cc + " bcc=" + bcc
        + " subject=" + subject + " attachments=" + (attachments == null ? 0 : attachments.size())
        + " size=" + size);
  }
}
