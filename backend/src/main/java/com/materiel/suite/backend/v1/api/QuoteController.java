package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.DocLine;
import com.materiel.suite.backend.v1.domain.DocumentStatus;
import com.materiel.suite.backend.v1.domain.Quote;
import com.materiel.suite.backend.v1.service.ChangeFeedService;
import com.materiel.suite.backend.v1.service.IdempotencyService;
import com.materiel.suite.backend.v1.service.TotalsCalculator;
import com.materiel.suite.backend.v1.util.Etags;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {

  private final Map<UUID, Quote> store = new ConcurrentHashMap<>();
  private final TotalsCalculator totals = new TotalsCalculator();
  private final IdempotencyService idem;
  private final ChangeFeedService changes;

  public QuoteController(IdempotencyService idem, ChangeFeedService changes){
    this.idem = idem;
    this.changes = changes;
  }

  /* ===== LIST / GET ===== */
  @GetMapping
  public ResponseEntity<List<Quote>> list(
      @RequestParam(value = "q", required = false) String q,
      @RequestParam(value = "status", required = false) String status){
    var all = new ArrayList<>(store.values());
    if (StringUtils.hasText(q)){
      String qq = q.toLowerCase(Locale.ROOT);
      all.removeIf(qu -> (qu.getCustomerName()==null? "":qu.getCustomerName()).toLowerCase(Locale.ROOT).contains(qq) == false
          && (qu.getNumber()==null? "":qu.getNumber()).toLowerCase(Locale.ROOT).contains(qq) == false);
    }
    if (StringUtils.hasText(status)){
      DocumentStatus st = DocumentStatus.valueOf(status.toUpperCase(Locale.ROOT));
      all.removeIf(qu -> qu.getStatus()!=st);
    }
    all.sort(Comparator.comparing(Quote::getUpdatedAt).reversed());
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(all);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Quote> get(@PathVariable UUID id){
    Quote q = store.get(id);
    if (q==null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok()
        .eTag(Etags.weakOfVersion(q.getVersion()))
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(q);
  }

  /* ===== CREATE (POST) avec Idempotency-Key ===== */
  @PostMapping
  public ResponseEntity<Quote> create(@RequestHeader(value = "Idempotency-Key", required = false) String idk,
                                      @RequestBody Quote q){
    // Idempotency : si même clé + même route → renvoyer la ressource existante
    if (StringUtils.hasText(idk)){
      var existing = idem.findExisting("POST:/api/v1/quotes", idk, Quote.class);
      if (existing!=null){
        return ResponseEntity.status(HttpStatus.OK)
            .eTag(Etags.weakOfVersion(existing.getVersion()))
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(existing);
      }
    }
    if (q.getId()==null) q.setId(UUID.randomUUID());
    if (q.getStatus()==null) q.setStatus(DocumentStatus.DRAFT);
    if (q.getLines()==null) q.setLines(new ArrayList<>());
    sanitizeLines(q.getLines());
    totals.recomputeTotals(q);
    q.setVersion(q.getVersion()+1);
    q.setUpdatedAt(Instant.now());
    store.put(q.getId(), q);
    changes.emit("QUOTE_CREATED", q.getId().toString(), Map.of("number", q.getNumber()));
    if (StringUtils.hasText(idk)){
      idem.remember("POST:/api/v1/quotes", idk, q);
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .eTag(Etags.weakOfVersion(q.getVersion()))
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(q);
  }

  /* ===== UPDATE (PUT) avec If-Match ETag ===== */
  @PutMapping("/{id}")
  public ResponseEntity<Quote> update(@PathVariable UUID id,
                                      @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                      @RequestBody Quote q){
    Quote cur = store.get(id);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!StringUtils.hasText(ifMatch)) return ResponseEntity.status(428).build(); // Precondition Required
    String expected = Etags.weakOfVersion(cur.getVersion());
    if (!Etags.matches(ifMatch, expected)) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();

    // Update allowed fields
    cur.setCustomerName(q.getCustomerName());
    cur.setNumber(q.getNumber());
    if (q.getStatus()!=null) cur.setStatus(q.getStatus());
    List<DocLine> lines = (q.getLines()==null)? new ArrayList<>() : q.getLines();
    sanitizeLines(lines);
    cur.setLines(lines);
    totals.recomputeTotals(cur);
    cur.setVersion(cur.getVersion()+1);
    cur.setUpdatedAt(Instant.now());
    changes.emit("QUOTE_UPDATED", cur.getId().toString(), Map.of("version", cur.getVersion()));

    return ResponseEntity.ok()
        .eTag(Etags.weakOfVersion(cur.getVersion()))
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(cur);
  }

  /* ===== DELETE (If-Match) ===== */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id,
                                     @RequestHeader(value = "If-Match", required = false) String ifMatch){
    Quote cur = store.get(id);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!StringUtils.hasText(ifMatch)) return ResponseEntity.status(428).build();
    String expected = Etags.weakOfVersion(cur.getVersion());
    if (!Etags.matches(ifMatch, expected)) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    store.remove(id);
    changes.emit("QUOTE_DELETED", id.toString(), Map.of());
    return ResponseEntity.noContent().build();
  }

  /* ===== TRANSITIONS d'état ===== */
  @PostMapping("/{id}:send")
  public ResponseEntity<Quote> send(@PathVariable UUID id,
                                    @RequestHeader(value = "If-Match", required = false) String ifMatch){
    return changeStatus(id, ifMatch, DocumentStatus.SENT);
  }
  @PostMapping("/{id}:accept")
  public ResponseEntity<Quote> accept(@PathVariable UUID id,
                                      @RequestHeader(value = "If-Match", required = false) String ifMatch){
    return changeStatus(id, ifMatch, DocumentStatus.ACCEPTED);
  }
  @PostMapping("/{id}:refuse")
  public ResponseEntity<Quote> refuse(@PathVariable UUID id,
                                      @RequestHeader(value = "If-Match", required = false) String ifMatch){
    return changeStatus(id, ifMatch, DocumentStatus.REFUSED);
  }
  @PostMapping("/{id}:lock")
  public ResponseEntity<Quote> lock(@PathVariable UUID id,
                                    @RequestHeader(value = "If-Match", required = false) String ifMatch){
    return changeStatus(id, ifMatch, DocumentStatus.LOCKED);
  }

  private ResponseEntity<Quote> changeStatus(UUID id, String ifMatch, DocumentStatus target){
    Quote cur = store.get(id);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!StringUtils.hasText(ifMatch)) return ResponseEntity.status(428).build();
    String expected = Etags.weakOfVersion(cur.getVersion());
    if (!Etags.matches(ifMatch, expected)) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    if (!cur.getStatus().canTransitionTo(target)){
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    cur.setStatus(target);
    cur.setVersion(cur.getVersion()+1);
    cur.setUpdatedAt(Instant.now());
    changes.emit("QUOTE_STATUS", cur.getId().toString(), Map.of("status", target.name()));
    return ResponseEntity.ok()
        .eTag(Etags.weakOfVersion(cur.getVersion()))
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(cur);
  }

  private void sanitizeLines(List<DocLine> lines){
    for (DocLine l : lines){
      if (l.getQty()==null) l.setQty(BigDecimal.ZERO);
      if (l.getUnitPrice()==null) l.setUnitPrice(BigDecimal.ZERO);
      if (l.getDiscountPct()==null) l.setDiscountPct(BigDecimal.ZERO);
      if (l.getVatPct()==null) l.setVatPct(BigDecimal.ZERO);
    }
  }
}
