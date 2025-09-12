package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.service.DeliveryNoteService;

import java.util.List;
import java.util.UUID;

public class MockDeliveryNoteService implements DeliveryNoteService {
  @Override public List<DeliveryNote> list(){ return MockData.DELIVERY_NOTES; }
  @Override public DeliveryNote get(UUID id){ return MockData.findById(MockData.DELIVERY_NOTES, id); }
  @Override public DeliveryNote save(DeliveryNote d){
    if (d.getId()==null) d.setId(UUID.randomUUID());
    if (d.getNumber()==null || d.getNumber().isBlank()){
      d.setNumber(MockData.nextNumber("BL", new java.util.concurrent.atomic.AtomicInteger(MockData.DELIVERY_NOTES.size()+1)));
    }
    d.recomputeTotals();
    var ex = get(d.getId());
    if (ex==null) MockData.DELIVERY_NOTES.add(d); else MockData.DELIVERY_NOTES.replaceAll(x -> x.getId().equals(d.getId())?d:x);
    return d;
  }
  @Override public void delete(UUID id){ MockData.DELIVERY_NOTES.removeIf(o -> o.getId().equals(id)); }
  @Override public DeliveryNote createFromOrder(UUID orderId){
    Order o = MockData.findById(MockData.ORDERS, orderId);
    if (o==null) return null;
    DeliveryNote d = MockData.fromOrder(o);
    MockData.DELIVERY_NOTES.add(d);
    return d;
  }
}
