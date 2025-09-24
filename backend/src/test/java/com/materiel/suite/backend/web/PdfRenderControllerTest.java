package com.materiel.suite.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfRenderControllerTest {

  private final PdfRenderController controller = new PdfRenderController();

  @Test
  void renderMinimalHtmlProducesPdfHeader(){
    PdfRenderController.RenderPayload payload = new PdfRenderController.RenderPayload(
        "<html><body><h1>Test</h1></body></html>", Map.of(), null);

    ResponseEntity<byte[]> response = controller.render(payload);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "render should succeed");
    byte[] body = response.getBody();
    assertNotNull(body, "pdf body");
    assertTrue(body.length > 4, "pdf must not be empty");
    assertEquals('%', body[0], "PDF signature 0");
    assertEquals('P', body[1], "PDF signature 1");
    assertEquals('D', body[2], "PDF signature 2");
    assertEquals('F', body[3], "PDF signature 3");
  }

  @Test
  void renderSupportsInlineImages(){
    String html = "<html><body><img src=\"cid:logo\"/></body></html>";
    Map<String, String> inline = Map.of(
        "logo", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wIAAgMBAp0NtwAAAABJRU5ErkJggg=="
    );
    PdfRenderController.RenderPayload payload = new PdfRenderController.RenderPayload(html, inline, null);

    ResponseEntity<byte[]> response = controller.render(payload);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "render should succeed with inline images");
    byte[] body = response.getBody();
    assertNotNull(body);
    assertTrue(body.length > 4);
  }
}
