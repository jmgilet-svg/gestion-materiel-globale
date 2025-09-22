package com.materiel.suite.client.service;

/**
 * Accès simplifié à la configuration d'agence (informations société,
 * styles email, etc.).
 */
public interface AgencyConfigGateway {
  record AgencyConfig(String companyName,
                      String companyAddressHtml,
                      Double vatRate,
                      String cgvHtml,
                      String emailCss,
                      String emailSignatureHtml){}

  AgencyConfig get();

  AgencyConfig save(AgencyConfig cfg);
}
