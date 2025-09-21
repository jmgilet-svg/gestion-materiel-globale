package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.service.SalesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Implémentation mock en mémoire pour la génération et la gestion de ventes v2. */
public class MockSalesService implements SalesService {
  private final AtomicInteger quoteSeq = new AtomicInteger(1);
  private final AtomicInteger invoiceSeq = new AtomicInteger(1);
  private final Map<String, QuoteV2> quotes = new ConcurrentHashMap<>();
  private final Map<String, InvoiceV2> invoices = new ConcurrentHashMap<>();

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
    quote.setReference(String.format("Q%s-%04d", Year.now(), quoteSeq.getAndIncrement()));
    quote.setStatus("DRAFT");
    quote.setDate(LocalDate.now());
    quote.setTotalHt(total);
    quote.setTotalTtc(total);
    quote.setSent(Boolean.FALSE);
    if (intervention != null){
      quote.setClientId(intervention.getClientId() == null ? null : intervention.getClientId().toString());
      quote.setClientName(intervention.getClientName());
      quote.setAgencyId(intervention.getAgencyId());
    }
    quotes.put(id, copyQuote(quote));
    return copyQuote(quote);
  }

  @Override public QuoteV2 getQuote(String id){
    if (id == null){
      return null;
    }
    QuoteV2 quote = quotes.get(id);
    return quote == null ? null : copyQuote(quote);
  }

  @Override public List<QuoteV2> listQuotes(){
    return quotes.values().stream()
        .sorted(Comparator.comparing(QuoteV2::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .map(this::copyQuote)
        .toList();
  }

  @Override public QuoteV2 saveQuote(QuoteV2 quote){
    if (quote == null){
      return null;
    }
    QuoteV2 copy = copyQuote(quote);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    if (copy.getReference() == null || copy.getReference().isBlank()){
      copy.setReference(String.format("Q%s-%04d", Year.now(), quoteSeq.getAndIncrement()));
    }
    if (copy.getDate() == null){
      copy.setDate(LocalDate.now());
    }
    quotes.put(copy.getId(), copyQuote(copy));
    return copyQuote(copy);
  }

  @Override public void deleteQuote(String id){
    if (id == null){
      return;
    }
    quotes.remove(id);
  }

  @Override public List<InvoiceV2> listInvoices(){
    return invoices.values().stream()
        .sorted(Comparator.comparing(InvoiceV2::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .map(this::copyInvoice)
        .toList();
  }

  @Override public InvoiceV2 saveInvoice(InvoiceV2 invoice){
    if (invoice == null){
      return null;
    }
    InvoiceV2 copy = copyInvoice(invoice);
    if (copy.getId() == null || copy.getId().isBlank()){
      copy.setId(UUID.randomUUID().toString());
    }
    if (copy.getNumber() == null || copy.getNumber().isBlank()){
      copy.setNumber(String.format("F%s-%04d", Year.now(), invoiceSeq.getAndIncrement()));
    }
    if (copy.getDate() == null){
      copy.setDate(LocalDate.now());
    }
    invoices.put(copy.getId(), copyInvoice(copy));
    return copyInvoice(copy);
  }

  @Override public void deleteInvoice(String id){
    if (id == null){
      return;
    }
    invoices.remove(id);
  }

  private QuoteV2 copyQuote(QuoteV2 src){
    QuoteV2 copy = new QuoteV2();
    copy.setId(src.getId());
    copy.setReference(src.getReference());
    copy.setClientId(src.getClientId());
    copy.setClientName(src.getClientName());
    copy.setDate(src.getDate());
    copy.setStatus(src.getStatus());
    copy.setTotalHt(src.getTotalHt());
    copy.setTotalTtc(src.getTotalTtc());
    copy.setSent(src.getSent());
    copy.setAgencyId(src.getAgencyId());
    copy.setLines(src.getLines());
    return copy;
  }

  private InvoiceV2 copyInvoice(InvoiceV2 src){
    InvoiceV2 copy = new InvoiceV2();
    copy.setId(src.getId());
    copy.setNumber(src.getNumber());
    copy.setClientId(src.getClientId());
    copy.setClientName(src.getClientName());
    copy.setDate(src.getDate());
    copy.setTotalHt(src.getTotalHt());
    copy.setTotalTtc(src.getTotalTtc());
    copy.setStatus(src.getStatus());
    copy.setAgencyId(src.getAgencyId());
    copy.setLines(src.getLines());
    return copy;
  }
}
