package com.materiel.suite.client.service.api;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.service.PdfService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client REST pour le rendu PDF via l'API backend. */
public class ApiPdfService implements PdfService {
  private final RestClient rc;
  private final PdfService fallback;

  public ApiPdfService(RestClient rc, PdfService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public byte[] render(String html, Map<String, String> inlineImages, String baseUrl){
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("html", html == null ? "<html><body></body></html>" : html);
    payload.put("inlineImages", inlineImages == null ? Map.of() : inlineImages);
    if (baseUrl != null && !baseUrl.isBlank()){
      payload.put("baseUrl", baseUrl);
    }
    try {
      String json = toJson(payload);
      return rc.postForBytes("/api/v2/pdf/render", json, "application/pdf");
    } catch (Exception ex){
      if (fallback != null){
        return fallback.render(html, inlineImages, baseUrl);
      }
      throw new RuntimeException(ex);
    }
  }

  private String toJson(Map<String, Object> payload){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : payload.entrySet()){
      if (!first){
        sb.append(',');
      }
      first = false;
      sb.append('"').append(entry.getKey()).append('"').append(':');
      Object value = entry.getValue();
      if (value == null){
        sb.append("null");
      } else if (value instanceof String s){
        sb.append('"').append(escape(s)).append('"');
      } else if (value instanceof Map<?, ?> map){
        appendMap(sb, map);
      } else {
        sb.append(value);
      }
    }
    sb.append('}');
    return sb.toString();
  }

  private void appendMap(StringBuilder sb, Map<?, ?> map){
    sb.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> e : map.entrySet()){
      if (!first){
        sb.append(',');
      }
      first = false;
      Object key = e.getKey();
      Object value = e.getValue();
      sb.append('"').append(escape(key == null ? "" : key.toString())).append('"').append(':');
      if (value == null){
        sb.append("null");
      } else {
        sb.append('"').append(escape(value.toString())).append('"');
      }
    }
    sb.append('}');
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
