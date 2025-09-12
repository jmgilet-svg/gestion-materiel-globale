package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Quote;

import java.util.List;
import java.util.UUID;

public interface QuoteService {
  List<Quote> list();
  Quote get(UUID id);
  Quote save(Quote q);
  void delete(UUID id);
}
