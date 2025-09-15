package com.materiel.suite.backend.v1.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class OpenApiController {

  @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
  public ResponseEntity<byte[]> openapiYaml() throws IOException {
    var res = new ClassPathResource("openapi/openapi-v1.yaml");
    byte[] bytes = res.getInputStream().readAllBytes();
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(MediaType.parseMediaType("application/yaml"))
        .body(bytes);
  }
}
