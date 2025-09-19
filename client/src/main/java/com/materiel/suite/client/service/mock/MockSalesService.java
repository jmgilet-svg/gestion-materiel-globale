package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.service.SalesService;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Implémentation mock en mémoire pour la génération de devis v2. */
public class MockSalesService implements SalesService {
  private final AtomicInteger seq = new AtomicInteger(1);
  private final Map<String, QuoteV2> store = new ConcurrentHashMap<>();

  @Override public QuoteV2 createQuoteFromIntervention(Intervention intervention){
    BigDecimal total = BigDecimal.ZERO;
    List<BillingLine> lines = intervention == null ? List.of() : intervention.getBillingLines();
    for (BillingLine line : lines){
      if (line == null){
        continue;
      }
      BigDecimal amount = line.getTotalHt();
      if (amount == null){
        BigDecimal unit = line.getUnitPriceHt();
        BigDecimal qty = line.getQuantity();
        if (unit != null && qty != null){
          amount = unit.multiply(qty);
        }
      }
      if (amount != null){
        total = total.add(amount);
      }
    }
    QuoteV2 quote = new QuoteV2();
    String id = UUID.randomUUID().toString();
    quote.setId(id);
    quote.setReference(String.format("Q%s-%04d", Year.now(), seq.getAndIncrement()));
    quote.setStatus("DRAFT");
    quote.setTotalHt(total);
    quote.setTotalTtc(total);
    store.put(id, quote);
    return quote;
  }

  @Override public QuoteV2 getQuote(String id){
    return id == null ? null : store.get(id);
  }
}
