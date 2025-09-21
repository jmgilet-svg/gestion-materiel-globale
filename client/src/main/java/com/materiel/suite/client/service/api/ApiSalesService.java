package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.net.RestClient;
import com.materiel.suite.client.net.SimpleJson;
import com.materiel.suite.client.service.SalesService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
      String body = rc.get("/api/v2/quotes/" + encode(id));
      return parseQuote(body);
    } catch (Exception e){
      return fallback.getQuote(id);
    }
  }

  @Override public List<QuoteV2> listQuotes(){
    try {
      String body = rc.get("/api/v2/quotes");
      return parseQuoteList(body);
    } catch (Exception e){
      return fallback.listQuotes();
    }
  }

  @Override public QuoteV2 saveQuote(QuoteV2 quote){
    if (quote == null){
      return null;
    }
    try {
      String payload = quoteToJson(quote);
      String response;
      if (quote.getId() == null || quote.getId().isBlank()){
        response = rc.post("/api/v2/quotes", payload);
      } else {
        response = rc.put("/api/v2/quotes", payload);
      }
      return parseQuote(response);
    } catch (Exception e){
      return fallback.saveQuote(quote);
    }
  }

  @Override public void deleteQuote(String id){
    if (id == null || id.isBlank()){
      return;
    }
    try {
      rc.delete("/api/v2/quotes/" + encode(id));
    } catch (Exception ignore){
    }
    fallback.deleteQuote(id);
  }

  @Override public List<InvoiceV2> listInvoices(){
    try {
      String body = rc.get("/api/v2/invoices");
      return parseInvoiceList(body);
    } catch (Exception e){
      return fallback.listInvoices();
    }
  }

  @Override public InvoiceV2 saveInvoice(InvoiceV2 invoice){
    if (invoice == null){
      return null;
    }
    try {
      String payload = invoiceToJson(invoice);
      String response;
      if (invoice.getId() == null || invoice.getId().isBlank()){
        response = rc.post("/api/v2/invoices", payload);
      } else {
        response = rc.put("/api/v2/invoices", payload);
      }
      return parseInvoice(response);
    } catch (Exception e){
      return fallback.saveInvoice(invoice);
    }
  }

  @Override public void deleteInvoice(String id){
    if (id == null || id.isBlank()){
      return;
    }
    try {
      rc.delete("/api/v2/invoices/" + encode(id));
    } catch (Exception ignore){
    }
    fallback.deleteInvoice(id);
  }

  private List<QuoteV2> parseQuoteList(String body){
    if (body == null || body.isBlank()){
      return List.of();
    }
    List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
    List<QuoteV2> out = new ArrayList<>();
    for (Object element : arr){
      out.add(parseQuote(SimpleJson.asObj(element)));
    }
    return out;
  }

  private List<InvoiceV2> parseInvoiceList(String body){
    if (body == null || body.isBlank()){
      return List.of();
    }
    List<Object> arr = SimpleJson.asArr(SimpleJson.parse(body));
    List<InvoiceV2> out = new ArrayList<>();
    for (Object element : arr){
      out.add(parseInvoice(SimpleJson.asObj(element)));
    }
    return out;
  }

  private QuoteV2 parseQuote(String body){
    if (body == null || body.isBlank()){
      return null;
    }
    return parseQuote(SimpleJson.asObj(SimpleJson.parse(body)));
  }

  private QuoteV2 parseQuote(Map<String, Object> map){
    QuoteV2 quote = new QuoteV2();
    quote.setId(SimpleJson.str(map.get("id")));
    quote.setReference(SimpleJson.str(map.get("reference")));
    quote.setClientId(SimpleJson.str(map.get("clientId")));
    quote.setClientName(SimpleJson.str(map.get("clientName")));
    quote.setDate(parseLocalDate(map.get("date")));
    quote.setStatus(SimpleJson.str(map.get("status")));
    quote.setTotalHt(parseBigDecimal(map.get("totalHt")));
    quote.setTotalTtc(parseBigDecimal(map.get("totalTtc")));
    Object sent = map.get("sent");
    quote.setSent(sent == null ? null : SimpleJson.bool(sent));
    quote.setAgencyId(SimpleJson.str(map.get("agencyId")));
    Object lines = map.get("lines");
    if (lines instanceof List<?> list){
      quote.setLines(new ArrayList<>(list));
    }
    return quote;
  }

  private InvoiceV2 parseInvoice(String body){
    if (body == null || body.isBlank()){
      return null;
    }
    return parseInvoice(SimpleJson.asObj(SimpleJson.parse(body)));
  }

  private InvoiceV2 parseInvoice(Map<String, Object> map){
    InvoiceV2 invoice = new InvoiceV2();
    invoice.setId(SimpleJson.str(map.get("id")));
    invoice.setNumber(SimpleJson.str(map.get("number")));
    invoice.setClientId(SimpleJson.str(map.get("clientId")));
    invoice.setClientName(SimpleJson.str(map.get("clientName")));
    invoice.setDate(parseLocalDate(map.get("date")));
    invoice.setTotalHt(parseBigDecimal(map.get("totalHt")));
    invoice.setTotalTtc(parseBigDecimal(map.get("totalTtc")));
    invoice.setStatus(SimpleJson.str(map.get("status")));
    invoice.setAgencyId(SimpleJson.str(map.get("agencyId")));
    Object lines = map.get("lines");
    if (lines instanceof List<?> list){
      invoice.setLines(new ArrayList<>(list));
    }
    return invoice;
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

  private LocalDate parseLocalDate(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof LocalDate localDate){
      return localDate;
    }
    if (value instanceof java.util.Date date){
      return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    if (value instanceof Number number){
      return Instant.ofEpochMilli(number.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    if (value instanceof String text){
      if (text.isBlank()){
        return null;
      }
      try {
        return LocalDate.parse(text);
      } catch (DateTimeParseException ignore){
        return null;
      }
    }
    return null;
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
    firstField = appendStringField(sb, firstField, "agencyId", intervention.getAgencyId());
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

  private String quoteToJson(QuoteV2 quote){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    first = appendStringField(sb, first, "id", quote.getId());
    first = appendStringField(sb, first, "reference", quote.getReference());
    first = appendStringField(sb, first, "clientId", quote.getClientId());
    first = appendStringField(sb, first, "clientName", quote.getClientName());
    first = appendStringField(sb, first, "date", quote.getDate() == null ? null : quote.getDate().toString());
    first = appendStringField(sb, first, "status", quote.getStatus());
    first = appendNumberField(sb, first, "totalHt", quote.getTotalHt());
    first = appendNumberField(sb, first, "totalTtc", quote.getTotalTtc());
    first = appendBooleanField(sb, first, "sent", quote.getSent());
    first = appendStringField(sb, first, "agencyId", quote.getAgencyId());
    sb.append('}');
    return sb.toString();
  }

  private String invoiceToJson(InvoiceV2 invoice){
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    first = appendStringField(sb, first, "id", invoice.getId());
    first = appendStringField(sb, first, "number", invoice.getNumber());
    first = appendStringField(sb, first, "clientId", invoice.getClientId());
    first = appendStringField(sb, first, "clientName", invoice.getClientName());
    first = appendStringField(sb, first, "date", invoice.getDate() == null ? null : invoice.getDate().toString());
    first = appendNumberField(sb, first, "totalHt", invoice.getTotalHt());
    first = appendNumberField(sb, first, "totalTtc", invoice.getTotalTtc());
    first = appendStringField(sb, first, "status", invoice.getStatus());
    first = appendStringField(sb, first, "agencyId", invoice.getAgencyId());
    sb.append('}');
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

  private boolean appendNumberField(StringBuilder sb, boolean first, String name, Number value){
    if (!first){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else if (value instanceof BigDecimal bd){
      sb.append(bd.toPlainString());
    } else {
      sb.append(value.toString());
    }
    return false;
  }

  private boolean appendBooleanField(StringBuilder sb, boolean first, String name, Boolean value){
    if (!first){
      sb.append(',');
    }
    sb.append('"').append(name).append('"').append(':');
    if (value == null){
      sb.append("null");
    } else {
      sb.append(value.booleanValue());
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

  private String encode(String value){
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
