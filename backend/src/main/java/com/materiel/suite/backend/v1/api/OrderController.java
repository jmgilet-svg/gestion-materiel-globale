package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.*;
import com.materiel.suite.backend.v1.repo.OrderRepository;
import com.materiel.suite.backend.v1.service.ChangeFeedService;
import com.materiel.suite.backend.v1.service.IdempotencyService;
import com.materiel.suite.backend.v1.service.TotalsCalculator;
import com.materiel.suite.backend.v1.service.DocumentStateMachine;
import com.materiel.suite.backend.v1.service.NumberingService;
import com.materiel.suite.backend.v1.util.Etags;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderRepository repo;
  private final TotalsCalculator totals;
  private final IdempotencyService idem;
  private final ChangeFeedService changes;
  private final DocumentStateMachine sm = new DocumentStateMachine();
  private final NumberingService numbering;

  public OrderController(OrderRepository repo, TotalsCalculator totals, IdempotencyService idem, ChangeFeedService changes, NumberingService numbering){
    this.repo = repo; this.totals = totals; this.idem = idem; this.changes = changes; this.numbering = numbering;
  }

  @GetMapping
  public ResponseEntity<List<OrderEntity>> list(){ return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").body(repo.findAll()); }

  @GetMapping(params={"page","size"})
  public ResponseEntity<List<OrderEntity>> listPaged(@RequestParam int page, @RequestParam int size){
    var p = org.springframework.data.domain.PageRequest.of(Math.max(0,page), Math.min(200, Math.max(1,size)));
    var res = repo.findAll(p);
    return ResponseEntity.ok().header("X-Total-Count", String.valueOf(res.getTotalElements())).body(res.getContent());
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderEntity> get(@PathVariable UUID id){
    return repo.findById(id)
        .map(o -> ResponseEntity.ok().eTag(Etags.weakOfVersion(o.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(o))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<OrderEntity> create(@RequestHeader(value="Idempotency-Key", required=false) String idk,
                                            @RequestBody OrderEntity o){
    if (StringUtils.hasText(idk)){
      var ex = idem.findExisting("POST:/api/v1/orders", idk, OrderEntity.class);
      if (ex!=null) return ResponseEntity.ok().eTag(Etags.weakOfVersion(ex.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(ex);
    }
    if (o.getId()==null) o.setId(UUID.randomUUID());
    sanitize(o);
    totals.recomputeTotals(o);
    if (o.getNumber()==null || o.getNumber().isBlank()) o.setNumber(numbering.next("ORDER"));
    o.setVersion(o.getVersion()+1);
    var saved = repo.save(o);
    changes.emit("ORDER_CREATED", saved.getId().toString(), Map.of("number", saved.getNumber()));
    if (StringUtils.hasText(idk)) idem.remember("POST:/api/v1/orders", idk, saved);
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<OrderEntity> update(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @RequestBody OrderEntity in){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    cur.setCustomerName(in.getCustomerName()); cur.setNumber(in.getNumber()); if (in.getStatus()!=null) cur.setStatus(in.getStatus());
    cur.setLines(Optional.ofNullable(in.getLines()).orElseGet(ArrayList::new)); sanitize(cur);
    totals.recomputeTotals(cur); cur.setVersion(cur.getVersion()+1);
    var saved = repo.save(cur);
    changes.emit("ORDER_UPDATED", saved.getId().toString(), Map.of("version", saved.getVersion()));
    return ResponseEntity.ok().eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  /* ===== Transitions ===== */
  @PostMapping("/{id}:confirm")
  public ResponseEntity<OrderEntity> confirm(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.CONFIRM, "ORDER_CONFIRMED");
  }
  @PostMapping("/{id}:cancel")
  public ResponseEntity<OrderEntity> cancel(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.CANCEL, "ORDER_CANCELED");
  }
  @PostMapping("/{id}:lock")
  public ResponseEntity<OrderEntity> lock(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    return transition(id, ifMatch, DocumentStateMachine.Action.LOCK, "ORDER_LOCKED");
  }

  private ResponseEntity<OrderEntity> transition(UUID id, String ifMatch, DocumentStateMachine.Action action, String eventType){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    try {
      var next = sm.next(cur.getStatus(), action);
      cur.setStatus(next);
      cur.setVersion(cur.getVersion()+1);
      var saved = repo.save(cur);
      changes.emit(eventType, saved.getId().toString(), Map.of("status", next.name()));
      return ResponseEntity.ok().eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
    } catch(IllegalStateException ex){
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch){
    var cur = repo.findById(id).orElse(null);
    if (cur==null) return ResponseEntity.notFound().build();
    if (!Etags.matches(ifMatch, Etags.weakOfVersion(cur.getVersion()))) return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
    repo.delete(cur); changes.emit("ORDER_DELETED", id.toString(), Map.of()); return ResponseEntity.noContent().build();
  }

  @PostMapping("/from-quote")
  public ResponseEntity<OrderEntity> fromQuote(@RequestBody Map<String,Object> body){
    // Micro: créer une commande à partir d'un snapshot "quote" fourni
    Map<String,Object> q = (Map<String,Object>) body.getOrDefault("quote", Map.of());
    OrderEntity o = new OrderEntity();
    o.setId(UUID.randomUUID());
    o.setNumber((String) q.getOrDefault("number",""));
    o.setCustomerName((String) q.getOrDefault("customerName",""));
    List<Map<String,Object>> lines = (List<Map<String,Object>>) q.getOrDefault("lines", List.of());
    o.setLines(copyLines(lines));
    sanitize(o); totals.recomputeTotals(o); o.setVersion(1);
    var saved = repo.save(o);
    changes.emit("ORDER_CREATED_FROM_QUOTE", saved.getId().toString(), Map.of("quoteNumber", o.getNumber()));
    return ResponseEntity.status(HttpStatus.CREATED).eTag(Etags.weakOfVersion(saved.getVersion())).header(HttpHeaders.CACHE_CONTROL,"no-store").body(saved);
  }

  static List<DocLineEmb> copyLines(List<Map<String,Object>> src){
    List<DocLineEmb> out = new ArrayList<>();
    for (var m : src){
      DocLineEmb l = new DocLineEmb();
      l.setDesignation((String)m.get("designation"));
      l.setUnit((String)m.get("unit"));
      l.setQty(asDecimal(m.get("qty")));
      l.setUnitPrice(asDecimal(m.get("unitPrice")));
      l.setDiscountPct(asDecimal(m.get("discountPct")));
      l.setVatPct(asDecimal(m.get("vatPct")));
      out.add(l);
    }
    return out;
  }
  private static java.math.BigDecimal asDecimal(Object o){
    if (o==null) return java.math.BigDecimal.ZERO;
    if (o instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
    return new java.math.BigDecimal(o.toString());
  }
  private static void sanitize(OrderEntity o){
    if (o.getStatus()==null) o.setStatus(DocumentStatus.DRAFT);
    if (o.getLines()==null) o.setLines(new ArrayList<>());
  }
}

