package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.service.QuoteService;

import java.util.List;
import java.util.UUID;

public class MockQuoteService implements QuoteService {
  @Override public List<Quote> list() { return MockData.QUOTES; }
  @Override public Quote get(UUID id){ return MockData.findById(MockData.QUOTES, id); }
  @Override public Quote save(Quote q){
    if (q.getId()==null){ q.setId(UUID.randomUUID()); }
    if (q.getNumber()==null || q.getNumber().isBlank()){
      q.setNumber(MockData.nextQuoteNumber());
    }
    q.recomputeTotals();
    var existing = get(q.getId());
    if (existing==null){ MockData.QUOTES.add(q); } else {
      MockData.QUOTES.replaceAll(x -> x.getId().equals(q.getId()) ? q : x);
    }
    return q;
  }
  @Override public void delete(UUID id){ MockData.QUOTES.removeIf(q -> q.getId().equals(id)); }
}
