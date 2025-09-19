package com.materiel.suite.backend.intervention.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** DTO léger pour l'API mockée d'interventions v2. */
public class InterventionV2Dto {
  private String id;
  private String title;
  private OffsetDateTime plannedStart;
  private OffsetDateTime plannedEnd;
  private String clientId;
  private String address;
  private String quoteId;
  private String quoteReference;
  private List<Object> billingLines;
  private String workflowStage;
  private boolean generalDone;
  private boolean detailsDone;
  private boolean billingReady;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getTitle(){
    return title;
  }

  public void setTitle(String title){
    this.title = title;
  }

  public OffsetDateTime getPlannedStart(){
    return plannedStart;
  }

  public void setPlannedStart(OffsetDateTime plannedStart){
    this.plannedStart = plannedStart;
  }

  public OffsetDateTime getPlannedEnd(){
    return plannedEnd;
  }

  public void setPlannedEnd(OffsetDateTime plannedEnd){
    this.plannedEnd = plannedEnd;
  }

  public String getClientId(){
    return clientId;
  }

  public void setClientId(String clientId){
    this.clientId = clientId;
  }

  public String getAddress(){
    return address;
  }

  public void setAddress(String address){
    this.address = address;
  }

  public String getQuoteId(){
    return quoteId;
  }

  public void setQuoteId(String quoteId){
    this.quoteId = quoteId;
  }

  public String getQuoteReference(){
    return quoteReference;
  }

  public void setQuoteReference(String quoteReference){
    this.quoteReference = quoteReference;
  }

  public List<Object> getBillingLines(){
    return billingLines;
  }

  public void setBillingLines(List<Object> billingLines){
    this.billingLines = billingLines;
  }

  public String getWorkflowStage(){
    return workflowStage;
  }

  public void setWorkflowStage(String workflowStage){
    this.workflowStage = workflowStage;
  }

  public boolean isGeneralDone(){
    return generalDone;
  }

  public void setGeneralDone(boolean generalDone){
    this.generalDone = generalDone;
  }

  public boolean isDetailsDone(){
    return detailsDone;
  }

  public void setDetailsDone(boolean detailsDone){
    this.detailsDone = detailsDone;
  }

  public boolean isBillingReady(){
    return billingReady;
  }

  public void setBillingReady(boolean billingReady){
    this.billingReady = billingReady;
  }
}
