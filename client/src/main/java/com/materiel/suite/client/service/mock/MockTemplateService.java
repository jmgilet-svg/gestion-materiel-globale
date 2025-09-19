package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.InterventionTemplate;
import com.materiel.suite.client.service.TemplateService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Catalogue statique de modèles pour le mode mock. */
public class MockTemplateService implements TemplateService {
  private final List<InterventionTemplate> templates = new ArrayList<>();

  public MockTemplateService(){
    seed();
  }

  @Override
  public List<InterventionTemplate> list(){
    List<InterventionTemplate> copy = new ArrayList<>();
    for (InterventionTemplate template : templates){
      copy.add(copy(template));
    }
    return copy;
  }

  private void seed(){
    if (!templates.isEmpty()){
      return;
    }
    templates.add(build(
        "Levage standard 4h",
        "LEVAGE_STD",
        240,
        List.of("CRANE", "FORKLIFT"),
        List.of(line("Main d'œuvre grutier", "h", bd(85), null),
            line("Assistance manœuvre", "h", bd(60), null))));
    templates.add(build(
        "Transport simple 2h",
        "TRANSPORT",
        120,
        List.of("TRUCK"),
        List.of(line("Chauffeur", "h", bd(70), null))));
    templates.add(build(
        "Manutention ponctuelle",
        "MANUTENTION",
        60,
        List.of("FORKLIFT"),
        List.of(line("Forfait manutention", "u", bd(120), BigDecimal.ONE))));
  }

  private InterventionTemplate build(String name,
                                     String typeId,
                                     int durationMinutes,
                                     List<String> resourceTypes,
                                     List<InterventionTemplate.TemplateLine> lines){
    InterventionTemplate template = new InterventionTemplate();
    template.setId(UUID.randomUUID().toString());
    template.setName(name);
    template.setDefaultTypeId(typeId);
    template.setDefaultDurationMinutes(durationMinutes);
    template.setSuggestedResourceTypeIds(resourceTypes);
    template.setDefaultLines(lines);
    return template;
  }

  private InterventionTemplate.TemplateLine line(String designation,
                                                  String unit,
                                                  BigDecimal unitPrice,
                                                  BigDecimal quantity){
    InterventionTemplate.TemplateLine line = new InterventionTemplate.TemplateLine();
    line.setDesignation(designation);
    line.setUnit(unit);
    line.setUnitPriceHt(unitPrice);
    line.setQuantity(quantity);
    return line;
  }

  private InterventionTemplate copy(InterventionTemplate template){
    InterventionTemplate copy = new InterventionTemplate();
    copy.setId(template.getId());
    copy.setName(template.getName());
    copy.setDefaultTypeId(template.getDefaultTypeId());
    copy.setDefaultDurationMinutes(template.getDefaultDurationMinutes());
    copy.setSuggestedResourceTypeIds(template.getSuggestedResourceTypeIds());
    copy.setDefaultLines(template.getDefaultLines());
    return copy;
  }

  private BigDecimal bd(double value){
    return BigDecimal.valueOf(value);
  }
}
