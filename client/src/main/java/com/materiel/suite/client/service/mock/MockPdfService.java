package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.service.PdfService;
import com.materiel.suite.client.ui.sales.pdf.PdfMini;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/** Implémentation mock : convertit grossièrement le HTML en PDF texte. */
public class MockPdfService implements PdfService {
  @Override
  public byte[] render(String html, Map<String, String> inlineImages, String baseUrl){
    try {
      PdfMini pdf = new PdfMini();
      pdf.addTitle("Aperçu PDF (mock)");
      pdf.addParagraph(strip(html));
      File tmp = File.createTempFile("mock-pdf-", ".pdf");
      try {
        pdf.save(tmp);
        return Files.readAllBytes(tmp.toPath());
      } finally {
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
      }
    } catch (Exception ex){
      return new byte[0];
    }
  }

  private String strip(String html){
    if (html == null){
      return "";
    }
    return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
  }
}
