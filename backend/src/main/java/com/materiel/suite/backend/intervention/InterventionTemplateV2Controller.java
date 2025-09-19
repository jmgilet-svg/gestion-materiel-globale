package com.materiel.suite.backend.intervention;

import com.materiel.suite.backend.intervention.dto.InterventionTemplateV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class InterventionTemplateV2Controller {
  @GetMapping("/api/v2/intervention-templates")
  public ResponseEntity<List<InterventionTemplateV2Dto>> list(){
    List<InterventionTemplateV2Dto> templates = new ArrayList<>();
    templates.add(template(
        "Levage standard 4h",
        "LEVAGE_STD",
        240,
        List.of("CRANE", "FORKLIFT"),
        List.of(
            line("Main d'œuvre grutier", "h", bd(85), null),
            line("Assistance manœuvre", "h", bd(60), null)
        )));
    templates.add(template(
        "Transport simple 2h",
        "TRANSPORT",
        120,
        List.of("TRUCK"),
        List.of(line("Chauffeur", "h", bd(70), null))));
    templates.add(template(
        "Manutention ponctuelle",
        "MANUTENTION",
        60,
        List.of("FORKLIFT"),
        List.of(line("Forfait manutention", "u", bd(120), BigDecimal.ONE))));
    return ResponseEntity.ok(templates);
  }

  private InterventionTemplateV2Dto template(String name,
                                             String typeId,
                                             int durationMinutes,
                                             List<String> resourceTypes,
                                             List<InterventionTemplateV2Dto.TemplateLine> lines){
    InterventionTemplateV2Dto dto = new InterventionTemplateV2Dto();
    dto.setId(UUID.randomUUID().toString());
    dto.setName(name);
    dto.setDefaultTypeId(typeId);
    dto.setDefaultDurationMinutes(durationMinutes);
    dto.setSuggestedResourceTypeIds(resourceTypes);
    dto.setDefaultLines(lines);
    return dto;
  }

  private InterventionTemplateV2Dto.TemplateLine line(String designation,
                                                       String unit,
                                                       BigDecimal unitPrice,
                                                       BigDecimal quantity){
    InterventionTemplateV2Dto.TemplateLine dto = new InterventionTemplateV2Dto.TemplateLine();
    dto.setDesignation(designation);
    dto.setUnit(unit);
    dto.setUnitPriceHt(unitPrice);
    dto.setQuantity(quantity);
    return dto;
  }

  private BigDecimal bd(double value){
    return BigDecimal.valueOf(value);
  }
}
