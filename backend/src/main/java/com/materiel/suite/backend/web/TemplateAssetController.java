package com.materiel.suite.backend.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Gestion simplifiée des assets (images, pièces jointes) associés aux templates HTML. */
@RestController
@RequestMapping("/api/v2/template-assets")
public class TemplateAssetController {

  public record AssetDto(String id, String agencyId, String key, String name, String contentType, String base64) { }

  private static final Map<String, AssetDto> STORE = new ConcurrentHashMap<>();

  @GetMapping
  public List<AssetDto> list(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId){
    return STORE.values().stream()
        .filter(asset -> agencyId == null || agencyId.isBlank() || agencyId.equals(asset.agencyId()))
        .collect(Collectors.toList());
  }

  @PostMapping
  public AssetDto upsert(@RequestHeader(value = "X-Agency-Id", required = false) String agencyId,
                         @RequestBody AssetDto asset){
    String id = asset.id() == null || asset.id().isBlank() ? UUID.randomUUID().toString() : asset.id();
    String resolvedAgency = asset.agencyId() == null || asset.agencyId().isBlank() ? agencyId : asset.agencyId();
    AssetDto stored = new AssetDto(id, resolvedAgency, asset.key(), asset.name(), asset.contentType(), asset.base64());
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
