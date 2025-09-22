package com.materiel.suite.backend.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v2/mail")
public class MailController {
  private static final Logger LOGGER = LoggerFactory.getLogger(MailController.class);

  public record MailPayload(List<String> to,
                            List<String> cc,
                            List<String> bcc,
                            String subject,
                            String body,
                            List<Attachment> attachments){
    public record Attachment(String name, String contentType, String base64){}
  }

  @PostMapping("/send")
  public ResponseEntity<String> send(@RequestBody MailPayload payload){
    int totalBytes = 0;
    List<MailPayload.Attachment> attachments = payload.attachments();
    if (attachments != null){
      for (MailPayload.Attachment attachment : attachments){
        if (attachment == null || attachment.base64() == null){
          continue;
        }
        try {
          totalBytes += Base64.getDecoder().decode(attachment.base64()).length;
        } catch (IllegalArgumentException ignore){
          // ignore invalid attachment
        }
      }
    }
    LOGGER.info("[MAIL] to={} cc={} bcc={} subject={} attCount={} bytes={}",
        payload.to(), payload.cc(), payload.bcc(), payload.subject(),
        attachments == null ? 0 : attachments.size(), totalBytes);
    return ResponseEntity.ok("queued");
  }
}
