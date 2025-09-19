package com.materiel.suite.backend.sales.dto;

import java.util.ArrayList;
import java.util.List;

/** Représente les informations utiles d'une intervention pour générer un devis v2. */
public class InterventionV2Dto {
  private String id;
  private String title;
  private String clientId;
  private List<BillingLineV2Dto> billingLines = new ArrayList<>();

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

  public String getClientId(){
    return clientId;
  }

  public void setClientId(String clientId){
    this.clientId = clientId;
  }

  public List<BillingLineV2Dto> getBillingLines(){
    return billingLines;
  }

  public void setBillingLines(List<BillingLineV2Dto> billingLines){
    this.billingLines = billingLines == null ? new ArrayList<>() : new ArrayList<>(billingLines);
  }
}
