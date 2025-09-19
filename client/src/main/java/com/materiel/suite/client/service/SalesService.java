package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.QuoteV2;

/** Services liés à la génération de devis v2 à partir des interventions. */
public interface SalesService {
  /** Crée un devis à partir d'une intervention (lignes de facturation + méta). */
  QuoteV2 createQuoteFromIntervention(Intervention intervention);

  /** Récupère un devis par son identifiant (prévisualisation). */
  QuoteV2 getQuote(String id);
}
