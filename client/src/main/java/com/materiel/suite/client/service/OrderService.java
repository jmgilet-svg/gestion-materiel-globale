package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {
  List<Order> list();
  Order get(UUID id);
  Order save(Order o);
  void delete(UUID id);
  Order createFromQuote(UUID quoteId);
}
