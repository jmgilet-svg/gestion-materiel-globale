package com.materiel.suite.backend.v1.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Quote {
  private UUID id;
  private String number;
  private String customerName;
  private DocumentStatus status = DocumentStatus.DRAFT;
  private List<DocLine> lines = new ArrayList<>();

  // Totaux
  private BigDecimal totalHt;
  private BigDecimal totalVat;
  private BigDecimal totalTtc;

  // Concurrency
  private long version = 0L;
  private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getNumber() { return number; }
  public void setNumber(String number) { this.number = number; }
  public String getCustomerName() { return customerName; }
  public void setCustomerName(String customerName) { this.customerName = customerName; }
  public DocumentStatus getStatus() { return status; }
  public void setStatus(DocumentStatus status) { this.status = status; }
  public List<DocLine> getLines() { return lines; }
  public void setLines(List<DocLine> lines) { this.lines = lines; }
  public BigDecimal getTotalHt() { return totalHt; }
  public void setTotalHt(BigDecimal totalHt) { this.totalHt = totalHt; }
  public BigDecimal getTotalVat() { return totalVat; }
  public void setTotalVat(BigDecimal totalVat) { this.totalVat = totalVat; }
  @JsonProperty("totalTTC")
  public BigDecimal getTotalTtc() { return totalTtc; }
  @JsonProperty("totalTTC")
  public void setTotalTtc(BigDecimal totalTtc) { this.totalTtc = totalTtc; }
  public long getVersion() { return version; }
  public void setVersion(long version) { this.version = version; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
