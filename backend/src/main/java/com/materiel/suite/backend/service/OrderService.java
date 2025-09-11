package com.materiel.suite.backend.service;

import com.materiel.suite.backend.model.Order;
import com.materiel.suite.backend.repo.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository repo;
    private final SequenceService sequenceService;
    private final TotalsService totalsService;

    public OrderService(OrderRepository repo, SequenceService sequenceService, TotalsService totalsService) {
        this.repo = repo;
        this.sequenceService = sequenceService;
        this.totalsService = totalsService;
    }

    public List<Order> list() {
        return repo.findAll();
    }

    public Optional<Order> get(UUID id) {
        return repo.findById(id);
    }

    public Order create(Order order) {
        order.setNumber(sequenceService.nextOrderNumber());
        totalsService.computeTotals(order);
        return repo.save(order);
    }

    public Order update(Order order) {
        totalsService.computeTotals(order);
        return repo.save(order);
    }
}

