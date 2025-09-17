package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.InterventionTypeService;

import java.util.ArrayList;
import java.util.List;

/** Récupère les types d'intervention via l'API (v2). */
public class ApiInterventionTypeService implements InterventionTypeService {
  private final RestClient rc;
  private final InterventionTypeService fallback;

  public ApiInterventionTypeService(RestClient rc, InterventionTypeService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public List<InterventionType> list(){
    try {
      String body = rc.get("/api/v2/intervention-types");
      List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<InterventionType> list = new ArrayList<>();
      for (Object o : arr){
        if (!(o instanceof java.util.Map<?,?> map)) continue;
        String code = SimpleJson.str(map.get("id"));
        String label = SimpleJson.str(map.get("name"));
        String icon = SimpleJson.str(map.get("iconKey"));
        if (code == null || code.isBlank()){
          if (label == null || label.isBlank()){
            continue;
          }
          code = label.trim();
        }
        InterventionType type = new InterventionType();
        type.setCode(code);
        type.setLabel(label != null && !label.isBlank() ? label : code);
        type.setIconKey(icon);
        list.add(type);
      }
      if (!list.isEmpty()){
        return list;
      }
    } catch (Exception ignore){
      // fallback ci-dessous
    }
    return fallback != null ? fallback.list() : List.of();
  }
}
