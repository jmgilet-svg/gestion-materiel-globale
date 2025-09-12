package com.materiel.suite.client.backend.api;

import com.materiel.suite.client.backend.invoker.ApiClient;
import com.materiel.suite.client.backend.model.Order;

import java.util.Collections;
import java.util.List;

/** Minimal Order API using the vendored ApiClient. */
public class OrderApi {
    private final ApiClient client;

    public OrderApi(ApiClient client) {
        this.client = client;
    }

    /**
     * Returns an empty list offline. Online mode can parse server response.
     */
    public List<Order> listOrders() {
        return Collections.emptyList();
    }
}

