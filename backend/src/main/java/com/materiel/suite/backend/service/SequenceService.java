package com.materiel.suite.backend.service;

import java.time.Year;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class SequenceService {
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private String next(String prefix) {
        String year = String.valueOf(Year.now().getValue());
        String key = prefix + year;
        int seq = counters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        return String.format("%s%s-%05d", prefix, year, seq);
    }

    public String nextQuoteNumber() {
        return next("DEV-");
    }

    public String nextOrderNumber() {
        return next("CMD-");
    }
}
