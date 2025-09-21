package com.materiel.suite.backend.sales;

import com.materiel.suite.backend.sales.dto.BillingLineV2Dto;
import com.materiel.suite.backend.sales.dto.CreateQuoteFromInterventionV2Request;
import com.materiel.suite.backend.sales.dto.InterventionV2Dto;
import com.materiel.suite.backend.sales.dto.QuoteV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
public class SalesV2Controller {

  @PostMapping("/api/v2/quotes/from-intervention")
  public ResponseEntity<QuoteV2Dto> createFromIntervention(@RequestBody CreateQuoteFromInterventionV2Request body){
    if (body == null || body.getIntervention() == null){
      return ResponseEntity.badRequest().build();
    }
    InterventionV2Dto itv = body.getIntervention();
    BigDecimal total = BigDecimal.ZERO;
    if (itv.getBillingLines() != null){
      for (BillingLineV2Dto bl : itv.getBillingLines()){
        if (bl == null){
          continue;
        }
        BigDecimal line = bl.getTotalHt();
        if (line == null){
          BigDecimal unit = bl.getUnitPriceHt();
          BigDecimal qty = bl.getQuantity();
          if (unit != null && qty != null){
            line = unit.multiply(qty);
          }
        }
        if (line != null){
          total = total.add(line);
        }
      }
    }
    QuoteV2Dto out = new QuoteV2Dto();
    String id = UUID.randomUUID().toString();
    out.setId(id);
    out.setReference(SalesMemoryStore.nextQuoteReference());
    out.setStatus("DRAFT");
    out.setDate(LocalDate.now());
    out.setTotalHt(total);
    out.setTotalTtc(total); // TVA ignorée en v2 mock
    out.setClientId(itv.getClientId());
    out.setClientName(itv.getTitle());
    out.setAgencyId(itv.getAgencyId());
    out.setSent(Boolean.FALSE);
    SalesMemoryStore.putQuote(out);
    return ResponseEntity.ok(out);
  }

  @GetMapping("/api/v2/quotes/{id}")
  public ResponseEntity<QuoteV2Dto> getById(@PathVariable String id){
    QuoteV2Dto q = SalesMemoryStore.getQuote(id);
    return q == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(q);
  }
}
