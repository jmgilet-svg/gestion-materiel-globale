package com.materiel.suite.backend.web;

import com.materiel.suite.backend.model.Order;
import com.materiel.suite.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<Order> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable UUID id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        Order saved = service.create(order);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable UUID id, @RequestBody Order order) {
        if (!id.equals(order.getId())) {
            return ResponseEntity.badRequest().build();
        }
        Order saved = service.update(order);
        return ResponseEntity.ok(saved);
    }
}

