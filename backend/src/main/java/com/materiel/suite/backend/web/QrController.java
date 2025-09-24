package com.materiel.suite.backend.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

/** Endpoint minimaliste pour générer des QR codes PNG. */
@RestController
@RequestMapping("/api/v2/qr")
public class QrController {

  @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> qr(@RequestParam("text") String text,
                                   @RequestParam(value = "size", defaultValue = "256") int size) throws Exception {
    String payload = text == null ? "" : text;
    int effectiveSize = Math.max(64, Math.min(size, 1024));
    BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, effectiveSize, effectiveSize);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(matrix, "PNG", out);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(out.toByteArray());
  }
}
