package com.materiel.suite.backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Expose une configuration d'agence très simple afin de permettre
 * la personnalisation des modèles côté client (API v2).
 */
@RestController
@RequestMapping("/api/v2/agency/config")
public class AgencyConfigController {
  public record AgencyConfigDto(String agencyId,
                                String companyName,
                                String companyAddressHtml,
                                Double vatRate,
                                String cgvHtml,
                                String emailCss,
                                String emailSignatureHtml){}

  private static final Map<String, AgencyConfigDto> STORE = new ConcurrentHashMap<>();

  private static final String DEFAULT_KEY = "_default";

  @GetMapping
  public AgencyConfigDto get(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    String key = normalize(agencyId);
    return STORE.getOrDefault(key, new AgencyConfigDto(
        agencyId,
        "Votre Société",
        "<p>Adresse société…</p>",
        0.20,
        "<p>CGV…</p>",
        "table{border-collapse:collapse}td,th{border:1px solid #ddd;padding:6px;font-family:Arial}",
        "<p>Cordialement,<br>Équipe {{agency.name}}</p>"
    ));
  }

  @PostMapping
  public AgencyConfigDto save(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                              @RequestBody AgencyConfigDto payload){
    AgencyConfigDto stored = new AgencyConfigDto(
        agencyId,
        payload.companyName(),
        payload.companyAddressHtml(),
        payload.vatRate(),
        payload.cgvHtml(),
        payload.emailCss(),
        payload.emailSignatureHtml()
    );
    STORE.put(normalize(agencyId), stored);
    return stored;
  }

  private String normalize(String agencyId){
    if (agencyId == null || agencyId.isBlank()){
      return DEFAULT_KEY;
    }
    return agencyId;
  }
}
