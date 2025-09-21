package com.materiel.suite.backend.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** DTO mock pour exposer une facture v2. */
public class InvoiceV2Dto {
  private String id;
  private String number;
  private String clientId;
  private String clientName;
  private LocalDate date;
  private BigDecimal totalHt;
  private BigDecimal totalTtc;
  private String status;
  private String agencyId;
  private List<Object> lines = new ArrayList<>();

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getNumber(){
    return number;
  }

  public void setNumber(String number){
    this.number = number;
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

  public String getStatus(){
    return status;
  }

  public void setStatus(String status){
    this.status = status;
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
