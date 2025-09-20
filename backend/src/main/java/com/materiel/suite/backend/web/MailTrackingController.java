package com.materiel.suite.backend.web;

import com.materiel.suite.backend.intervention.TimelineEventStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Base64;

@RestController
public class MailTrackingController {
  private final TimelineEventStore timelineStore;

  public MailTrackingController(TimelineEventStore timelineStore){
    this.timelineStore = timelineStore;
  }

  @GetMapping(value = "/api/v2/mail/open", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> open(
      @RequestParam(name = "ids", required = false) String idsCsv,
      @RequestParam(name = "to", required = false) String to
  ){
    if (idsCsv != null && !idsCsv.isBlank()){
      String actor = to == null || to.isBlank() ? "destinataire inconnu" : to;
      for (String raw : idsCsv.split(",")){
        String id = raw.trim();
        if (id.isEmpty()){
          continue;
        }
        timelineStore.appendAction(id, "Ouverture email par " + actor, actor, Instant.now());
      }
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .body(PIXEL);
  }

  private static final byte[] PIXEL = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAA" +
      "AAC0lEQVR42mP8/x8AAwMB/axv7j8AAAAASUVORK5CYII=");
}
