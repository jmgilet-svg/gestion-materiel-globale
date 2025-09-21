package com.materiel.suite.client.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Quote {
  private UUID id;
  private String number;
  private LocalDate date;
  private String customerName;
  private String status; // Brouillon, Envoyé, Accepté, Refusé, Expiré
  private String agencyId;
  private List<DocumentLine> lines = new ArrayList<>();
  private DocumentTotals totals = new DocumentTotals();
  // === CRM-INJECT BEGIN: quote-client-link ===
  private UUID clientId;
  private UUID contactId;
  // === CRM-INJECT END ===

  public Quote() {}
  public Quote(UUID id, String number, LocalDate date, String customerName, String status) {
    this.id=id; this.number=number; this.date=date; this.customerName=customerName; this.status=status;
  }
  public UUID getId(){ return id; }
  public void setId(UUID v){ id=v; }
  public String getNumber(){ return number; }
  public void setNumber(String v){ number=v; }
  public LocalDate getDate(){ return date; }
  public void setDate(LocalDate v){ date=v; }
  public String getCustomerName(){ return customerName; }
  public void setCustomerName(String v){ customerName=v; }
  public String getStatus(){ return status; }
  public void setStatus(String v){ status=v; }
  public String getAgencyId(){ return agencyId; }
  public void setAgencyId(String agencyId){ this.agencyId = agencyId; }
  // === CRM-INJECT BEGIN: quote-client-accessors ===
  public UUID getClientId(){ return clientId; }
  public void setClientId(UUID v){ clientId=v; }
  public UUID getContactId(){ return contactId; }
  public void setContactId(UUID v){ contactId=v; }
  // === CRM-INJECT END ===
  public List<DocumentLine> getLines(){ return lines; }
  public void setLines(List<DocumentLine> lines){ this.lines = lines; }
  public DocumentTotals getTotals(){ return totals; }
  public void recomputeTotals(){ totals = DocumentTotals.compute(lines); }
}
