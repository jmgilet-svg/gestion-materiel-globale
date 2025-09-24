package com.materiel.suite.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailControllerTest {

  private final MailController controller = new MailController();

  @Test
  void sendWithAttachmentsReturnsQueued(){
    String pdfBase64 = Base64.getEncoder().encodeToString("demo".getBytes(StandardCharsets.UTF_8));
    MailController.MailPayload payload = new MailController.MailPayload(
        List.of("to@example.test"),
        List.of("cc@example.test"),
        List.of("bcc@example.test"),
        "Subject",
        "<p>Body</p>",
        List.of(new MailController.MailPayload.Attachment("doc.pdf", "application/pdf", pdfBase64))
    );

    ResponseEntity<String> response = controller.send(payload);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("queued", response.getBody());
  }

  @Test
  void sendIgnoresInvalidBase64(){
    MailController.MailPayload payload = new MailController.MailPayload(
        List.of("to@example.test"),
        List.of(),
        List.of(),
        "Sujet",
        "Body",
        List.of(new MailController.MailPayload.Attachment("broken.txt", "text/plain", "???invalid???"))
    );

    ResponseEntity<String> response = controller.send(payload);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("queued", response.getBody());
  }
}
