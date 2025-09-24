package com.materiel.suite.client.service.api;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.DocumentTemplateService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Client REST léger pour la gestion des templates HTML. */
public class ApiDocumentTemplateService implements DocumentTemplateService {
  private final RestClient rc;
  private final DocumentTemplateService fallback;

  public ApiDocumentTemplateService(RestClient rc, DocumentTemplateService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public List<Template> list(String type){
    try {
      String path = "/api/v2/templates";
      if (type != null && !type.isBlank()){
        path += "?type=" + URLEncoder.encode(type, StandardCharsets.UTF_8);
      }
      String json = rc.get(path);
      Object parsed = SimpleJson.parse(json);
      List<Object> arr = SimpleJson.asArr(parsed);
      List<Template> result = new ArrayList<>();
      for (Object item : arr){
        result.add(fromMap(SimpleJson.asObj(item)));
      }
      return result;
    } catch (Exception ex){
      if (fallback != null){
        return fallback.list(type);
      }
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Template save(Template template){
    try {
      String json = rc.post("/api/v2/templates", toJson(template));
      return fromMap(SimpleJson.asObj(SimpleJson.parse(json)));
    } catch (Exception ex){
      if (fallback != null){
        return fallback.save(template);
      }
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void delete(String id){
    try {
      rc.delete("/api/v2/templates/" + id);
    } catch (Exception ex){
      if (fallback != null){
        fallback.delete(id);
        return;
      }
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<Asset> listAssets(){
    try {
      String json = rc.get("/api/v2/template-assets");
      Object parsed = SimpleJson.parse(json);
      List<Object> arr = SimpleJson.asArr(parsed);
      List<Asset> result = new ArrayList<>();
      for (Object item : arr){
        result.add(assetFromMap(SimpleJson.asObj(item)));
      }
      return result;
    } catch (Exception ex){
      if (fallback != null){
        return fallback.listAssets();
      }
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Asset saveAsset(Asset asset){
    try {
      String json = rc.post("/api/v2/template-assets", assetToJson(asset));
      return assetFromMap(SimpleJson.asObj(SimpleJson.parse(json)));
    } catch (Exception ex){
      if (fallback != null){
        return fallback.saveAsset(asset);
      }
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void deleteAsset(String id){
    try {
      rc.delete("/api/v2/template-assets/" + id);
    } catch (Exception ex){
      if (fallback != null){
        fallback.deleteAsset(id);
        return;
      }
      throw new RuntimeException(ex);
    }
  }

  private Template fromMap(Map<String, Object> map){
    Template t = new Template();
    t.setId(SimpleJson.str(map.get("id")));
    t.setAgencyId(SimpleJson.str(map.get("agencyId")));
    t.setType(SimpleJson.str(map.get("type")));
    t.setKey(SimpleJson.str(map.get("key")));
    t.setName(SimpleJson.str(map.get("name")));
    t.setContent(SimpleJson.str(map.get("content")));
    return t;
  }

  private String toJson(Template template){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    first = append(sb, first, "id", template.getId());
    first = append(sb, first, "agencyId", template.getAgencyId());
    first = append(sb, first, "type", template.getType());
    first = append(sb, first, "key", template.getKey());
    first = append(sb, first, "name", template.getName());
    append(sb, first, "content", template.getContent());
    sb.append('}');
    return sb.toString();
  }

  private Asset assetFromMap(Map<String, Object> map){
    Asset asset = new Asset();
    asset.setId(SimpleJson.str(map.get("id")));
    asset.setAgencyId(SimpleJson.str(map.get("agencyId")));
    asset.setKey(SimpleJson.str(map.get("key")));
    asset.setName(SimpleJson.str(map.get("name")));
    asset.setContentType(SimpleJson.str(map.get("contentType")));
    asset.setBase64(SimpleJson.str(map.get("base64")));
    return asset;
  }

  private String assetToJson(Asset asset){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    first = append(sb, first, "id", asset == null ? null : asset.getId());
    first = append(sb, first, "agencyId", asset == null ? null : asset.getAgencyId());
    first = append(sb, first, "key", asset == null ? null : asset.getKey());
    first = append(sb, first, "name", asset == null ? null : asset.getName());
    first = append(sb, first, "contentType", asset == null ? null : asset.getContentType());
    append(sb, first, "base64", asset == null ? null : asset.getBase64());
    sb.append('}');
    return sb.toString();
  }

  private boolean append(StringBuilder sb, boolean first, String name, String value){
    if (!first){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append('"').append(escape(value)).append('"');
    }
    return false;
  }

  private String escape(String value){
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < value.length(); i++){
      char c = value.charAt(i);
      if (c == '\\' || c == '"'){
        sb.append('\\').append(c);
      } else if (c < 0x20){
        sb.append(String.format("\\u%04x", (int) c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
