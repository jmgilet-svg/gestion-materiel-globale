package com.materiel.suite.backend.web;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Rendu HTML -> PDF pour l'API v2. */
@RestController
@RequestMapping("/api/v2/pdf")
public class PdfRenderController {
  public record RenderPayload(String html, Map<String, String> inlineImages, String baseUrl){}

  @PostMapping("/render")
  public ResponseEntity<byte[]> render(@RequestBody RenderPayload payload){
    try {
      String html = payload.html() == null ? "<html><body></body></html>" : payload.html();
      Map<String, String> images = payload.inlineImages();
      if (images != null && !images.isEmpty()){
        for (var entry : images.entrySet()){
          String cid = entry.getKey();
          String b64 = entry.getValue();
          if (cid != null && b64 != null){
            html = html.replace("cid:" + cid, "data:image/png;base64," + b64);
          }
        }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      if (payload.baseUrl() != null && !payload.baseUrl().isBlank()){
        builder.withHtmlContent(html, payload.baseUrl());
      } else {
        builder.withHtmlContent(html, null);
      }
      builder.toStream(out);
      builder.run();
      byte[] pdf = out.toByteArray();
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=document.pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdf);
    } catch (Exception ex){
      String message = "PDF render error: " + ex.getMessage();
      return ResponseEntity.badRequest()
          .contentType(MediaType.TEXT_PLAIN)
          .body(message.getBytes(StandardCharsets.UTF_8));
    }
  }
}
