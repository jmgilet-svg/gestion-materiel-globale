package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.service.DeliveryNoteService;

import java.util.*;

public class MockDeliveryNoteService implements DeliveryNoteService {
  private final Map<UUID, DeliveryNote> store = new LinkedHashMap<>();

  @Override public List<DeliveryNote> list(){ return new ArrayList<>(store.values()); }
  @Override public DeliveryNote get(UUID id){ return store.get(id); }
  @Override public DeliveryNote save(DeliveryNote d){
    if (d.getId()==null) d.setId(UUID.randomUUID());
    store.put(d.getId(), d); return d;
  }
  @Override public void delete(UUID id){ store.remove(id); }

  @Override public DeliveryNote createFromOrder(UUID orderId){
    DeliveryNote d = new DeliveryNote();
    d.setId(UUID.randomUUID());
    d.setNumber("BL-" + String.format("%06d", store.size()+1));
    d.setCustomerName("Client depuis BC " + orderId.toString().substring(0,8));
    d.setLines(new ArrayList<>());
    save(d);
    return d;
  }
}

