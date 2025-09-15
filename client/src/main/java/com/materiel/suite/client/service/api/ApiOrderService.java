package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.OrderService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApiOrderService implements OrderService {
  private final RestClient rc; private final OrderService fb;
  public ApiOrderService(RestClient rc, OrderService fallback){ this.rc=rc; this.fb=fallback; }
  @Override public List<Order> list(){
    try {
      String body = rc.get("/api/v1/orders");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Order> out = new ArrayList<>();
      for (Object o : arr){ out.add(ApiSupport.toOrder(SimpleJson.asObj(o))); }
      return out;
    } catch(Exception e){ return fb.list(); }
  }
  @Override public Order get(UUID id){
    try {
      String body = rc.get("/api/v1/orders/"+id);
      return ApiSupport.toOrder(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.get(id); }
  }
  @Override public Order save(Order o){
    try {
      String json = ApiSupport.toJson(o);
      if (o.getId()==null){
        String body = rc.post("/api/v1/orders", json);
        return ApiSupport.toOrder(SimpleJson.asObj(SimpleJson.parse(body)));
      } else {
        Map<String,String> h = new HashMap<>();
        long v = readVersion(o); if (v>0) h.put("If-Match", "W/\""+v+"\"");
        String body = rc.put("/api/v1/orders/"+o.getId(), json, h);
        return ApiSupport.toOrder(SimpleJson.asObj(SimpleJson.parse(body)));
      }
    } catch(Exception e){ return fb.save(o); }
  }
  @Override public void delete(UUID id){
    try { rc.delete("/api/v1/orders/"+id); } catch(Exception ignore){}
    fb.delete(id);
  }
  @Override public Order createFromQuote(UUID quoteId){
    try {
      String body = rc.post("/api/v1/orders/from-quote", "{\"quote\":{\"id\":\""+quoteId+"\"}}" );
      return ApiSupport.toOrder(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.createFromQuote(quoteId); }
  }
  private long readVersion(Order o){
    try { return (long) Order.class.getMethod("getVersion").invoke(o); } catch(Exception e){ return 0L; }
  }
}

