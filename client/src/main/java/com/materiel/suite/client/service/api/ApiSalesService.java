package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.SalesService;

import java.math.BigDecimal;
import java.util.List;

/** Client REST minimal pour les opérations de ventes v2. */
public class ApiSalesService implements SalesService {
  private final RestClient rc;
  private final SalesService fallback;

  public ApiSalesService(RestClient rc, SalesService fallback){
    this.rc = rc;
    this.fallback = fallback;
  }

  @Override public QuoteV2 createQuoteFromIntervention(Intervention intervention){
    if (intervention == null){
      return null;
    }
    try {
      String body = rc.post("/api/v2/quotes/from-intervention", toJson(intervention));
      return parseQuote(body);
    } catch (Exception e){
      return fallback.createQuoteFromIntervention(intervention);
    }
  }

  @Override public QuoteV2 getQuote(String id){
    if (id == null || id.isBlank()){
      return null;
    }
    try {
      String body = rc.get("/api/v2/quotes/" + id);
      return parseQuote(body);
    } catch (Exception e){
      return fallback.getQuote(id);
    }
  }

  private QuoteV2 parseQuote(String body){
    if (body == null || body.isBlank()){
      return null;
    }
    var map = SimpleJson.asObj(SimpleJson.parse(body));
    QuoteV2 quote = new QuoteV2();
    quote.setId(SimpleJson.str(map.get("id")));
    quote.setReference(SimpleJson.str(map.get("reference")));
    quote.setStatus(SimpleJson.str(map.get("status")));
    quote.setTotalHt(parseBigDecimal(map.get("totalHt")));
    quote.setTotalTtc(parseBigDecimal(map.get("totalTtc")));
    return quote;
  }

  private BigDecimal parseBigDecimal(Object value){
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

  private String toJson(Intervention intervention){
    StringBuilder sb = new StringBuilder();
    sb.append("{\"intervention\":{");
    boolean firstField = true;
    String id = intervention.getId() == null ? null : intervention.getId().toString();
    firstField = appendStringField(sb, firstField, "id", id);
    firstField = appendStringField(sb, firstField, "title", intervention.getLabel());
    String clientId = intervention.getClientId() == null ? null : intervention.getClientId().toString();
    firstField = appendStringField(sb, firstField, "clientId", clientId);
    if (!firstField){
      sb.append(',');
    }
    sb.append("\"billingLines\":[");
    List<BillingLine> lines = intervention.getBillingLines();
    boolean firstLine = true;
    for (BillingLine line : lines){
      if (line == null){
        continue;
      }
      if (!firstLine){
        sb.append(',');
      }
      firstLine = false;
      sb.append('{');
      boolean firstProp = true;
      firstProp = appendStringField(sb, firstProp, "id", line.getId());
      firstProp = appendStringField(sb, firstProp, "designation", line.getDesignation());
      firstProp = appendNumberField(sb, firstProp, "quantity", line.getQuantity());
      firstProp = appendStringField(sb, firstProp, "unit", line.getUnit());
      firstProp = appendNumberField(sb, firstProp, "unitPriceHt", line.getUnitPriceHt());
      appendNumberField(sb, firstProp, "totalHt", line.getTotalHt());
      sb.append('}');
    }
    sb.append(']');
    sb.append("}}");
    return sb.toString();
  }

  private boolean appendStringField(StringBuilder sb, boolean first, String name, String value){
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

  private boolean appendNumberField(StringBuilder sb, boolean first, String name, BigDecimal value){
    if (!first){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append(value.toPlainString());
    }
    return false;
  }

  private String escape(String value){
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < value.length(); i++){
      char c = value.charAt(i);
      if (c == '"' || c == '\\'){
        out.append('\\');
      }
      out.append(c);
    }
    return out.toString();
  }
}
