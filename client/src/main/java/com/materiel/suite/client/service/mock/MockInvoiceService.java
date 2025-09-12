package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.service.InvoiceService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockInvoiceService implements InvoiceService {
  @Override public List<Invoice> list(){ return MockData.INVOICES; }
  @Override public Invoice get(UUID id){ return MockData.findById(MockData.INVOICES, id); }
  @Override public Invoice save(Invoice i){
    if (i.getId()==null) i.setId(UUID.randomUUID());
    if (i.getNumber()==null || i.getNumber().isBlank()){
      i.setNumber(MockData.nextNumber("FAC", new java.util.concurrent.atomic.AtomicInteger(MockData.INVOICES.size()+1)));
    }
    i.recomputeTotals();
    var ex = get(i.getId());
    if (ex==null) MockData.INVOICES.add(i); else MockData.INVOICES.replaceAll(x -> x.getId().equals(i.getId())?i:x);
    return i;
  }
  @Override public void delete(UUID id){ MockData.INVOICES.removeIf(o -> o.getId().equals(id)); }
  @Override public Invoice createFromQuote(UUID quoteId){
    Quote q = MockData.findById(MockData.QUOTES, quoteId);
    if (q==null) return null;
    Invoice inv = MockData.fromQuoteToInvoice(q);
    MockData.INVOICES.add(inv);
    return inv;
  }
  @Override public Invoice createFromDeliveryNotes(List<UUID> deliveryNoteIds){
    List<DeliveryNote> dns = new ArrayList<>();
    for (UUID id : deliveryNoteIds){
      DeliveryNote d = MockData.findById(MockData.DELIVERY_NOTES, id);
      if (d!=null) dns.add(d);
    }
    Invoice i = MockData.fromDeliveryNotes(dns);
    MockData.INVOICES.add(i);
    return i;
  }
}
