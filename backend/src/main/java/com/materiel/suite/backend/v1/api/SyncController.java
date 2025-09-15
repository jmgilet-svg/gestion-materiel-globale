package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.service.ChangeFeedService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {
  private final ChangeFeedService changes;
  public SyncController(ChangeFeedService changes){ this.changes = changes; }

  @GetMapping("/changes")
  public ResponseEntity<Map<String,Object>> changes(@RequestParam(name="since", required = false) Long since){
    var feed = changes.getSince(since==null? 0L : since);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(feed);
  }
}
