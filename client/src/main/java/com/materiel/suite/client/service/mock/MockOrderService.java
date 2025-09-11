package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.service.OrderService;

import java.util.List;
import java.util.UUID;

public class MockOrderService implements OrderService {
  @Override public List<Order> list(){ return MockData.ORDERS; }
  @Override public Order get(UUID id){ return MockData.findById(MockData.ORDERS, id); }
  @Override public Order save(Order o){
    if (o.getId()==null) o.setId(UUID.randomUUID());
    if (o.getNumber()==null || o.getNumber().isBlank()){
      o.setNumber(MockData.nextNumber("CMD", new java.util.concurrent.atomic.AtomicInteger(MockData.ORDERS.size()+1)));
    }
    o.recomputeTotals();
    var ex = get(o.getId());
    if (ex==null) MockData.ORDERS.add(o); else MockData.ORDERS.replaceAll(x -> x.getId().equals(o.getId())?o:x);
    return o;
  }
  @Override public void delete(UUID id){ MockData.ORDERS.removeIf(o -> o.getId().equals(id)); }
  @Override public Order createFromQuote(UUID quoteId){
    Quote q = MockData.findById(MockData.QUOTES, quoteId);
    if (q==null) return null;
    Order o = MockData.fromQuote(q);
    MockData.ORDERS.add(o);
    return o;
  }
}
