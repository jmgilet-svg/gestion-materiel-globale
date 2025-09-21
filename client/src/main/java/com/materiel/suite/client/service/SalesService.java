package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;

import java.util.List;

/** Services liés à la génération et la gestion des documents de vente v2. */
public interface SalesService {
  /** Crée un devis à partir d'une intervention (lignes de facturation + méta). */
  QuoteV2 createQuoteFromIntervention(Intervention intervention);

  /** Récupère un devis par son identifiant (prévisualisation). */
  QuoteV2 getQuote(String id);

  /** Liste les devis existants. */
  List<QuoteV2> listQuotes();

  /** Crée ou met à jour un devis. */
  QuoteV2 saveQuote(QuoteV2 quote);

  /** Supprime un devis. */
  void deleteQuote(String id);

  /** Liste les factures existantes. */
  List<InvoiceV2> listInvoices();

  /** Crée ou met à jour une facture. */
  InvoiceV2 saveInvoice(InvoiceV2 invoice);

  /** Supprime une facture. */
  void deleteInvoice(String id);
}
