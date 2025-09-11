package com.materiel.suite.client.backend.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {
    private UUID id;
    private String number;
    private List<DocumentLine> lines = new ArrayList<>();
    private BigDecimal totalHT;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public List<DocumentLine> getLines() { return lines; }
    public void setLines(List<DocumentLine> lines) { this.lines = lines; }
    public BigDecimal getTotalHT() { return totalHT; }
    public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }
}

