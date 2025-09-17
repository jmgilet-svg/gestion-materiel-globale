package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.InterventionTypeService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    if (rc == null){
      return fallback != null ? fallback.list() : List.of();
    }
    try {
      String body = rc.get("/api/v2/intervention-types");
      List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<InterventionType> list = new ArrayList<>();
      for (Object o : arr){
        InterventionType type = fromMap(SimpleJson.asObj(o));
        if (type != null){
          list.add(type);
        }
      }
      return list;
    } catch (Exception ignore){
      return fallback != null ? fallback.list() : List.of();
    }
  }

  @Override
  public InterventionType save(InterventionType type){
    if (type == null){
      return null;
    }
    if (rc == null){
      return fallback != null ? fallback.save(type) : type;
    }
    try {
      boolean create = type.getCode() == null || type.getCode().isBlank();
      String payload = toJson(type);
      String response;
      if (create){
        response = rc.post("/api/v2/intervention-types", payload);
      } else {
        response = rc.put("/api/v2/intervention-types/" + encode(type.getCode()), payload);
      }
      return fromMap(SimpleJson.asObj(SimpleJson.parse(response)));
    } catch (Exception ignore){
      return fallback != null ? fallback.save(type) : type;
    }
  }

  @Override
  public void delete(String code){
    if (code == null || code.isBlank()){
      return;
    }
    if (rc != null){
      try {
        rc.delete("/api/v2/intervention-types/" + encode(code));
      } catch (Exception ignore){}
    }
    if (fallback != null){
      fallback.delete(code);
    }
  }

  private InterventionType fromMap(Map<String, Object> map){
    if (map == null){
      return null;
    }
    String code = SimpleJson.str(map.get("id"));
    String label = SimpleJson.str(map.get("name"));
    String icon = SimpleJson.str(map.get("iconKey"));
    Integer orderIndex = null;
    Object orderValue = map.get("orderIndex");
    if (orderValue instanceof Number number){
      orderIndex = number.intValue();
    } else if (orderValue != null){
      try {
        orderIndex = Integer.parseInt(orderValue.toString());
      } catch (NumberFormatException ignore){}
    }
    if (code == null || code.isBlank()){
      return null;
    }
    InterventionType type = new InterventionType();
    type.setCode(code);
    type.setLabel(label != null && !label.isBlank() ? label : code);
    type.setIconKey(icon);
    type.setOrderIndex(orderIndex);
    return type;
  }

  private String toJson(InterventionType type){
    StringBuilder sb = new StringBuilder("{");
    appendField(sb, "id", type.getCode());
    String name = type.getLabel();
    if (name == null || name.isBlank()){
      name = type.getCode();
    }
    appendField(sb, "name", name);
    appendField(sb, "iconKey", type.getIconKey());
    appendField(sb, "orderIndex", type.getOrderIndex());
    sb.append('}');
    return sb.toString();
  }

  private void appendField(StringBuilder sb, String key, Object value){
    if (sb.length() > 1){
      sb.append(',');
    }
    sb.append('"').append(key).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else if (value instanceof Number || value instanceof Boolean){
      sb.append(value);
    } else {
      sb.append('"').append(escape(value.toString())).append('"');
    }
  }

  private String escape(String s){
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String encode(String value){
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
