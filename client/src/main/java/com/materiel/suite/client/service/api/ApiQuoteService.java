package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.QuoteService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SDK léger — utilise RestClient. Fallback transparent sur mock en cas d'erreur.
 */
public class ApiQuoteService implements QuoteService {
  private final RestClient rc;
  private final QuoteService fallback;
  public ApiQuoteService(RestClient rc, QuoteService fallback){ this.rc=rc; this.fallback=fallback; }
  @Override public List<Quote> list(){
    try {
      String body = rc.get("/api/v1/quotes");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<Quote> out = new ArrayList<>();
      for (Object o : arr){ out.add(ApiSupport.toQuote(SimpleJson.asObj(o))); }
      return out;
    } catch(Exception e){ return fallback.list(); }
  }
  @Override public Quote get(UUID id){
    try {
      String body = rc.get("/api/v1/quotes/"+id);
      return ApiSupport.toQuote(SimpleJson.asObj(SimpleJson.parse(body)));
    } catch(Exception e){ return fallback.get(id); }
  }
  @Override public Quote save(Quote q){
    try {
      String json = ApiSupport.toJson(q);
      if (q.getId()==null){
        Map<String,String> h = new HashMap<>();
        h.put("Idempotency-Key", UUID.randomUUID().toString());
        String body = rc.post("/api/v1/quotes", json, h);
        return ApiSupport.toQuote(SimpleJson.asObj(SimpleJson.parse(body)));
      } else {
        Map<String,String> h = new HashMap<>();
        long version = readVersion(q);
        if (version>0) h.put("If-Match", "W/\""+version+"\"");
        String body = rc.put("/api/v1/quotes/"+q.getId(), json, h);
        return ApiSupport.toQuote(SimpleJson.asObj(SimpleJson.parse(body)));
      }
    } catch(Exception e){ return fallback.save(q); }
  }
  @Override public void delete(UUID id){
    try { rc.delete("/api/v1/quotes/"+id); } catch(Exception ignore){}
    fallback.delete(id);
  }

  private long readVersion(Quote q){
    try { return (long) Quote.class.getMethod("getVersion").invoke(q); } catch(Exception e){ return 0L; }
  }
}
