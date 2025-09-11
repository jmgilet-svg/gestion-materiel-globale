package com.materiel.suite.client.backend.api;

import com.materiel.suite.client.backend.invoker.ApiClient;
import com.materiel.suite.client.backend.model.Quote;

import java.util.Collections;
import java.util.List;

/** Minimal Quote API using the vendored ApiClient. */
public class QuoteApi {
    private final ApiClient client;

    public QuoteApi(ApiClient client) {
        this.client = client;
    }

    /**
     * Returns an empty list offline. Online mode can parse server response.
     */
    public List<Quote> listQuotes() {
        return Collections.emptyList();
    }
}
