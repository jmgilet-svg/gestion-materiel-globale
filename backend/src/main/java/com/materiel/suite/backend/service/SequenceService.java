package com.materiel.suite.backend.service;

import java.time.Year;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class SequenceService {
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public String nextQuoteNumber() {
        String year = String.valueOf(Year.now().getValue());
        String key = "DEV-" + year;
        int seq = counters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        return String.format("DEV-%s-%05d", year, seq);
    }
}
