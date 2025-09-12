package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Invoice;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {
  List<Invoice> list();
  Invoice get(UUID id);
  Invoice save(Invoice inv);
  void delete(UUID id);
  Invoice createFromQuote(UUID quoteId);
  Invoice createFromDeliveryNotes(List<UUID> deliveryNoteIds);
}
