package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.model.InvoiceV2;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.service.AgencyConfigGateway;
import com.materiel.suite.client.service.DocumentTemplateService;
import com.materiel.suite.client.service.PdfService;
import com.materiel.suite.client.service.ServiceLocator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fusion très simple {{var}} + appel backend HTML->PDF. */
public final class PdfTemplateEngine {
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  private PdfTemplateEngine(){
  }

  public static byte[] renderQuote(QuoteV2 quote, String logoBase64){
    String html = loadTemplate("QUOTE", "default", defaultQuoteTemplate());
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
    html = html.replace("{{logo.cdi}}", logoBase64 == null ? "" : "cid:logo");
    return renderHtml(html, logoBase64);
  }

  public static byte[] renderInvoice(InvoiceV2 invoice, String logoBase64){
    String html = loadTemplate("INVOICE", "default", defaultInvoiceTemplate());
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
    html = html.replace("{{logo.cdi}}", logoBase64 == null ? "" : "cid:logo");
    return renderHtml(html, logoBase64);
  }

  private static String loadTemplate(String type, String key, String fallback){
    DocumentTemplateService svc = ServiceLocator.documentTemplates();
    if (svc != null){
      try {
        List<DocumentTemplateService.Template> list = svc.list(type);
        for (DocumentTemplateService.Template template : list){
          if (template.getKey() != null && template.getKey().equalsIgnoreCase(key)){
            String content = template.getContent();
            if (content != null && !content.isBlank()){
              return content;
            }
          }
        }
      } catch (Exception ignore){
        // fallback below
      }
    }
    return fallback;
  }

  private static String merge(String template, Map<String, String> values){
    String out = template;
    for (Map.Entry<String, String> entry : values.entrySet()){
      out = out.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
    }
    return out;
  }

  private static void populateAgency(Map<String, String> values){
    values.put("agency.name", nz(ServiceLocator.agencyLabel()));
    values.put("agency.addressHtml", "");
    values.put("agency.vatRate", "");
    values.put("agency.cgvHtml", "");
    values.put("agency.emailCss", "");
    values.put("agency.emailSignatureHtml", "");
    AgencyConfigGateway gateway = ServiceLocator.agencyConfig();
    if (gateway == null){
      return;
    }
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
    <tr><td>TVA</td><td style=\"text-align:right\">{{agency.vatRate}}</td></tr>
    <tr><td>Total TTC</td><td style=\"text-align:right\"><b>{{quote.totalTtc}} €</b></td></tr>
  </table>
  <div class=\"cgv\">{{agency.cgvHtml}}</div>
</body></html>
""";
  }

  private static String defaultInvoiceTemplate(){
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
    <tr><td>TVA</td><td style=\"text-align:right\">{{agency.vatRate}}</td></tr>
    <tr><td>Total TTC</td><td style=\"text-align:right\"><b>{{invoice.totalTtc}} €</b></td></tr>
    <tr><td>Statut</td><td style=\"text-align:right\">{{invoice.status}}</td></tr>
  </table>
  <div class=\"cgv\">{{agency.cgvHtml}}</div>
</body></html>
""";
  }
}
