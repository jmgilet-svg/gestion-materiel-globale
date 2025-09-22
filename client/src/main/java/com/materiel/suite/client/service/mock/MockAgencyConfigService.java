package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.service.AgencyConfigGateway;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Implémentation mémoire pour la configuration d'agence en mode mock. */
public class MockAgencyConfigService implements AgencyConfigGateway {
  private static final String DEFAULT_KEY = "_default";
  private final Map<String, AgencyConfig> store = new ConcurrentHashMap<>();

  @Override
  public AgencyConfig get(){
    AgencyConfig cfg = store.get(currentKey());
    if (cfg != null){
      return copy(cfg);
    }
    AgencyConfig defaults = store.computeIfAbsent(DEFAULT_KEY, k -> defaults());
    return copy(defaults);
  }

  @Override
  public AgencyConfig save(AgencyConfig cfg){
    AgencyConfig safe = cfg == null ? defaults() : cfg;
    store.put(currentKey(), copy(safe));
    return copy(safe);
  }

  private String currentKey(){
    String agencyId = AgencyContext.agencyId();
    return agencyId == null || agencyId.isBlank() ? DEFAULT_KEY : agencyId;
  }

  private AgencyConfig copy(AgencyConfig cfg){
    if (cfg == null){
      return null;
    }
    return new AgencyConfig(
        cfg.companyName(),
        cfg.companyAddressHtml(),
        cfg.vatRate(),
        cfg.cgvHtml(),
        cfg.emailCss(),
        cfg.emailSignatureHtml()
    );
  }

  private AgencyConfig defaults(){
    return new AgencyConfig(
        "Votre Société",
        "<p>Adresse société…</p>",
        0.20,
        "<p>CGV…</p>",
        "table{border-collapse:collapse}td,th{border:1px solid #ddd;padding:6px;font-family:Arial}",
        "<p>Cordialement,<br>Équipe {{agency.name}}</p>"
    );
  }
}
