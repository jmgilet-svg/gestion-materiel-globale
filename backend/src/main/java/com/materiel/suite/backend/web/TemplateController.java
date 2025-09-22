package com.materiel.suite.backend.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Gestion simple des templates HTML pour devis/factures/emails (API v2). */
@RestController
@RequestMapping("/api/v2/templates")
public class TemplateController {
  public enum TemplateType { QUOTE, INVOICE, EMAIL }

  public record TemplateDto(String id, String agencyId, TemplateType type, String key, String name, String content){}

  private static final Map<String, TemplateDto> STORE = new ConcurrentHashMap<>();

  @GetMapping
  public List<TemplateDto> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                                @RequestParam(value = "type", required = false) TemplateType type){
    return STORE.values().stream()
        .filter(t -> agencyId == null || agencyId.isBlank() || agencyId.equals(t.agencyId()))
        .filter(t -> type == null || type == t.type())
        .collect(Collectors.toList());
  }

  @PostMapping
  public TemplateDto upsert(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                            @RequestBody TemplateDto template){
    String id = template.id() == null || template.id().isBlank() ? UUID.randomUUID().toString() : template.id();
    String resolvedAgency = template.agencyId() == null || template.agencyId().isBlank() ? agencyId : template.agencyId();
    TemplateDto stored = new TemplateDto(id, resolvedAgency, template.type(), template.key(), template.name(), template.content());
    STORE.put(id, stored);
    return stored;
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id){
    if (id != null){
      STORE.remove(id);
    }
  }
}
