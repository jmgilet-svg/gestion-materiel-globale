package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.service.OrderService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockOrderService implements OrderService {
  @Override public List<Order> list(){ return new ArrayList<>(MockData.ORDERS); }
  @Override public Order get(UUID id){ return MockData.findById(MockData.ORDERS, id); }
  @Override public Order save(Order o){
    if (o.getId()==null) o.setId(UUID.randomUUID());
    if (o.getNumber()==null || o.getNumber().isBlank()){
      o.setNumber(MockData.nextOrderNumber());
    }
    o.recomputeTotals();
    replaceOrAdd(o);
    return o;
  }
  @Override public void delete(UUID id){ MockData.ORDERS.removeIf(o -> o.getId().equals(id)); }

  @Override public Order createFromQuote(UUID quoteId){
    Quote q = MockData.findById(MockData.QUOTES, quoteId);
    if (q==null) return null;
    return save(MockData.fromQuote(q));
  }

  private void replaceOrAdd(Order o){
    for (int i=0; i<MockData.ORDERS.size(); i++){
      if (MockData.ORDERS.get(i).getId().equals(o.getId())){
        MockData.ORDERS.set(i, o);
        return;
      }
    }
    MockData.ORDERS.add(o);
  }
}

