package com.materiel.suite.server.api.v2.pdf;

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

@RestController
@RequestMapping("/api/v2/pdf")
public class PdfRenderControllerV2 {

  public record RenderPayload(String html, Map<String,String> inlineImages, String baseUrl){}

  @PostMapping("/render")
  public ResponseEntity<byte[]> render(@RequestBody RenderPayload p){
    try{
      String html = (p.html()==null || p.html().isBlank()) ? "<html><body></body></html>" : p.html();
      String base = (p.baseUrl()==null || p.baseUrl().isBlank()) ? null : p.baseUrl();
      if(p.inlineImages()!=null && !p.inlineImages().isEmpty()){
        for(var e : p.inlineImages().entrySet()){
          String id = e.getKey();
          String b64 = e.getValue();
          if(id==null || id.isBlank() || b64==null || b64.isBlank()) continue;
          html = html.replace("cid:"+id, "data:image/png;base64,"+b64);
        }
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PdfRendererBuilder b = new PdfRendererBuilder();
      b.useFastMode();
      b.withHtmlContent(html, base);
      b.toStream(baos);
      b.run();
      byte[] pdf = baos.toByteArray();
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=document.pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdf);
    }catch(Exception ex){
      return ResponseEntity.badRequest()
          .contentType(MediaType.TEXT_PLAIN)
          .body(("PDF render error: "+ex.getMessage()).getBytes(StandardCharsets.UTF_8));
    }
  }
}
