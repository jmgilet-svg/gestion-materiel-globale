package com.materiel.suite.backend.v1.service;

import com.materiel.suite.backend.v1.api.ResourceTypeDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceTypeService {
  private final Map<String, ResourceTypeDto> types = new LinkedHashMap<>();

  @PostConstruct
  public void seed(){
    if (!types.isEmpty()) return;
    upsert(new ResourceTypeDto("CRANE", "Grue", "🏗️"));
    upsert(new ResourceTypeDto("TRUCK", "Camion", "🚚"));
    upsert(new ResourceTypeDto("TRAILER", "Remorque", "🚛"));
    upsert(new ResourceTypeDto("DRIVER", "Chauffeur", "👷"));
    upsert(new ResourceTypeDto("GENERIC", "Ressource", "🏷️"));
  }

  public synchronized List<ResourceTypeDto> list(){
    return new ArrayList<>(types.values());
  }

  public synchronized ResourceTypeDto upsert(ResourceTypeDto dto){
    if (dto==null || !StringUtils.hasText(dto.getCode())){
      throw new IllegalArgumentException("code requis");
    }
    String code = normalizeCode(dto.getCode());
    ResourceTypeDto existing = types.get(code);
    String label = normalizeLabel(dto.getLabel(), existing, code);
    String icon = normalizeIcon(dto.getIcon(), existing);
    ResourceTypeDto saved = new ResourceTypeDto(code, label, icon);
    types.put(code, saved);
    return saved;
  }

  public synchronized void delete(String code){
    if (!StringUtils.hasText(code)) return;
    types.remove(normalizeCode(code));
  }

  public synchronized ResourceTypeDto get(String code){
    if (!StringUtils.hasText(code)) return null;
    return types.get(normalizeCode(code));
  }

  private String normalizeCode(String code){
    return code.trim().toUpperCase();
  }

  private String normalizeLabel(String label, ResourceTypeDto existing, String code){
    if (StringUtils.hasText(label)) return label.trim();
    if (existing!=null && StringUtils.hasText(existing.getLabel())) return existing.getLabel();
    return code;
  }

  private String normalizeIcon(String icon, ResourceTypeDto existing){
    if (StringUtils.hasText(icon)) return icon.trim();
    if (existing!=null && StringUtils.hasText(existing.getIcon())) return existing.getIcon();
    return null;
  }
}
