package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.InterventionTemplate;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.TemplateService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Récupère les modèles d'intervention via l'API backend. */
public class ApiTemplateService implements TemplateService {
  private final RestClient rc;
  private final TemplateService fallback;

  public ApiTemplateService(RestClient rc, TemplateService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public List<InterventionTemplate> list(){
    if (rc == null){
      return fallback != null ? fallback.list() : List.of();
    }
    try {
      String body = rc.get("/api/v2/intervention-templates");
      List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<InterventionTemplate> list = new ArrayList<>();
      for (Object item : arr){
        InterventionTemplate template = fromMap(SimpleJson.asObj(item));
        if (template != null){
          list.add(template);
        }
      }
      return list;
    } catch (Exception ignore){
      return fallback != null ? fallback.list() : List.of();
    }
  }

  private InterventionTemplate fromMap(Map<String, Object> map){
    if (map == null){
      return null;
    }
    InterventionTemplate template = new InterventionTemplate();
    template.setId(SimpleJson.str(map.get("id")));
    template.setName(SimpleJson.str(map.get("name")));
    template.setDefaultTypeId(SimpleJson.str(map.get("defaultTypeId")));
    Object duration = map.get("defaultDurationMinutes");
    if (duration instanceof Number number){
      template.setDefaultDurationMinutes(number.intValue());
    } else if (duration != null){
      try {
        template.setDefaultDurationMinutes(Integer.parseInt(duration.toString()));
      } catch (NumberFormatException ignore){}
    }
    Object resources = map.get("suggestedResourceTypeIds");
    if (resources instanceof List<?> list){
      List<String> ids = new ArrayList<>();
      for (Object value : list){
        if (value != null){
          ids.add(value.toString());
        }
      }
      template.setSuggestedResourceTypeIds(ids);
    }
    Object lines = map.get("defaultLines");
    if (lines instanceof List<?> arr){
      List<InterventionTemplate.TemplateLine> parsed = new ArrayList<>();
      for (Object value : arr){
        Map<String, Object> entry = value instanceof Map<?, ?> m ? cast(m) : null;
        if (entry != null){
          InterventionTemplate.TemplateLine line = toLine(entry);
          if (line != null){
            parsed.add(line);
          }
        }
      }
      template.setDefaultLines(parsed);
    }
    return template;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> cast(Map<?, ?> map){
    return (Map<String, Object>) map;
  }

  private InterventionTemplate.TemplateLine toLine(Map<String, Object> map){
    InterventionTemplate.TemplateLine line = new InterventionTemplate.TemplateLine();
    line.setDesignation(SimpleJson.str(map.get("designation")));
    line.setUnit(SimpleJson.str(map.get("unit")));
    line.setUnitPriceHt(toBigDecimal(map.get("unitPriceHt")));
    line.setQuantity(toBigDecimal(map.get("quantity")));
    return line;
  }

  private BigDecimal toBigDecimal(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof BigDecimal bd){
      return bd;
    }
    if (value instanceof Number number){
      return new BigDecimal(number.toString());
    }
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException ex){
      return null;
    }
  }
}
