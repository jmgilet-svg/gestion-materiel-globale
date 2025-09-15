package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.DeliveryNoteService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApiDeliveryNoteService implements DeliveryNoteService {
  private final RestClient rc; private final DeliveryNoteService fb;
  public ApiDeliveryNoteService(RestClient rc, DeliveryNoteService fallback){ this.rc=rc; this.fb=fallback; }
  @Override public List<DeliveryNote> list(){
    try {
      String body = rc.get("/api/v1/delivery-notes");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<DeliveryNote> out = new ArrayList<>();
      for (Object o : arr){ out.add(ApiSupport.toDelivery(SimpleJson.asObj(o))); }
      return out;
    } catch(Exception e){ return fb.list(); }
  }
  @Override public DeliveryNote get(UUID id){
    try {
      String body = rc.get("/api/v1/delivery-notes/"+id);
      return ApiSupport.toDelivery(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.get(id); }
  }
  @Override public DeliveryNote save(DeliveryNote d){
    try {
      String json = ApiSupport.toJson(d);
      if (d.getId()==null){
        String body = rc.post("/api/v1/delivery-notes", json);
        return ApiSupport.toDelivery(SimpleJson.asObj(SimpleJson.parse(body)));
      } else {
        Map<String,String> h = new HashMap<>();
        long v = readVersion(d); if (v>0) h.put("If-Match", "W/\""+v+"\"");
        String body = rc.put("/api/v1/delivery-notes/"+d.getId(), json, h);
        return ApiSupport.toDelivery(SimpleJson.asObj(SimpleJson.parse(body)));
      }
    } catch(Exception e){ return fb.save(d); }
  }
  @Override public void delete(UUID id){
    try { rc.delete("/api/v1/delivery-notes/"+id); } catch(Exception ignore){}
    fb.delete(id);
  }
  @Override public DeliveryNote createFromOrder(UUID orderId){
    try {
      String body = rc.post("/api/v1/delivery-notes/from-order", "{\"order\":{\"id\":\""+orderId+"\"}}" );
      return ApiSupport.toDelivery(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.createFromOrder(orderId); }
  }
  private long readVersion(DeliveryNote d){
    try { return (long) DeliveryNote.class.getMethod("getVersion").invoke(d); } catch(Exception e){ return 0L; }
  }
}

