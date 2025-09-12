package com.materiel.suite.backend.web;

import com.materiel.suite.backend.model.Quote;
import com.materiel.suite.backend.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {
    private final QuoteService service;

    public QuoteController(QuoteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Quote> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> get(@PathVariable UUID id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Quote> create(@RequestBody Quote quote) {
        Quote saved = service.create(quote);
        return ResponseEntity.created(URI.create("/api/v1/quotes/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Quote> update(@PathVariable UUID id, @RequestBody Quote quote) {
        if (!id.equals(quote.getId())) {
            return ResponseEntity.badRequest().build();
        }
        Quote saved = service.update(quote);
        return ResponseEntity.ok(saved);
    }
}
