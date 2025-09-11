package com.materiel.suite.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Quote {
    private UUID id = UUID.randomUUID();
    private String number;
    private LocalDate date = LocalDate.now();
    private UUID customerId;
    private String customerName;
    private DocumentStatus status = DocumentStatus.QUOTE_DRAFT;
    private String notes;
    private List<DocumentLine> lines = new ArrayList<>();
    private BigDecimal totalHT = BigDecimal.ZERO;
    private BigDecimal totalTVA = BigDecimal.ZERO;
    private BigDecimal totalTTC = BigDecimal.ZERO;

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<DocumentLine> getLines() { return lines; }
    public void setLines(List<DocumentLine> lines) { this.lines = lines; }
    public BigDecimal getTotalHT() { return totalHT; }
    public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }
    public BigDecimal getTotalTVA() { return totalTVA; }
    public void setTotalTVA(BigDecimal totalTVA) { this.totalTVA = totalTVA; }
    public BigDecimal getTotalTTC() { return totalTTC; }
    public void setTotalTTC(BigDecimal totalTTC) { this.totalTTC = totalTTC; }
}
