package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.InvoiceService;

import java.util.List;
import java.util.UUID;

public class ApiInvoiceService implements InvoiceService {
  private final RestClient rc; private final InvoiceService fb;
  public ApiInvoiceService(RestClient rc, InvoiceService fb){ this.rc=rc; this.fb=fb; }
  @Override public List<Invoice> list(){ try { return fb.list(); } catch(Exception e){ return fb.list(); } }
  @Override public Invoice get(UUID id){ try { return fb.get(id); } catch(Exception e){ return fb.get(id); } }
  @Override public Invoice save(Invoice i){ try { return fb.save(i); } catch(Exception e){ return fb.save(i); } }
  @Override public void delete(UUID id){ try { rc.delete("/api/invoices/"+id); } catch(Exception ignore){} fb.delete(id); }
  @Override public Invoice createFromQuote(UUID quoteId){ try { return fb.createFromQuote(quoteId); } catch(Exception e){ return fb.createFromQuote(quoteId); } }
  @Override public Invoice createFromDeliveryNotes(List<UUID> ids){ try { return fb.createFromDeliveryNotes(ids); } catch(Exception e){ return fb.createFromDeliveryNotes(ids); } }
}
