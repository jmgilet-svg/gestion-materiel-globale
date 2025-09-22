package com.materiel.suite.client.service.api;

import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.AgencyConfigGateway;

import java.util.Map;

/** Client REST minimaliste pour récupérer et sauvegarder la configuration d'agence. */
public class ApiAgencyConfigService implements AgencyConfigGateway {
  private final RestClient rc;
  private final AgencyConfigGateway fallback;

  public ApiAgencyConfigService(RestClient rc, AgencyConfigGateway fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override
  public AgencyConfig get(){
    if (rc == null){
      return fallback != null ? fallback.get() : defaults();
    }
    try {
      String body = rc.get("/api/v2/agency/config");
      Map<String, Object> map = SimpleJson.asObj(SimpleJson.parse(body));
      AgencyConfig cfg = fromMap(map);
      return cfg != null ? cfg : defaults();
    } catch (Exception ex){
      return fallback != null ? fallback.get() : defaults();
    }
  }

  @Override
  public AgencyConfig save(AgencyConfig cfg){
    if (rc == null){
      return fallback != null ? fallback.save(cfg) : (cfg == null ? defaults() : cfg);
    }
    try {
      String payload = toJson(cfg);
      String body = rc.post("/api/v2/agency/config", payload);
      Map<String, Object> map = SimpleJson.asObj(SimpleJson.parse(body));
      AgencyConfig saved = fromMap(map);
      return saved != null ? saved : cfg;
    } catch (Exception ex){
      return fallback != null ? fallback.save(cfg) : (cfg == null ? defaults() : cfg);
    }
  }

  private AgencyConfig fromMap(Map<String, Object> map){
    if (map == null){
      return null;
    }
    return new AgencyConfig(
        SimpleJson.str(map.get("companyName")),
        SimpleJson.str(map.get("companyAddressHtml")),
        toDouble(map.get("vatRate")),
        SimpleJson.str(map.get("cgvHtml")),
        SimpleJson.str(map.get("emailCss")),
        SimpleJson.str(map.get("emailSignatureHtml"))
    );
  }

  private Double toDouble(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof Number number){
      return number.doubleValue();
    }
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException ex){
      return null;
    }
  }

  private String toJson(AgencyConfig cfg){
    AgencyConfig safe = cfg == null ? defaults() : cfg;
    StringBuilder sb = new StringBuilder("{");
    appendField(sb, "companyName", safe.companyName());
    appendField(sb, "companyAddressHtml", safe.companyAddressHtml());
    appendNumberField(sb, "vatRate", safe.vatRate());
    appendField(sb, "cgvHtml", safe.cgvHtml());
    appendField(sb, "emailCss", safe.emailCss());
    appendField(sb, "emailSignatureHtml", safe.emailSignatureHtml());
    sb.append('}');
    return sb.toString();
  }

  private void appendField(StringBuilder sb, String name, String value){
    if (sb.length() > 1){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append('"').append(escape(value)).append('"');
    }
  }

  private void appendNumberField(StringBuilder sb, String name, Double value){
    if (sb.length() > 1){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append(value);
    }
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

  private AgencyConfig defaults(){
    return new AgencyConfig(
        "Votre Société",
        "<p>Adresse société…</p>",
        0.20,
        "<p>CGV…</p>",
        "table{border-collapse:collapse}td,th{border:1px solid #ddd;padding:6px;font-family:Arial}",
        "<p>Cordialement,<br>Équipe {{agency.name}}</p>"
    );
  }
}
