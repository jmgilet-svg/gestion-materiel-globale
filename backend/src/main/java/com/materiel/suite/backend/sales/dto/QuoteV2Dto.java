package com.materiel.suite.backend.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** DTO minimal pour exposer un devis v2 via l'API mock. */
public class QuoteV2Dto {
  private String id;
  private String reference;
  private String clientId;
  private String clientName;
  private LocalDate date;
  private String status;
  private BigDecimal totalHt;
  private BigDecimal totalTtc;
  private Boolean sent;
  private String agencyId;
  private List<Object> lines = new ArrayList<>();

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getReference(){
    return reference;
  }

  public void setReference(String reference){
    this.reference = reference;
  }

  public String getClientId(){
    return clientId;
  }

  public void setClientId(String clientId){
    this.clientId = clientId;
  }

  public String getClientName(){
    return clientName;
  }

  public void setClientName(String clientName){
    this.clientName = clientName;
  }

  public LocalDate getDate(){
    return date;
  }

  public void setDate(LocalDate date){
    this.date = date;
  }

  public String getStatus(){
    return status;
  }

  public void setStatus(String status){
    this.status = status;
  }

  public BigDecimal getTotalHt(){
    return totalHt;
  }

  public void setTotalHt(BigDecimal totalHt){
    this.totalHt = totalHt;
  }

  public BigDecimal getTotalTtc(){
    return totalTtc;
  }

  public void setTotalTtc(BigDecimal totalTtc){
    this.totalTtc = totalTtc;
  }

  public Boolean getSent(){
    return sent;
  }

  public void setSent(Boolean sent){
    this.sent = sent;
  }

  public String getAgencyId(){
    return agencyId;
  }

  public void setAgencyId(String agencyId){
    this.agencyId = agencyId;
  }

  public List<Object> getLines(){
    return lines;
  }

  public void setLines(List<Object> lines){
    this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
  }
}
