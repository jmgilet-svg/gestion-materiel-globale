package com.materiel.suite.backend.repo;

import com.materiel.suite.backend.model.Quote;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class QuoteRepository {
    private final Map<UUID, Quote> storage = new ConcurrentHashMap<>();

    public List<Quote> findAll() {
        return new ArrayList<>(storage.values());
    }

    public Optional<Quote> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Quote save(Quote quote) {
        storage.put(quote.getId(), quote);
        return quote;
    }
}
