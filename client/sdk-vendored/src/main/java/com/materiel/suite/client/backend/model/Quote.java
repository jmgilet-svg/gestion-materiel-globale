package com.materiel.suite.client.backend.model;

import java.util.List;

/** Quote model for offline SDK. */
public class Quote {
    public String id;
    public String number;
    public List<DocumentLine> lines;
    public double totalHT;
}
