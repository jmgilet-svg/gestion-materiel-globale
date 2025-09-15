package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.InvoiceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApiInvoiceService implements InvoiceService {
  private final RestClient rc; private final InvoiceService fb;
  public ApiInvoiceService(RestClient rc, InvoiceService fallback){ this.rc=rc; this.fb=fallback; }
  @Override public List<Invoice> list(){
    try {
      String body = rc.get("/api/v1/invoices");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Invoice> out = new ArrayList<>();
      for (Object o : arr){ out.add(ApiSupport.toInvoice(SimpleJson.asObj(o))); }
      return out;
    } catch(Exception e){ return fb.list(); }
  }
  @Override public Invoice get(UUID id){
    try {
      String body = rc.get("/api/v1/invoices/"+id);
      return ApiSupport.toInvoice(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.get(id); }
  }
  @Override public Invoice save(Invoice i){
    try {
      String json = ApiSupport.toJson(i);
      if (i.getId()==null){
        String body = rc.post("/api/v1/invoices", json);
        return ApiSupport.toInvoice(SimpleJson.asObj(SimpleJson.parse(body)));
      } else {
        Map<String,String> h = new HashMap<>();
        long v = readVersion(i); if (v>0) h.put("If-Match", "W/\""+v+"\"");
        String body = rc.put("/api/v1/invoices/"+i.getId(), json, h);
        return ApiSupport.toInvoice(SimpleJson.asObj(SimpleJson.parse(body)));
      }
    } catch(Exception e){ return fb.save(i); }
  }
  @Override public void delete(UUID id){
    try { rc.delete("/api/v1/invoices/"+id); } catch(Exception ignore){}
    fb.delete(id);
  }
  @Override public Invoice createFromQuote(UUID quoteId){
    try {
      String body = rc.post("/api/v1/invoices/from-quote", "{\"quote\":{\"id\":\""+quoteId+"\"}}" );
      return ApiSupport.toInvoice(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.createFromQuote(quoteId); }
  }
  @Override public Invoice createFromDeliveryNotes(List<UUID> deliveryNoteIds){
    try {
      StringBuilder arr = new StringBuilder("[");
      for (int i=0;i<deliveryNoteIds.size();i++){
        if (i>0) arr.append(",");
        arr.append("{\"id\":\"").append(deliveryNoteIds.get(i)).append("\"}");
      }
      arr.append("]");
      String body = rc.post("/api/v1/invoices/from-delivery-notes", "{\"deliveryNotes\":"+arr+"}");
      return ApiSupport.toInvoice(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fb.createFromDeliveryNotes(deliveryNoteIds); }
  }
  private long readVersion(Invoice i){
    try { return (long) Invoice.class.getMethod("getVersion").invoke(i); } catch(Exception e){ return 0L; }
  }
}

