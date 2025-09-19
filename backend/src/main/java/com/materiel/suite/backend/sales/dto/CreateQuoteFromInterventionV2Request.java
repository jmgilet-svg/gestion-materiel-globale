package com.materiel.suite.backend.sales.dto;

/** Payload d'entrée pour générer un devis à partir d'une intervention. */
public class CreateQuoteFromInterventionV2Request {
  private InterventionV2Dto intervention;

  public InterventionV2Dto getIntervention(){
    return intervention;
  }

  public void setIntervention(InterventionV2Dto intervention){
    this.intervention = intervention;
  }
}
