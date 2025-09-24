package com.materiel.suite.client.ui.sales;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.service.AgencyConfigGateway;
import com.materiel.suite.client.service.DocumentTemplateService;
import com.materiel.suite.client.service.PdfService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplatesGateway;
import com.materiel.suite.client.settings.GeneralSettings;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fusion très simple {{var}} + appel backend HTML->PDF. */
public final class PdfTemplateEngine {
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final Pattern PARTIAL_PATTERN = Pattern.compile("\\{\\{>partial:([a-zA-Z0-9._-]+)\\}\\}");

  private PdfTemplateEngine(){
  }

  public static byte[] renderQuote(QuoteV2 quote, String logoBase64){
    String html = loadTemplate("QUOTE", "default", defaultQuoteTemplate());
    html = applyPartials(html);
    Map<String, String> values = new LinkedHashMap<>();
    populateAgency(values);
    values.put("client.name", nz(quote == null ? null : quote.getClientName()));
    values.put("client.addressHtml", "");
    values.put("quote.reference", nz(quote == null ? null : quote.getReference()));
    values.put("quote.date", formatDate(quote == null ? null : quote.getDate()));
    values.put("quote.totalHt", formatAmount(quote == null ? null : quote.getTotalHt()));
    values.put("quote.totalTtc", formatAmount(quote == null ? null : quote.getTotalTtc()));
    values.put("lines.rows", EmailTableBuilder.rowsHtml(quote == null ? null : quote.getLines()));
    values.put("lines.tableHtml", EmailTableBuilder.tableHtml(quote == null ? null : quote.getLines()));
    html = merge(html, values);
    html = applyPartials(html);
    html = html.replace("{{logo.cdi}}", logoBase64 == null ? "" : "cid:logo");

    return renderHtml(html, logoBase64);
  }

  public static byte[] renderInvoice(InvoiceV2 invoice, String logoBase64){
    String html = loadTemplate("INVOICE", "default", defaultInvoiceTemplate());
    html = applyPartials(html);
    Map<String, String> values = new LinkedHashMap<>();
    populateAgency(values);
    values.put("client.name", nz(invoice == null ? null : invoice.getClientName()));
    values.put("client.addressHtml", "");
    values.put("invoice.number", nz(invoice == null ? null : nz(invoice.getNumber(), invoice.getId())));
    values.put("invoice.date", formatDate(invoice == null ? null : invoice.getDate()));
    values.put("invoice.totalHt", formatAmount(invoice == null ? null : invoice.getTotalHt()));
    values.put("invoice.totalTtc", formatAmount(invoice == null ? null : invoice.getTotalTtc()));
    values.put("invoice.status", nz(invoice == null ? null : invoice.getStatus()));
    values.put("lines.rows", EmailTableBuilder.rowsHtml(invoice == null ? null : invoice.getLines()));
    values.put("lines.tableHtml", EmailTableBuilder.tableHtml(invoice == null ? null : invoice.getLines()));
    html = merge(html, values);
    html = applyPartials(html);
    html = html.replace("{{logo.cdi}}", logoBase64 == null ? "" : "cid:logo");

    return renderHtml(html, logoBase64);
  }

  /** Aperçu brut d'un HTML saisi manuellement (sans image inline). */
  public static byte[] renderHtmlForPreview(String html, String baseUrl){
    PdfService svc = ServiceLocator.pdf();
    if (svc == null){
      throw new IllegalStateException("Service PDF indisponible");
    }
    String safeHtml = html == null ? "" : html;
    safeHtml = applyPartials(safeHtml);
    return svc.render(safeHtml, Map.of(), baseUrl);
  }

  private static String loadTemplate(String type, String key, String fallback){
    String content = null;
    DocumentTemplateService svc = ServiceLocator.documentTemplates();
    if (svc != null){
      try {
        List<DocumentTemplateService.Template> list = svc.list(type);
        if (list != null && !list.isEmpty()){
          String preferredKey = key == null || key.isBlank() ? "default" : key;
          for (DocumentTemplateService.Template template : list){
            if (template.getKey() != null && template.getKey().equalsIgnoreCase(preferredKey)){
              content = template.getContent();
              break;
            }
          }
          if ((content == null || content.isBlank()) && !"default".equalsIgnoreCase(preferredKey)){
            for (DocumentTemplateService.Template template : list){
              if (template.getKey() != null && template.getKey().equalsIgnoreCase("default")){
                content = template.getContent();
                break;
              }
            }
          }
          if (content == null || content.isBlank()){
            for (DocumentTemplateService.Template template : list){
              if (template.getContent() != null && !template.getContent().isBlank()){
                content = template.getContent();
                break;
              }
            }
          }
        }
      } catch (Exception ignore){
        content = null;
      }
    }
    if (content == null || content.isBlank()){
      content = fallback;
    }
    return applyPartials(content);
  }

  public static String merge(String template, Map<String, String> values){
    String withPartials = applyPartials(template);
    String merged = mergeValues(withPartials, values);
    return enrichAssetsAndMacros(merged, null);
  }

  private static String mergeValues(String template, Map<String, String> values){
    String out = template == null ? "" : template;
    if (values == null || values.isEmpty()){
      return out;
    }
    for (Map.Entry<String, String> entry : values.entrySet()){
      String key = entry.getKey();
      if (key == null){
        continue;
      }
      String value = entry.getValue() == null ? "" : entry.getValue();
      out = out.replace("{{" + key + "}}", value);
    }
    return out;
  }

  private static String applyPartials(String html){
    if (html == null){
      return "";
    }
    Matcher matcher = PARTIAL_PATTERN.matcher(html);
    if (!matcher.find()){
      return html;
    }
    Map<String, String> partials = loadPartials();
    matcher.reset();
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()){
      String key = matcher.group(1);
      String replacement = partials.getOrDefault(key == null ? "" : key.toLowerCase(Locale.ROOT), "");
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private static Map<String, String> loadPartials(){
    TemplatesGateway gateway = ServiceLocator.templates();
    Map<String, String> partials = new HashMap<>();
    if (gateway == null){
      return partials;
    }
    try {
      List<TemplatesGateway.Template> list = gateway.list("PARTIAL");
      for (TemplatesGateway.Template template : list){
        if (template != null && template.key() != null){
          String key = template.key().toLowerCase(Locale.ROOT);
          String value = template.content() == null ? "" : template.content();
          partials.put(key, value);
        }
      }
    } catch (Exception ignore){
      // ignore errors and keep partials empty
    }
    return partials;
  }

  private static String enrichAssetsAndMacros(String html, String logoBase64){
    String out = html == null ? "" : html;
    String logoValue = logoBase64 == null || logoBase64.isBlank() ? "" : "cid:logo";
    out = out.replace("{{logo.cdi}}", logoValue);
    out = replaceAssetTokens(out);
    out = replaceQrTokens(out);
    return out;
  }

  private static String applyPartials(String html){
    String safe = html == null ? "" : html;
    if (safe.isBlank() || !safe.contains("{{>partial:")){
      return safe;
    }
    Map<String, String> partials = loadPartialContents();
    GeneralSettings general = loadGeneralSettings();
    String fallbackCgv = fallbackCgvHtml(general);
    String current = safe;
    for (int depth = 0; depth < 5; depth++){
      Matcher matcher = PARTIAL_PATTERN.matcher(current);
      if (!matcher.find()){
        break;
      }
      StringBuffer sb = new StringBuffer();
      do {
        String key = matcher.group(1);
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        String content = partials.getOrDefault(normalized, "");
        if (content.isBlank() && "cgv".equalsIgnoreCase(key)){
          content = fallbackCgv;
        }
        matcher.appendReplacement(sb, Matcher.quoteReplacement(content));
      } while (matcher.find());
      matcher.appendTail(sb);
      String replaced = sb.toString();
      if (replaced.equals(current)){
        break;
      }
      current = replaced;
    }
    return current;
  }

  private static Map<String, String> loadPartialContents(){
    TemplatesGateway gateway = ServiceLocator.templates();
    if (gateway == null){
      return Collections.emptyMap();
    }
    try {
      List<TemplatesGateway.Template> list = gateway.list("PARTIAL");
      if (list == null || list.isEmpty()){
        return Collections.emptyMap();
      }
      Map<String, String> map = new HashMap<>();
      for (TemplatesGateway.Template template : list){
        if (template == null){
          continue;
        }
        String key = template.key();
        if (key == null || key.isBlank()){
          continue;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        map.put(normalized, template.content() == null ? "" : template.content());
      }
      return map;
    } catch (Exception ignore){
      return Collections.emptyMap();
    }
  }

  private static void populateAgency(Map<String, String> values){
    values.put("agency.name", nz(ServiceLocator.agencyLabel()));
    values.put("agency.addressHtml", "");
    values.put("agency.vatRate", "");
    values.put("agency.cgvHtml", "");
    values.put("agency.emailCss", "");
    values.put("agency.emailSignatureHtml", "");
    AgencyConfigGateway gateway = ServiceLocator.agencyConfig();
    if (gateway != null){
      try {
        AgencyConfigGateway.AgencyConfig cfg = gateway.get();
        if (cfg != null){
          if (cfg.companyName() != null && !cfg.companyName().isBlank()){
            values.put("agency.name", cfg.companyName());
          }
          values.put("agency.addressHtml", nz(cfg.companyAddressHtml()));
          values.put("agency.vatRate", cfg.vatRate() == null ? "" : cfg.vatRate().toString());
          values.put("agency.cgvHtml", nz(cfg.cgvHtml()));
          values.put("agency.emailCss", nz(cfg.emailCss()));
          values.put("agency.emailSignatureHtml", nz(cfg.emailSignatureHtml()));
        }
      } catch (Exception ignore){
        // valeurs par défaut déjà renseignées
      }
    }
    GeneralSettings general = loadGeneralSettings();
    if (general != null){
      String vat = values.get("agency.vatRate");
      String fallbackVat = fallbackVatRate(general);
      if ((vat == null || vat.isBlank()) && !fallbackVat.isBlank()){
        values.put("agency.vatRate", fallbackVat);
      }
      String cgv = values.get("agency.cgvHtml");
      String fallbackCgv = fallbackCgvHtml(general);
      if ((cgv == null || cgv.isBlank()) && !fallbackCgv.isBlank()){
        values.put("agency.cgvHtml", fallbackCgv);
      }
    }
  }

  private static GeneralSettings loadGeneralSettings(){
    try {
      return ServiceLocator.settings().getGeneral();
    } catch (RuntimeException ex){
      return null;
    }
  }

  private static String fallbackVatRate(GeneralSettings settings){
    if (settings == null){
      return "";
    }
    Double percent = settings.getDefaultVatPercent();
    if (percent == null){
      return "";
    }
    BigDecimal rate = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
    return stripTrailingZeros(rate);
  }

  private static String fallbackCgvHtml(GeneralSettings settings){
    if (settings == null){
      return "";
    }
    String text = settings.getCgvText();
    if (text == null){
      return "";
    }
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    if (normalized.trim().isEmpty()){
      return "";
    }
    return normalized.trim().replace("\n", "<br>");
  }

  private static String stripTrailingZeros(BigDecimal value){
    if (value == null){
      return "";
    }
    return value.stripTrailingZeros().toPlainString();
  }

  private static byte[] renderHtml(String html, String logoBase64){
    PdfService svc = ServiceLocator.pdf();
    if (svc == null){
      throw new IllegalStateException("Service PDF indisponible");
    }
    Map<String, String> images = (logoBase64 == null || logoBase64.isBlank())
        ? Map.of()
        : Map.of("logo", logoBase64);
    return svc.render(html, images, null);
  }

  private static String vatPercent(){
    Double agencyRate = null;
    AgencyConfigGateway gateway = ServiceLocator.agencyConfig();
    if (gateway != null){
      try {
        AgencyConfigGateway.AgencyConfig cfg = gateway.get();
        if (cfg != null){
          agencyRate = cfg.vatRate();
        }
      } catch (Exception ignore){
        agencyRate = null;
      }
    }
    double percent = agencyRate != null ? agencyRate : Money.vatPercent().doubleValue();
    if (percent <= 0){
      return "";
    }
    double rounded = Math.rint(percent);
    if (Math.abs(percent - rounded) < 0.01){
      return String.format(Locale.ROOT, "%.0f%%", rounded);
    }
    return String.format(Locale.ROOT, "%.2f%%", percent);
  }

  private static String formatAmount(BigDecimal amount){
    if (amount == null){
      return "0.00";
    }
    return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private static String formatDate(LocalDate date){
    return date == null ? "" : DATE_FMT.format(date);
  }

  private static String nz(String value){
    return value == null ? "" : value;
  }

  private static String nz(String value, String fallback){
    if (value == null || value.isBlank()){
      return fallback == null ? "" : fallback;
    }
    return value;
  }

  private static String defaultQuoteTemplate(){
    return loadDefaultTemplate("/templates/_default-quote.html", legacyQuoteTemplate());
  }

  private static String defaultInvoiceTemplate(){
    return loadDefaultTemplate("/templates/_default-invoice.html", legacyInvoiceTemplate());
  }

  private static String loadDefaultTemplate(String resourcePath, String legacy){
    String fromResource = loadResource(resourcePath);
    if (fromResource != null && !fromResource.isBlank()){
      return fromResource;
    }
    return legacy;
  }

  private static String loadResource(String path){
    if (path == null){
      return null;
    }
    try (InputStream in = PdfTemplateEngine.class.getResourceAsStream(path)){
      if (in == null){
        return null;
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ignore){
      return null;
    }
  }

  private static String legacyQuoteTemplate(){
    return """
<!DOCTYPE html><html><head><meta charset=\"UTF-8\">
<style>
  body{ font-family: DejaVu Sans, Arial, sans-serif; font-size: 12px; }
  .header{ display:flex; justify-content:space-between; align-items:center; }
  .title{ font-size:20px; font-weight:bold; }
  table{ width:100%; border-collapse:collapse; margin-top:14px;}
  th,td{ border:1px solid #ddd; padding:6px; }
  th{ background:#f5f5f5; text-align:left; }
  .totals{ margin-top:12px; float:right; width:40%; }
  .totals td{ border:none; }
  .cgv{ margin-top:24px; font-size:10px; color:#444;}
</style></head><body>
  <div class=\"header\">
    <div><div class=\"title\">Devis {{quote.reference}}</div><div>Agence: {{agency.name}}</div><div>{{agency.addressHtml}}</div></div>
    <div><img src=\"{{logo.cdi}}\" style=\"height:60px\" /></div>
  </div>
  <div>Client: <b>{{client.name}}</b></div><div>{{client.addressHtml}}</div>
  <div>Date: {{quote.date}}</div>
  <table>
    <thead><tr><th>Désignation</th><th>Qté</th><th>PU HT</th><th>Total HT</th></tr></thead>
    <tbody>
      {{lines.rows}}
    </tbody>
  </table>
  <!-- Variante pour emails: {{lines.tableHtml}} -->
  <table class=\"totals\">
    <tr><td>Total HT</td><td style=\"text-align:right\">{{quote.totalHt}} €</td></tr>
    <tr><td>TVA ({{tax.rate}})</td><td style=\"text-align:right\">{{agency.vatRate}}</td></tr>
    <tr><td>Total TTC</td><td style=\"text-align:right\"><b>{{quote.totalTtc}} €</b></td></tr>
    <tr><td>Net à payer</td><td style=\"text-align:right\"><b>{{amount.netToPay}} €</b></td></tr>
  </table>
  <div class=\"cgv\">{{agency.cgvHtml}}{{>partial:cgv}}</div>
</body></html>
""";
  }

  private static String legacyInvoiceTemplate(){
    return """
<!DOCTYPE html><html><head><meta charset=\"UTF-8\">
<style>
  body{ font-family: DejaVu Sans, Arial, sans-serif; font-size: 12px; }
  .header{ display:flex; justify-content:space-between; align-items:center; }
  .title{ font-size:20px; font-weight:bold; }
  table{ width:100%; border-collapse:collapse; margin-top:14px;}
  th,td{ border:1px solid #ddd; padding:6px; }
  th{ background:#f5f5f5; text-align:left; }
  .totals{ margin-top:12px; float:right; width:40%; }
  .totals td{ border:none; }
  .cgv{ margin-top:24px; font-size:10px; color:#444;}
</style></head><body>
  <div class=\"header\">
    <div><div class=\"title\">Facture {{invoice.number}}</div><div>Agence: {{agency.name}}</div><div>{{agency.addressHtml}}</div></div>
    <div><img src=\"{{logo.cdi}}\" style=\"height:60px\" /></div>
  </div>
  <div>Client: <b>{{client.name}}</b></div><div>{{client.addressHtml}}</div>
  <div>Date: {{invoice.date}}</div>
  <table>
    <thead><tr><th>Désignation</th><th>Qté</th><th>PU HT</th><th>Total HT</th></tr></thead>
    <tbody>
      {{lines.rows}}
    </tbody>
  </table>
  <!-- Variante pour emails: {{lines.tableHtml}} -->
  <table class=\"totals\">
    <tr><td>Total HT</td><td style=\"text-align:right\">{{invoice.totalHt}} €</td></tr>
    <tr><td>TVA ({{tax.rate}})</td><td style=\"text-align:right\">{{agency.vatRate}}</td></tr>
    <tr><td>Total TTC</td><td style=\"text-align:right\"><b>{{invoice.totalTtc}} €</b></td></tr>
    <tr><td>Net à payer</td><td style=\"text-align:right\"><b>{{amount.netToPay}} €</b></td></tr>
    <tr><td>Statut</td><td style=\"text-align:right\">{{invoice.status}}</td></tr>
  </table>
  <div style=\"margin-top:24px\"><img style=\"height:90px\" src=\"{{qr:https://example.local/facture/{{invoice.number}}}}\"/></div>
  <div class=\"cgv\">{{agency.cgvHtml}}{{>partial:cgv}}</div>
</body></html>
""";
  }
}
