package com.materiel.suite.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class QrControllerTest {

  private final QrController controller = new QrController();

  @Test
  void generatesPngPayload() throws Exception {
    ResponseEntity<byte[]> response = controller.qr("hello", 128);
    assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().length > 0);
  }
}
