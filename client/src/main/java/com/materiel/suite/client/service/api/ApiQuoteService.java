package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.QuoteService;

import java.util.List;
import java.util.UUID;

public class ApiQuoteService implements QuoteService {
  private final RestClient rc;
  private final QuoteService fallback;
  public ApiQuoteService(RestClient rc, QuoteService fb){ this.rc=rc; this.fallback=fb; }
  @Override public List<Quote> list(){ try { return fallback.list(); } catch(Exception e){ return fallback.list(); } }
  @Override public Quote get(UUID id){ try { return fallback.get(id); } catch(Exception e){ return fallback.get(id); } }
  @Override public Quote save(Quote q){ try { return fallback.save(q); } catch(Exception e){ return fallback.save(q); } }
  @Override public void delete(UUID id){ try { rc.delete("/api/quotes/"+id); } catch(Exception ignore){} fallback.delete(id); }
}
