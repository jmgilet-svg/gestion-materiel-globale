package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.DocumentLine;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Construit un tableau HTML représentant les lignes d'un document afin de le
 * réutiliser dans les emails ou aperçus HTML.
 */
public final class EmailTableBuilder {
  private EmailTableBuilder(){
  }

  public static String tableHtml(List<?> lines){
    List<Row> rows = extractRows(lines);
    StringBuilder sb = new StringBuilder();
    sb.append("<table style=\"width:100%;border-collapse:collapse\">")
        .append("<thead><tr>")
        .append(th("Désignation"))
        .append(th("Qté"))
        .append(th("PU HT"))
        .append(th("Total HT"))
        .append("</tr></thead><tbody>");
    if (rows.isEmpty()){
      sb.append(emptyRow());
    } else {
      for (Row row : rows){
        sb.append(row.toHtml());
      }
    }
    sb.append("</tbody></table>");
    return sb.toString();
  }

  public static String rowsHtml(List<?> lines){
    List<Row> rows = extractRows(lines);
    if (rows.isEmpty()){
      return emptyRow();
    }
    StringBuilder sb = new StringBuilder();
    for (Row row : rows){
      sb.append(row.toHtml());
    }
    return sb.toString();
  }

  private static List<Row> extractRows(List<?> lines){
    if (lines == null){
      return List.of();
    }
    List<Row> rows = new ArrayList<>();
    for (Object line : lines){
      Row row = Row.from(line);
      if (row != null){
        rows.add(row);
      }
    }
    return rows;
  }

  private static String emptyRow(){
    return "<tr><td colspan=\"4\" style=\"padding:6px;color:#666\">Aucune ligne</td></tr>";
  }

  private static String th(String value){
    return "<th style='border:1px solid #ddd;padding:6px;text-align:left;background:#f7f7f7'>"
        + escape(value) + "</th>";
  }

  private static String td(String value){
    return "<td style='border:1px solid #ddd;padding:6px'>" + value + "</td>";
  }

  private static String tdRight(String value){
    return "<td style='border:1px solid #ddd;padding:6px;text-align:right'>" + escape(value) + "</td>";
  }

  private static String escape(String value){
    if (value == null){
      return "";
    }
    String escaped = value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
    escaped = escaped.replace("\r", "");
    return escaped.replace("\n", "<br/>");
  }

  private static BigDecimal big(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof BigDecimal bd){
      return bd;
    }
    if (value instanceof Number number){
      return new BigDecimal(number.toString());
    }
    if (value instanceof String text){
      String trimmed = text.trim();
      if (trimmed.isEmpty()){
        return null;
      }
      try {
        String normalized = trimmed.replace(',', '.');
        return new BigDecimal(normalized);
      } catch (NumberFormatException ignore){
        return null;
      }
    }
    return null;
  }

  private static String format(BigDecimal value){
    if (value == null){
      return "";
    }
    try {
      return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    } catch (ArithmeticException ex){
      return value.toPlainString();
    }
  }

  private static Object invoke(Object target, String... methods){
    for (String method : methods){
      try {
        Method m = target.getClass().getMethod(method);
        return m.invoke(target);
      } catch (ReflectiveOperationException ignore){
        // Essaye méthode suivante
      }
    }
    return null;
  }

  private static Object mapValue(Map<?, ?> map, String... keys){
    for (String key : keys){
      if (key == null){
        continue;
      }
      Object value = map.get(key);
      if (value == null){
        String lower = key.toLowerCase(Locale.ROOT);
        value = map.get(lower);
      }
      if (value == null){
        String upper = key.toUpperCase(Locale.ROOT);
        value = map.get(upper);
      }
      if (value != null){
        return value;
      }
    }
    return null;
  }

  private record Row(String descriptionHtml, String quantity, String unitPrice, String total) {
    static Row from(Object line){
      if (line == null){
        return null;
      }
      if (line instanceof Row row){
        return row;
      }
      if (line instanceof BillingLine billing){
        String desc = escape(billing.getDesignation());
        BigDecimal qty = billing.getQuantity();
        BigDecimal unit = billing.getUnitPriceHt();
        BigDecimal total = billing.getTotalHt();
        if (total == null && qty != null && unit != null){
          total = qty.multiply(unit);
        }
        return new Row(desc, format(qty), format(unit), format(total));
      }
      if (line instanceof DocumentLine doc){
        String desc = escape(doc.getDesignation());
        BigDecimal qty = BigDecimal.valueOf(doc.getQuantite());
        BigDecimal unit = BigDecimal.valueOf(doc.getPrixUnitaireHT());
        BigDecimal total = BigDecimal.valueOf(doc.lineHT());
        return new Row(desc, format(qty), format(unit), format(total));
      }
      if (line instanceof Map<?, ?> map){
        String desc = escape(asString(mapValue(map, "designation", "description", "label", "name")));
        BigDecimal qty = big(mapValue(map, "quantity", "quantite", "qty"));
        BigDecimal unit = big(mapValue(map, "unitPriceHt", "unitPrice", "prixUnitaireHT", "price"));
        BigDecimal total = big(mapValue(map, "totalHt", "total", "lineHt", "amount"));
        if (total == null && qty != null && unit != null){
          total = qty.multiply(unit);
        }
        if (desc.isEmpty() && qty == null && unit == null && total == null){
          return null;
        }
        return new Row(desc, format(qty), format(unit), format(total));
      }
      String desc = escape(asString(invoke(line, "getDesignation", "getDescription", "getLabel", "getName")));
      BigDecimal qty = big(invoke(line, "getQuantity", "getQuantite", "getQty"));
      BigDecimal unit = big(invoke(line, "getUnitPriceHt", "getUnitPrice", "getPrixUnitaireHT", "getPrice"));
      BigDecimal total = big(invoke(line, "getTotalHt", "getTotal", "getLineHt", "getAmount"));
      if (total == null && qty != null && unit != null){
        total = qty.multiply(unit);
      }
      if (desc.isEmpty()){ // fallback to toString
        String fallback = escape(line.toString());
        if (fallback.isEmpty()){
          return null;
        }
        desc = fallback;
      }
      return new Row(desc, format(qty), format(unit), format(total));
    }

    private static String asString(Object value){
      if (value == null){
        return "";
      }
      String text = value.toString();
      return text == null ? "" : text;
    }

    String toHtml(){
      return "<tr>"
          + td(descriptionHtml)
          + tdRight(quantity == null ? "" : quantity)
          + tdRight(unitPrice == null ? "" : unitPrice)
          + tdRight(total == null ? "" : total)
          + "</tr>";
    }
  }
}
