package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.ResourceTypeService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiResourceTypeService implements ResourceTypeService {
  private final RestClient rc;
  private final ResourceTypeService fallback;

  public ApiResourceTypeService(RestClient rc, ResourceTypeService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public List<ResourceType> listAll(){
    if (rc == null){
      return fallback != null ? fallback.listAll() : List.of();
    }
    try {
      String body = rc.get("/api/v2/resource-types");
      var arr = SimpleJson.asArr(SimpleJson.parse(body));
      List<ResourceType> out = new ArrayList<>();
      for (Object o : arr){
        out.add(fromMap(SimpleJson.asObj(o)));
      }
      return out;
    } catch (Exception e){
      return fallback != null ? fallback.listAll() : List.of();
    }
  }

  @Override
  public ResourceType save(ResourceType type){
    if (rc == null){
      return fallback != null ? fallback.save(type) : type;
    }
    try {
      boolean create = type.getId() == null || type.getId().isBlank();
      String payload = toJson(type);
      String response;
      if (create){
        response = rc.post("/api/v2/resource-types", payload);
      } else {
        response = rc.put("/api/v2/resource-types/" + encode(type.getId()), payload);
      }
      return fromMap(SimpleJson.asObj(SimpleJson.parse(response)));
    } catch (Exception e){
      return fallback != null ? fallback.save(type) : type;
    }
  }

  @Override
  public void delete(String id){
    if (id == null || id.isBlank()){
      return;
    }
    if (rc != null){
      try {
        rc.delete("/api/v2/resource-types/" + encode(id));
      } catch (IOException | InterruptedException ignore){}
    }
    if (fallback != null){
      fallback.delete(id);
    }
  }

  private ResourceType fromMap(Map<String, Object> map){
    ResourceType type = new ResourceType();
    type.setId(SimpleJson.str(map.get("id")));
    type.setName(SimpleJson.str(map.get("name")));
    type.setIconKey(SimpleJson.str(map.get("iconKey")));
    return type;
  }

  private String toJson(ResourceType type){
    StringBuilder sb = new StringBuilder("{");
    appendField(sb, "id", type.getId());
    appendField(sb, "name", type.getName());
    appendField(sb, "iconKey", type.getIconKey());
    sb.append('}');
    return sb.toString();
  }

  private void appendField(StringBuilder sb, String key, String value){
    if (sb.length() > 1){
      sb.append(',');
    }
    sb.append('"').append(key).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append('"').append(escape(value)).append('"');
    }
  }

  private String escape(String s){
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String encode(String value){
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
