package com.materiel.suite.backend;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Serves the vendored OpenAPI definition without requiring springdoc. */
@RestController
public class OpenApiStaticController {

    @GetMapping(value = "/v3/api-docs.yaml", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> yaml() throws IOException {
        ClassPathResource res = new ClassPathResource("openapi/gestion-materiel-v1.yaml");
        byte[] bytes = res.getInputStream().readAllBytes();
        return ResponseEntity.ok(new String(bytes, StandardCharsets.UTF_8));
    }
}
