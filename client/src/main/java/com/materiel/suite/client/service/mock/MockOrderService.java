package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.service.OrderService;

import java.util.*;

public class MockOrderService implements OrderService {
  private final Map<UUID, Order> store = new LinkedHashMap<>();

  @Override public List<Order> list(){ return new ArrayList<>(store.values()); }
  @Override public Order get(UUID id){ return store.get(id); }
  @Override public Order save(Order o){
    if (o.getId()==null) o.setId(UUID.randomUUID());
    store.put(o.getId(), o); return o;
  }
  @Override public void delete(UUID id){ store.remove(id); }

  @Override public Order createFromQuote(UUID quoteId){
    // Mock: duplique une commande depuis un "quote" présent déjà en Order pour demo
    Order o = new Order();
    o.setId(UUID.randomUUID());
    o.setNumber("BC-" + String.format("%06d", store.size()+1));
    o.setCustomerName("Client depuis devis " + quoteId.toString().substring(0,8));
    o.setLines(new ArrayList<>());
    save(o);
    return o;
  }
}

