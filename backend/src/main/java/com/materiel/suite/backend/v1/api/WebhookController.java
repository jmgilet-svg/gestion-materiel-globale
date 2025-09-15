package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.service.ChangeFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
  private final ChangeFeedService changes;
  public WebhookController(ChangeFeedService changes){ this.changes = changes; }

  @PostMapping
  public ResponseEntity<Void> register(@RequestBody Map<String,String> body){
    String url = body.get("url");
    changes.registerWebhook(url);
    return ResponseEntity.noContent().build();
  }
  @DeleteMapping
  public ResponseEntity<Void> unregister(@RequestParam String url){
    changes.unregisterWebhook(url);
    return ResponseEntity.noContent().build();
  }
  @GetMapping
  public ResponseEntity<List<String>> list(){
    return ResponseEntity.ok(changes.listWebhooks());
  }
}
