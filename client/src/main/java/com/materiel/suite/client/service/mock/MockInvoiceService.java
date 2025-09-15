package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.service.InvoiceService;

import java.util.*;

public class MockInvoiceService implements InvoiceService {
  private final Map<UUID, Invoice> store = new LinkedHashMap<>();

  @Override public List<Invoice> list(){ return new ArrayList<>(store.values()); }
  @Override public Invoice get(UUID id){ return store.get(id); }
  @Override public Invoice save(Invoice i){
    if (i.getId()==null) i.setId(UUID.randomUUID());
    store.put(i.getId(), i); return i;
  }
  @Override public void delete(UUID id){ store.remove(id); }

  @Override public Invoice createFromQuote(UUID quoteId){
    Invoice i = new Invoice();
    i.setId(UUID.randomUUID());
    i.setNumber("FA-" + String.format("%06d", store.size()+1));
    i.setCustomerName("Client depuis devis " + quoteId.toString().substring(0,8));
    i.setLines(new ArrayList<>());
    save(i);
    return i;
  }

  @Override public Invoice createFromDeliveryNotes(List<UUID> deliveryNoteIds){
    Invoice i = new Invoice();
    i.setId(UUID.randomUUID());
    i.setNumber("FA-" + String.format("%06d", store.size()+1));
    i.setCustomerName("Client depuis " + deliveryNoteIds.size() + " BL");
    i.setLines(new ArrayList<>());
    save(i);
    return i;
  }
}

