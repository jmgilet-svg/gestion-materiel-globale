package com.materiel.suite.backend.service;

import com.materiel.suite.backend.model.Quote;
import com.materiel.suite.backend.repo.QuoteRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuoteService {
    private final QuoteRepository repo;
    private final SequenceService sequenceService;
    private final TotalsService totalsService;

    public QuoteService(QuoteRepository repo, SequenceService sequenceService, TotalsService totalsService) {
        this.repo = repo;
        this.sequenceService = sequenceService;
        this.totalsService = totalsService;
    }

    public List<Quote> list() {
        return repo.findAll();
    }

    public Optional<Quote> get(UUID id) {
        return repo.findById(id);
    }

    public Quote create(Quote quote) {
        quote.setNumber(sequenceService.nextQuoteNumber());
        totalsService.computeTotals(quote);
        return repo.save(quote);
    }

    public Quote update(Quote quote) {
        totalsService.computeTotals(quote);
        return repo.save(quote);
    }
}
