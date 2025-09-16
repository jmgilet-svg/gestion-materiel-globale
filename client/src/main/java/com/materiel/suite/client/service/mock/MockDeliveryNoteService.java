package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.service.DeliveryNoteService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockDeliveryNoteService implements DeliveryNoteService {
  @Override public List<DeliveryNote> list(){ return new ArrayList<>(MockData.DELIVERY_NOTES); }
  @Override public DeliveryNote get(UUID id){ return MockData.findById(MockData.DELIVERY_NOTES, id); }
  @Override public DeliveryNote save(DeliveryNote d){
    if (d.getId()==null) d.setId(UUID.randomUUID());
    if (d.getNumber()==null || d.getNumber().isBlank()){
      d.setNumber(MockData.nextDeliveryNumber());
    }
    d.recomputeTotals();
    replaceOrAdd(d);
    return d;
  }
  @Override public void delete(UUID id){ MockData.DELIVERY_NOTES.removeIf(d -> d.getId().equals(id)); }

  @Override public DeliveryNote createFromOrder(UUID orderId){
    Order o = MockData.findById(MockData.ORDERS, orderId);
    if (o==null) return null;
    return save(MockData.fromOrder(o));
  }

  private void replaceOrAdd(DeliveryNote d){
    for (int i=0; i<MockData.DELIVERY_NOTES.size(); i++){
      if (MockData.DELIVERY_NOTES.get(i).getId().equals(d.getId())){
        MockData.DELIVERY_NOTES.set(i, d);
        return;
      }
    }
    MockData.DELIVERY_NOTES.add(d);
  }
}

