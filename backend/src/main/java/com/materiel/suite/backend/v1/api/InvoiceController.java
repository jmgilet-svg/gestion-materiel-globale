package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.*;
import com.materiel.suite.backend.v1.repo.InvoiceRepository;
import com.materiel.suite.backend.v1.service.ChangeFeedService;
import com.materiel.suite.backend.v1.service.TotalsCalculator;
import com.materiel.suite.backend.v1.service.DocumentStateMachine;
import com.materiel.suite.backend.v1.service.IdempotencyService;
import com.materiel.suite.backend.v1.util.Etags;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.*;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {
  private final InvoiceRepository repo;
  private final TotalsCalculator totals;
  private final ChangeFeedService changes;
  private final DocumentStateMachine sm = new DocumentStateMachine();
  private final IdempotencyService idem;
  public InvoiceController(InvoiceRepository repo, TotalsCalculator totals, ChangeFeedService changes, IdempotencyService idem){
    this.repo = repo; this.totals = totals; this.changes = changes; this.idem = idem;
  }

  @GetMapping public ResponseEntity<List<InvoiceEntity>> list(){
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").body(repo.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<InvoiceEntity> get(@PathVariable UUID id){
    return repo.findById(id).map(i -> ResponseEntity.ok().eTag(Etags.weakOfVersion(i.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(i))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<InvoiceEntity> create(@RequestHeader(value="Idempotency-Key", required=false) String idk,
                                              @RequestBody InvoiceEntity i){
    if (StringUtils.hasText(idk)){
      var ex = idem.findExisting("POST:/api/v1/invoices", idk, InvoiceEntity.class);
      if (ex!=null) return ResponseEntity.ok().eTag(Etags.weakOfVersion(ex.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(ex);
    }
    if (i.getId()==null) i.setId(UUID.randomUUID());
    sanitize(i); totals.recomputeTotals(i); i.setVersion(1);
    var saved = repo.save(i);
    changes.emit("INVOICE_CREATED", saved.getId().toString(), Map.of("number", saved.getNumber()));
    if (StringUtils.hasText(idk)) idem.remember("POST:/api/v1/invoices", idk, saved);
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<InvoiceEntity> update(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @RequestBody InvoiceEntity in){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    cur.setCustomerName(in.getCustomerName()); cur.setNumber(in.getNumber()); if (in.getStatus()!=null) cur.setStatus(in.getStatus());
    cur.setLines(Optional.ofNullable(in.getLines()).orElseGet(ArrayList::new));
    totals.recomputeTotals(cur); cur.setVersion(cur.getVersion()+1);
    var saved = repo.save(cur);
    changes.emit("INVOICE_UPDATED", saved.getId().toString(), Map.of("version", saved.getVersion()));
    return ResponseEntity.ok().eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  /* ===== Transitions ===== */
  @PostMapping("/{id}:issue")
  public ResponseEntity<InvoiceEntity> issue(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.ISSUE, "INVOICE_ISSUED");
  }
  @PostMapping("/{id}:pay")
  public ResponseEntity<InvoiceEntity> pay(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.PAY, "INVOICE_PAID");
  }
  @PostMapping("/{id}:cancel")
  public ResponseEntity<InvoiceEntity> cancel(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.CANCEL, "INVOICE_CANCELED");
  }

  private ResponseEntity<InvoiceEntity> transition(UUID id, String ifMatch, DocumentStateMachine.Action action, String evt){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    try {
      var next = sm.next(cur.getStatus(), action);
      cur.setStatus(next); cur.setVersion(cur.getVersion()+1);
      var saved = repo.save(cur);
      changes.emit(evt, saved.getId().toString(), Map.of("status", next.name()));
      return ResponseEntity.ok().eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
    } catch(IllegalStateException ex){ return ResponseEntity.status(HttpStatus.CONFLICT).build(); }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    repo.delete(cur); changes.emit("INVOICE_DELETED", id.toString(), Map.of()); return ResponseEntity.noContent().build();
  }

  @PostMapping("/from-delivery-notes")
  public ResponseEntity<InvoiceEntity> fromDeliveryNotes(@RequestBody Map<String,Object> body){
    List<Map<String,Object>> dels = (List<Map<String,Object>>) body.getOrDefault("deliveryNotes", List.of());
    InvoiceEntity i = new InvoiceEntity();
    i.setId(UUID.randomUUID());
    if (!dels.isEmpty()){
      Map<String,Object> first = dels.get(0);
      i.setCustomerName((String) first.getOrDefault("customerName",""));
    }
    List<DocLineEmb> all = new ArrayList<>();
    for (var d : dels){
      List<Map<String,Object>> lines = (List<Map<String,Object>>) d.getOrDefault("lines", List.of());
      all.addAll(OrderController.copyLines(lines));
    }
    i.setLines(all);
    sanitize(i); totals.recomputeTotals(i); i.setVersion(1);
    var saved = repo.save(i);
    changes.emit("INVOICE_CREATED_FROM_DN", saved.getId().toString(), Map.of("count", dels.size()));
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  @PostMapping("/from-quote")
  public ResponseEntity<InvoiceEntity> fromQuote(@RequestBody Map<String,Object> body){
    Map<String,Object> q = (Map<String,Object>) body.getOrDefault("quote", Map.of());
    InvoiceEntity i = new InvoiceEntity();
    i.setId(UUID.randomUUID());
    i.setNumber((String) q.getOrDefault("number",""));
    i.setCustomerName((String) q.getOrDefault("customerName",""));
    List<Map<String,Object>> lines = (List<Map<String,Object>>) q.getOrDefault("lines", List.of());
    i.setLines(OrderController.copyLines(lines));
    sanitize(i); totals.recomputeTotals(i); i.setVersion(1);
    var saved = repo.save(i);
    changes.emit("INVOICE_CREATED_FROM_QUOTE", saved.getId().toString(), Map.of("quoteNumber", i.getNumber()));
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  private static void sanitize(InvoiceEntity i){
    if (i.getStatus()==null) i.setStatus(DocumentStatus.DRAFT);
    if (i.getLines()==null) i.setLines(new ArrayList<>());
  }
}

