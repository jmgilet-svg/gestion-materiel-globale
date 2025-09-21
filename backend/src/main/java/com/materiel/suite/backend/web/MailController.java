package com.materiel.suite.backend.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/api/v2/mail")
public class MailController {
  private static final Logger LOGGER = LoggerFactory.getLogger(MailController.class);

  public record MailPayload(
      String to,
      String subject,
      String body,
      String attachmentName,
      String attachmentBase64,
      String contentType
  ){}

  @PostMapping("/send")
  public ResponseEntity<String> send(@RequestBody MailPayload payload){
    int size = 0;
    if (payload.attachmentBase64() != null && !payload.attachmentBase64().isBlank()){
      try {
        size = Base64.getDecoder().decode(payload.attachmentBase64()).length;
      } catch (IllegalArgumentException ignore){
        size = 0;
      }
    }
    LOGGER.info("[MAIL] to={} subject={} attachment={} size={}B", payload.to(), payload.subject(), payload.attachmentName(), size);
    return ResponseEntity.ok("queued");
  }
}
