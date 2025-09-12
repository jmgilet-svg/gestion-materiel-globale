package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.DeliveryNoteService;

import java.util.List;
import java.util.UUID;

public class ApiDeliveryNoteService implements DeliveryNoteService {
  private final RestClient rc; private final DeliveryNoteService fb;
  public ApiDeliveryNoteService(RestClient rc, DeliveryNoteService fb){ this.rc=rc; this.fb=fb; }
  @Override public List<DeliveryNote> list(){ try { return fb.list(); } catch(Exception e){ return fb.list(); } }
  @Override public DeliveryNote get(UUID id){ try { return fb.get(id); } catch(Exception e){ return fb.get(id); } }
  @Override public DeliveryNote save(DeliveryNote d){ try { return fb.save(d); } catch(Exception e){ return fb.save(d); } }
  @Override public void delete(UUID id){ try { rc.delete("/api/delivery-notes/"+id); } catch(Exception ignore){} fb.delete(id); }
  @Override public DeliveryNote createFromOrder(UUID orderId){ try { return fb.createFromOrder(orderId); } catch(Exception e){ return fb.createFromOrder(orderId); } }
}
