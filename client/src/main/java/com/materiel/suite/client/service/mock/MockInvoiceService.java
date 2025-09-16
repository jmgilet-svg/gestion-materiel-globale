package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.service.InvoiceService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockInvoiceService implements InvoiceService {
  @Override public List<Invoice> list(){ return new ArrayList<>(MockData.INVOICES); }
  @Override public Invoice get(UUID id){ return MockData.findById(MockData.INVOICES, id); }
  @Override public Invoice save(Invoice i){
    if (i.getId()==null) i.setId(UUID.randomUUID());
    if (i.getNumber()==null || i.getNumber().isBlank()){
      i.setNumber(MockData.nextInvoiceNumber());
    }
    i.recomputeTotals();
    replaceOrAdd(i);
    return i;
  }
  @Override public void delete(UUID id){ MockData.INVOICES.removeIf(inv -> inv.getId().equals(id)); }

  @Override public Invoice createFromQuote(UUID quoteId){
    Quote q = MockData.findById(MockData.QUOTES, quoteId);
    if (q==null) return null;
    return save(MockData.fromQuoteToInvoice(q));
  }

  @Override public Invoice createFromDeliveryNotes(List<UUID> deliveryNoteIds){
    List<DeliveryNote> dns = new ArrayList<>();
    for (UUID id : deliveryNoteIds){
      DeliveryNote dn = MockData.findById(MockData.DELIVERY_NOTES, id);
      if (dn!=null) dns.add(dn);
    }
    if (dns.isEmpty()) return null;
    return save(MockData.fromDeliveryNotes(dns));
  }

  private void replaceOrAdd(Invoice inv){
    for (int i=0; i<MockData.INVOICES.size(); i++){
      if (MockData.INVOICES.get(i).getId().equals(inv.getId())){
        MockData.INVOICES.set(i, inv);
        return;
      }
    }
    MockData.INVOICES.add(inv);
  }
}

