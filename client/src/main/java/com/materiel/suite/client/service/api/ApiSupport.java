package com.materiel.suite.client.service.api;

import com.materiel.suite.client.model.*;
import com.materiel.suite.client.net.SimpleJson;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

/**
 * Utilitaires de mapping JSON <-> modèles client (réflexion défensive).
 * Tolérant aux variations (qty vs quantity, totalHt vs totalHT, etc.).
 */
final class ApiSupport {
  private ApiSupport(){}

  /* ================= JSON → MODEL ================= */
  static Quote toQuote(Map<String,Object> m){
    Quote q = new Quote();
    q.setId(parseUUID(m.get("id")));
    q.setNumber(SimpleJson.str(m.get("number")));
    q.setCustomerName(SimpleJson.str(m.get("customerName")));
    setAgency(q, m);
    // === CRM-INJECT BEGIN: quote-client-mapping ===
    q.setClientId(parseUUID(m.get("clientId")));
    q.setContactId(parseUUID(m.get("contactId")));
    // === CRM-INJECT END ===
    setStatusIfPresent(q, m.get("status"));
    setIfPresent(q, "setVersion", m.get("version"));
    setIfPresent(q, "setUpdatedAt", parseInstant(m.get("updatedAt")));
    // totals
    setIfPresent(q, List.of("setTotalHt","setTotalHT"), m.get("totalHt"));
    setIfPresent(q, List.of("setTotalVat","setTotalVAT"), m.get("totalVat"));
    setIfPresent(q, List.of("setTotalTtc","setTotalTTC"), m.get("totalTtc"));
    // lines
    List<Object> arr = SimpleJson.asArr(m.getOrDefault("lines", List.of()));
    List<DocumentLine> out = new ArrayList<>();
    for (Object o : arr){ out.add(toLine(SimpleJson.asObj(o))); }
    try { q.setLines(out); } catch(Exception ignore){}
    return q;
  }

  static Order toOrder(Map<String,Object> m){
    Order o = new Order();
    o.setId(parseUUID(m.get("id")));
    setIfPresent(o, "setNumber", m.get("number"));
    setIfPresent(o, "setCustomerName", m.get("customerName"));
    setAgency(o, m);
    // === CRM-INJECT BEGIN: order-client-mapping ===
    setIfPresent(o, "setClientId", parseUUID(m.get("clientId")));
    setIfPresent(o, "setContactId", parseUUID(m.get("contactId")));
    // === CRM-INJECT END ===
    setStatusIfPresent(o, m.get("status"));
    setIfPresent(o, "setVersion", m.get("version"));
    setIfPresent(o, "setUpdatedAt", parseInstant(m.get("updatedAt")));
    setTotals(o, m);
    setLines(o, m);
    return o;
  }

  static DeliveryNote toDelivery(Map<String,Object> m){
    DeliveryNote d = new DeliveryNote();
    d.setId(parseUUID(m.get("id")));
    setIfPresent(d, "setNumber", m.get("number"));
    setIfPresent(d, "setCustomerName", m.get("customerName"));
    setAgency(d, m);
    // === CRM-INJECT BEGIN: delivery-client-mapping ===
    setIfPresent(d, "setClientId", parseUUID(m.get("clientId")));
    setIfPresent(d, "setContactId", parseUUID(m.get("contactId")));
    // === CRM-INJECT END ===
    setStatusIfPresent(d, m.get("status"));
    setIfPresent(d, "setVersion", m.get("version"));
    setIfPresent(d, "setUpdatedAt", parseInstant(m.get("updatedAt")));
    setTotals(d, m);
    setLines(d, m);
    return d;
  }

  static Invoice toInvoice(Map<String,Object> m){
    Invoice i = new Invoice();
    i.setId(parseUUID(m.get("id")));
    setIfPresent(i, "setNumber", m.get("number"));
    setIfPresent(i, "setCustomerName", m.get("customerName"));
    setAgency(i, m);
    // === CRM-INJECT BEGIN: invoice-client-mapping ===
    setIfPresent(i, "setClientId", parseUUID(m.get("clientId")));
    setIfPresent(i, "setContactId", parseUUID(m.get("contactId")));
    // === CRM-INJECT END ===
    setStatusIfPresent(i, m.get("status"));
    setIfPresent(i, "setVersion", m.get("version"));
    setIfPresent(i, "setUpdatedAt", parseInstant(m.get("updatedAt")));
    setTotals(i, m);
    setLines(i, m);
    return i;
  }

  static void setTotals(Object doc, Map<String,Object> m){
    setIfPresent(doc, List.of("setTotalHt","setTotalHT"), m.get("totalHt"));
    setIfPresent(doc, List.of("setTotalVat","setTotalVAT"), m.get("totalVat"));
    setIfPresent(doc, List.of("setTotalTtc","setTotalTTC"), m.get("totalTtc"));
  }

  static void setLines(Object doc, Map<String,Object> m){
    List<Object> arr = SimpleJson.asArr(m.getOrDefault("lines", List.of()));
    List<DocumentLine> out = new ArrayList<>();
    for (Object o : arr){ out.add(toLine(SimpleJson.asObj(o))); }
    try {
      Method setter = doc.getClass().getMethod("setLines", List.class);
      setter.invoke(doc, out);
    } catch(Exception ignore){}
  }

  static DocumentLine toLine(Map<String,Object> m){
    DocumentLine l = new DocumentLine();
    setIfPresent(l, "setDesignation", m.get("designation"));
    // qty / quantity
    Object qty = m.get("qty");
    if (qty==null) qty = m.get("quantity");
    setIfPresent(l, List.of("setQty","setQuantity"), qty);
    setIfPresent(l, List.of("setUnit","setUnite"), m.get("unit"));
    setIfPresent(l, List.of("setUnitPrice","setUnitPriceHt","setPuHt"), m.get("unitPrice"));
    setIfPresent(l, List.of("setDiscountPct","setRemisePct"), m.get("discountPct"));
    setIfPresent(l, List.of("setVatPct","setTvaPct"), m.get("vatPct"));
    // computed may exist
    setIfPresent(l, List.of("setLineHt","setLigneHt"), m.get("lineHt"));
    setIfPresent(l, List.of("setLineVat","setLigneTva"), m.get("lineVat"));
    setIfPresent(l, List.of("setLineTtc","setLigneTtc"), m.get("lineTtc"));
    return l;
  }

  /* ================= MODEL → JSON ================= */
  static String toJson(Quote q){
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    field(sb,"id", q.getId()==null? null : q.getId().toString()); comma(sb);
    field(sb,"number", q.getNumber()); comma(sb);
    field(sb,"agencyId", readString(q,"getAgencyId","getAgency","getAgencyCode")); comma(sb);
    // === CRM-INJECT BEGIN: quote-client-json ===
    field(sb,"clientId", q.getClientId()==null? null : q.getClientId().toString()); comma(sb);
    field(sb,"contactId", q.getContactId()==null? null : q.getContactId().toString()); comma(sb);
    // === CRM-INJECT END ===
    field(sb,"customerName", q.getCustomerName()); comma(sb);
    field(sb,"status", q.getStatus()==null? null : q.getStatus().toString()); comma(sb);
    numeric(sb,"version", readNumber(q,"getVersion")); comma(sb);
    // lines
    sb.append("\"lines\":"); linesArray(sb, q.getLines()); comma(sb);
    // optional totals
    numeric(sb,"totalHt", readNumber(q,"getTotalHt","getTotalHT")); comma(sb);
    numeric(sb,"totalVat", readNumber(q,"getTotalVat","getTotalVAT")); comma(sb);
    numeric(sb,"totalTtc", readNumber(q,"getTotalTtc","getTotalTTC"));
    sb.append("}");
    return sb.toString();
  }

  static String toJson(Order o){
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    field(sb,"id", o.getId()==null? null : o.getId().toString()); comma(sb);
    field(sb,"number", o.getNumber()); comma(sb);
    field(sb,"agencyId", readString(o,"getAgencyId","getAgency","getAgencyCode")); comma(sb);
    // === CRM-INJECT BEGIN: order-client-json ===
    field(sb,"clientId", o.getClientId()==null? null : o.getClientId().toString()); comma(sb);
    field(sb,"contactId", o.getContactId()==null? null : o.getContactId().toString()); comma(sb);
    // === CRM-INJECT END ===
    field(sb,"customerName", o.getCustomerName()); comma(sb);
    field(sb,"status", o.getStatus()==null? null : o.getStatus().toString()); comma(sb);
    numeric(sb,"version", readNumber(o,"getVersion")); comma(sb);
    sb.append("\"lines\":"); linesArray(sb, o.getLines()); comma(sb);
    numeric(sb,"totalHt", readNumber(o,"getTotalHt","getTotalHT")); comma(sb);
    numeric(sb,"totalVat", readNumber(o,"getTotalVat","getTotalVAT")); comma(sb);
    numeric(sb,"totalTtc", readNumber(o,"getTotalTtc","getTotalTTC"));
    sb.append("}");
    return sb.toString();
  }

  static String toJson(DeliveryNote d){
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    field(sb,"id", d.getId()==null? null : d.getId().toString()); comma(sb);
    field(sb,"number", d.getNumber()); comma(sb);
    field(sb,"agencyId", readString(d,"getAgencyId","getAgency","getAgencyCode")); comma(sb);
    // === CRM-INJECT BEGIN: delivery-client-json ===
    field(sb,"clientId", d.getClientId()==null? null : d.getClientId().toString()); comma(sb);
    field(sb,"contactId", d.getContactId()==null? null : d.getContactId().toString()); comma(sb);
    // === CRM-INJECT END ===
    field(sb,"customerName", d.getCustomerName()); comma(sb);
    field(sb,"status", d.getStatus()==null? null : d.getStatus().toString()); comma(sb);
    numeric(sb,"version", readNumber(d,"getVersion")); comma(sb);
    sb.append("\"lines\":"); linesArray(sb, d.getLines()); comma(sb);
    numeric(sb,"totalHt", readNumber(d,"getTotalHt","getTotalHT")); comma(sb);
    numeric(sb,"totalVat", readNumber(d,"getTotalVat","getTotalVAT")); comma(sb);
    numeric(sb,"totalTtc", readNumber(d,"getTotalTtc","getTotalTTC"));
    sb.append("}");
    return sb.toString();
  }

  static String toJson(Invoice i){
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    field(sb,"id", i.getId()==null? null : i.getId().toString()); comma(sb);
    field(sb,"number", i.getNumber()); comma(sb);
    field(sb,"agencyId", readString(i,"getAgencyId","getAgency","getAgencyCode")); comma(sb);
    // === CRM-INJECT BEGIN: invoice-client-json ===
    field(sb,"clientId", i.getClientId()==null? null : i.getClientId().toString()); comma(sb);
    field(sb,"contactId", i.getContactId()==null? null : i.getContactId().toString()); comma(sb);
    // === CRM-INJECT END ===
    field(sb,"customerName", i.getCustomerName()); comma(sb);
    field(sb,"status", i.getStatus()==null? null : i.getStatus().toString()); comma(sb);
    numeric(sb,"version", readNumber(i,"getVersion")); comma(sb);
    sb.append("\"lines\":"); linesArray(sb, i.getLines()); comma(sb);
    numeric(sb,"totalHt", readNumber(i,"getTotalHt","getTotalHT")); comma(sb);
    numeric(sb,"totalVat", readNumber(i,"getTotalVat","getTotalVAT")); comma(sb);
    numeric(sb,"totalTtc", readNumber(i,"getTotalTtc","getTotalTTC"));
    sb.append("}");
    return sb.toString();
  }

  private static void linesArray(StringBuilder sb, List<DocumentLine> lines){
    sb.append("[");
    if (lines!=null){
      for (int i=0;i<lines.size();i++){
        if (i>0) sb.append(",");
        sb.append(toJson(lines.get(i)));
      }
    }
    sb.append("]");
  }

  private static String toJson(DocumentLine l){
    StringBuilder sb = new StringBuilder("{");
    field(sb,"designation", readString(l,"getDesignation")); comma(sb);
    numeric(sb,"qty", readNumber(l,"getQty","getQuantity")); comma(sb);
    field(sb,"unit", readString(l,"getUnit","getUnite")); comma(sb);
    numeric(sb,"unitPrice", readNumber(l,"getUnitPrice","getUnitPriceHt","getPuHt")); comma(sb);
    numeric(sb,"discountPct", readNumber(l,"getDiscountPct","getRemisePct")); comma(sb);
    numeric(sb,"vatPct", readNumber(l,"getVatPct","getTvaPct"));
    sb.append("}");
    return sb.toString();
  }

  /* ================= helpers ================= */
  private static void setAgency(Object target, Map<String,Object> data){
    if (target == null || data == null){
      return;
    }
    Object value = firstNonBlank(data.get("agencyId"), data.get("agency"), data.get("agencyCode"));
    if (value != null){
      setIfPresent(target, "setAgencyId", value);
    }
  }

  private static Object firstNonBlank(Object... values){
    if (values == null){
      return null;
    }
    for (Object value : values){
      if (value == null){
        continue;
      }
      if (value instanceof CharSequence sequence && sequence.toString().isBlank()){
        continue;
      }
      return value;
    }
    return null;
  }

  private static void field(StringBuilder sb, String k, String v){
    sb.append("\"").append(k).append("\":");
    if (v==null) sb.append("null");
    else sb.append("\"").append(escape(v)).append("\"");
  }

  private static void numeric(StringBuilder sb, String k, Number v){
    sb.append("\"").append(k).append("\":");
    if (v==null) sb.append("null"); else sb.append(v.toString());
  }

  private static void comma(StringBuilder sb){ sb.append(","); }

  private static String escape(String s){ return s.replace("\\","\\\\").replace("\"","\\\""); }

  private static UUID parseUUID(Object o){
    try { return o==null? null : UUID.fromString(o.toString()); } catch(Exception e){ return null; }
  }

  private static Date parseInstant(Object o){
    try { if (o==null) return null; return Date.from(Instant.parse(o.toString())); } catch(Exception e){ return null; }
  }

  private static void setIfPresent(Object target, String method, Object value){
    setIfPresent(target, List.of(method), value);
  }

  private static void setIfPresent(Object target, List<String> methods, Object value){
    for (String m : methods){
      try {
        Method[] mm = target.getClass().getMethods();
        for (Method cand : mm){
          if (cand.getName().equals(m) && cand.getParameterCount()==1){
            Class<?> pt = cand.getParameterTypes()[0];
            Object coerced = coerce(value, pt);
            if (coerced!=null || !pt.isPrimitive()){
              cand.invoke(target, coerced);
              return;
            }
          }
        }
      } catch(Exception ignore){}
    }
  }

  private static void setStatusIfPresent(Object target, Object value){
    if (value==null) return;
    setIfPresent(target, "setStatus", value);
  }

  private static String readString(Object o, String... getters){
    for (String g : getters){
      try {
        Method m = o.getClass().getMethod(g);
        Object v = m.invoke(o);
        if (v!=null) return v.toString();
      } catch(Exception ignore){}
    }
    return null;
  }

  private static Number readNumber(Object o, String... getters){
    for (String g : getters){
      try {
        Method m = o.getClass().getMethod(g);
        Object v = m.invoke(o);
        if (v instanceof Number n) return n;
        if (v!=null) return Double.parseDouble(v.toString());
      } catch(Exception ignore){}
    }
    return null;
  }

  private static Object coerce(Object v, Class<?> pt){
    if (v==null) return null;
    if (pt.isAssignableFrom(v.getClass())) return v;
    try {
      if (pt==String.class) return v.toString();
      if (pt==int.class || pt==Integer.class) return (v instanceof Number n)? n.intValue() : Integer.parseInt(v.toString());
      if (pt==long.class || pt==Long.class) return (v instanceof Number n)? n.longValue() : Long.parseLong(v.toString());
      if (pt==double.class || pt==Double.class) return (v instanceof Number n)? n.doubleValue() : Double.parseDouble(v.toString());
      if (pt==java.util.UUID.class) return UUID.fromString(v.toString());
      if (pt==java.util.Date.class) return parseInstant(v);
      if (pt.isEnum()) return Enum.valueOf((Class<Enum>)pt.asSubclass(Enum.class), v.toString());
    } catch(Exception ignore){}
    return null;
  }
}

