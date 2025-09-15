package com.materiel.suite.backend.v1.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "delivery_notes")
public class DeliveryNoteEntity {
  @Id
  private UUID id;
  private String number;
  private String customerName;
  @Enumerated(EnumType.STRING)
  private DocumentStatus status = DocumentStatus.DRAFT;
  @ElementCollection
  @CollectionTable(name="delivery_lines", joinColumns=@JoinColumn(name="delivery_id"))
  private List<DocLineEmb> lines = new ArrayList<>();

  private BigDecimal totalHt;
  private BigDecimal totalVat;
  private BigDecimal totalTtc;

  private long version;
  private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getNumber() { return number; }
  public void setNumber(String number) { this.number = number; }
  public String getCustomerName() { return customerName; }
  public void setCustomerName(String customerName) { this.customerName = customerName; }
  public DocumentStatus getStatus() { return status; }
  public void setStatus(DocumentStatus status) { this.status = status; }
  public List<DocLineEmb> getLines() { return lines; }
  public void setLines(List<DocLineEmb> lines) { this.lines = lines; }
  public BigDecimal getTotalHt() { return totalHt; }
  public void setTotalHt(BigDecimal totalHt) { this.totalHt = totalHt; }
  public BigDecimal getTotalVat() { return totalVat; }
  public void setTotalVat(BigDecimal totalVat) { this.totalVat = totalVat; }
  public BigDecimal getTotalTtc() { return totalTtc; }
  public void setTotalTtc(BigDecimal totalTtc) { this.totalTtc = totalTtc; }
  public long getVersion() { return version; }
  public void setVersion(long version) { this.version = version; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

