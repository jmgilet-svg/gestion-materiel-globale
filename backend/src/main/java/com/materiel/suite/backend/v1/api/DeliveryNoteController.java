package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.*;
import com.materiel.suite.backend.v1.repo.DeliveryNoteRepository;
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
@RequestMapping("/api/v1/delivery-notes")
public class DeliveryNoteController {
  private final DeliveryNoteRepository repo;
  private final TotalsCalculator totals;
  private final ChangeFeedService changes;
  private final DocumentStateMachine sm = new DocumentStateMachine();
  private final IdempotencyService idem;
  public DeliveryNoteController(DeliveryNoteRepository repo, TotalsCalculator totals, ChangeFeedService changes, IdempotencyService idem){
    this.repo = repo; this.totals = totals; this.changes = changes; this.idem = idem;
  }

  @GetMapping public ResponseEntity<List<DeliveryNoteEntity>> list(){
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").body(repo.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<DeliveryNoteEntity> get(@PathVariable UUID id){
    return repo.findById(id).map(d -> ResponseEntity.ok().eTag(Etags.weakOfVersion(d.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(d))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<DeliveryNoteEntity> create(@RequestHeader(value="Idempotency-Key", required=false) String idk,
                                                   @RequestBody DeliveryNoteEntity d){
    if (StringUtils.hasText(idk)){
      var ex = idem.findExisting("POST:/api/v1/delivery-notes", idk, DeliveryNoteEntity.class);
      if (ex!=null) return ResponseEntity.ok().eTag(Etags.weakOfVersion(ex.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(ex);
    }
    if (d.getId()==null) d.setId(UUID.randomUUID());
    sanitize(d); totals.recomputeTotals(d); d.setVersion(1);
    var saved = repo.save(d);
    changes.emit("DELIVERY_CREATED", saved.getId().toString(), Map.of("number", saved.getNumber()));
    if (StringUtils.hasText(idk)) idem.remember("POST:/api/v1/delivery-notes", idk, saved);
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<DeliveryNoteEntity> update(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @RequestBody DeliveryNoteEntity in){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    cur.setCustomerName(in.getCustomerName()); cur.setNumber(in.getNumber()); if (in.getStatus()!=null) cur.setStatus(in.getStatus());
    cur.setLines(Optional.ofNullable(in.getLines()).orElseGet(ArrayList::new));
    totals.recomputeTotals(cur); cur.setVersion(cur.getVersion()+1);
    var saved = repo.save(cur);
    changes.emit("DELIVERY_UPDATED", saved.getId().toString(), Map.of("version", saved.getVersion()));
    return ResponseEntity.ok().eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  /* ===== Transitions ===== */
  @PostMapping("/{id}:deliver")
  public ResponseEntity<DeliveryNoteEntity> deliver(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.DELIVER, "DELIVERY_DELIVERED");
  }
  @PostMapping("/{id}:cancel")
  public ResponseEntity<DeliveryNoteEntity> cancel(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.CANCEL, "DELIVERY_CANCELED");
  }
  @PostMapping("/{id}:lock")
  public ResponseEntity<DeliveryNoteEntity> lock(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.LOCK, "DELIVERY_LOCKED");
  }

  private ResponseEntity<DeliveryNoteEntity> transition(UUID id, String ifMatch, DocumentStateMachine.Action action, String evt){
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
    repo.delete(cur); changes.emit("DELIVERY_DELETED", id.toString(), Map.of()); return ResponseEntity.noContent().build();
  }

  @PostMapping("/from-order")
  public ResponseEntity<DeliveryNoteEntity> fromOrder(@RequestBody Map<String,Object> body){
    Map<String,Object> o = (Map<String,Object>) body.getOrDefault("order", Map.of());
    DeliveryNoteEntity d = new DeliveryNoteEntity();
    d.setId(UUID.randomUUID());
    d.setNumber((String)o.getOrDefault("number",""));
    d.setCustomerName((String)o.getOrDefault("customerName",""));
    List<Map<String,Object>> lines = (List<Map<String,Object>>) o.getOrDefault("lines", List.of());
    d.setLines(OrderController.copyLines(lines));
    sanitize(d); totals.recomputeTotals(d); d.setVersion(1);
    var saved = repo.save(d);
    changes.emit("DELIVERY_CREATED_FROM_ORDER", saved.getId().toString(), Map.of("orderNumber", d.getNumber()));
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  private static void sanitize(DeliveryNoteEntity d){
    if (d.getStatus()==null) d.setStatus(DocumentStatus.DRAFT);
    if (d.getLines()==null) d.setLines(new ArrayList<>());
  }
}

