package com.materiel.suite.backend.repo;

import com.materiel.suite.backend.model.Order;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class OrderRepository {
    private final Map<UUID, Order> storage = new ConcurrentHashMap<>();

    public List<Order> findAll() {
        return new ArrayList<>(storage.values());
    }

    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Order save(Order order) {
        storage.put(order.getId(), order);
        return order;
    }
}

