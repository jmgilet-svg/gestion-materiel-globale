package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.OrderService;

import java.util.List;
import java.util.UUID;

public class ApiOrderService implements OrderService {
  private final RestClient rc; private final OrderService fb;
  public ApiOrderService(RestClient rc, OrderService fb){ this.rc=rc; this.fb=fb; }
  @Override public List<Order> list(){ try { return fb.list(); } catch(Exception e){ return fb.list(); } }
  @Override public Order get(UUID id){ try { return fb.get(id); } catch(Exception e){ return fb.get(id); } }
  @Override public Order save(Order o){ try { return fb.save(o); } catch(Exception e){ return fb.save(o); } }
  @Override public void delete(UUID id){ try { rc.delete("/api/orders/"+id); } catch(Exception ignore){} fb.delete(id); }
  @Override public Order createFromQuote(UUID quoteId){ try { return fb.createFromQuote(quoteId); } catch(Exception e){ return fb.createFromQuote(quoteId); } }
}
