package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class MockData {
  private MockData(){}
  public static final List<Quote> QUOTES = new ArrayList<>();
  public static final List<Order> ORDERS = new ArrayList<>();
  public static final List<DeliveryNote> DELIVERY_NOTES = new ArrayList<>();
  public static final List<Invoice> INVOICES = new ArrayList<>();
  private static final AtomicInteger seqQuote = new AtomicInteger(1);
  private static final AtomicInteger seqOrder = new AtomicInteger(1);
  private static final AtomicInteger seqDN = new AtomicInteger(1);
  private static final AtomicInteger seqInv = new AtomicInteger(1);

  public static void seedIfEmpty(){
    if (!QUOTES.isEmpty()) return;
    var q1 = new Quote(UUID.randomUUID(), nextNumber("DEV", seqQuote), LocalDate.now().minusDays(2), "Société Alpha", "Brouillon");
    q1.getLines().add(new DocumentLine("Location grue", 1, "j", 520, 0, 20));
    q1.getLines().add(new DocumentLine("Transport", 1, "forfait", 120, 0, 20));
    q1.recomputeTotals();
    QUOTES.add(q1);

    var q2 = new Quote(UUID.randomUUID(), nextNumber("DEV", seqQuote), LocalDate.now().minusDays(1), "BTP Béton", "Envoyé");
    q2.getLines().add(new DocumentLine("Levage charpente", 1, "forfait", 1800, 5, 20));
    q2.recomputeTotals();
    QUOTES.add(q2);
  }

  static String nextNumber(String prefix, AtomicInteger seq){
    int n = seq.getAndIncrement();
    return "%s-%d-%05d".formatted(prefix, LocalDate.now().getYear(), n);
  }

  public static <T> T findById(List<T> list, UUID id){
    for (T t : list){
      if (t instanceof Quote q && q.getId().equals(id)) return (T) q;
      if (t instanceof Order o && o.getId().equals(id)) return (T) o;
      if (t instanceof DeliveryNote d && d.getId().equals(id)) return (T) d;
      if (t instanceof Invoice i && i.getId().equals(id)) return (T) i;
    }
    return null;
  }

  public static Order fromQuote(Quote q){
    var o = new Order();
    o.setId(UUID.randomUUID());
    o.setNumber(nextNumber("CMD", seqOrder));
    o.setDate(LocalDate.now());
    o.setCustomerName(q.getCustomerName());
    o.setStatus("Brouillon");
    q.getLines().forEach(l -> o.getLines().add(copy(l)));
    o.recomputeTotals();
    return o;
  }
  public static DeliveryNote fromOrder(Order o){
    var d = new DeliveryNote();
    d.setId(UUID.randomUUID());
    d.setNumber(nextNumber("BL", seqDN));
    d.setDate(LocalDate.now());
    d.setCustomerName(o.getCustomerName());
    d.setStatus("Brouillon");
    o.getLines().forEach(l -> d.getLines().add(copy(l)));
    d.recomputeTotals();
    return d;
  }
  public static Invoice fromQuoteToInvoice(Quote q){
    var i = new Invoice();
    i.setId(UUID.randomUUID());
    i.setNumber(nextNumber("FAC", seqInv));
    i.setDate(LocalDate.now());
    i.setCustomerName(q.getCustomerName());
    i.setStatus("Brouillon");
    q.getLines().forEach(l -> i.getLines().add(copy(l)));
    i.recomputeTotals();
    return i;
  }
  public static Invoice fromDeliveryNotes(List<DeliveryNote> dns){
    var i = new Invoice();
    i.setId(UUID.randomUUID());
    i.setNumber(nextNumber("FAC", seqInv));
    i.setDate(LocalDate.now());
    i.setCustomerName(dns.isEmpty() ? "" : dns.get(0).getCustomerName());
    i.setStatus("Brouillon");
    for (var d : dns) d.getLines().forEach(l -> i.getLines().add(copy(l)));
    i.recomputeTotals();
    return i;
  }

  private static DocumentLine copy(DocumentLine l){
    return new DocumentLine(l.getDesignation(), l.getQuantite(), l.getUnite(), l.getPrixUnitaireHT(), l.getRemisePct(), l.getTvaPct());
  }
}
