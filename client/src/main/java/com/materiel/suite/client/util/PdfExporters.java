package com.materiel.suite.client.util;

import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Quote;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

/**
 * Export PDF minimal :
 * <ul>
 *   <li>Utilise OpenPDF si présent (chargé via réflexion).</li>
 *   <li>Sinon, fallback vers l’impression système (permet impression PDF).</li>
 * </ul>
 */
public final class PdfExporters {
  private PdfExporters(){}

  public static void exportQuote(Quote q, File file){
    if (!tryOpenPdfQuote(q, file)){
      printFallback();
    }
  }

  private static boolean tryOpenPdfQuote(Quote q, File file){
    try {
      Class<?> docCls = Class.forName("com.lowagie.text.Document");
      Class<?> pdfWriter = Class.forName("com.lowagie.text.pdf.PdfWriter");
      Object document = docCls.getConstructor().newInstance();
      pdfWriter.getMethod("getInstance", docCls, java.io.OutputStream.class)
          .invoke(null, document, new FileOutputStream(file));
      docCls.getMethod("open").invoke(document);
      Class<?> paragraph = Class.forName("com.lowagie.text.Paragraph");
      docCls.getMethod("add", Class.forName("com.lowagie.text.Element")).invoke(document,
          paragraph.getConstructor(String.class).newInstance("DEVIS "+(q.getNumber()!=null?q.getNumber():"")));
      docCls.getMethod("add", Class.forName("com.lowagie.text.Element")).invoke(document,
          paragraph.getConstructor(String.class).newInstance("Date: "+ LocalDate.now()));
      for (DocumentLine l : q.getLines()){
        String line = String.format("- %s  x%s %s  @%.2f  = %.2f",
            l.getDesignation(), l.getQuantite(), l.getUnite(), l.getPrixUnitaireHT(), l.lineHT());
        docCls.getMethod("add", Class.forName("com.lowagie.text.Element")).invoke(document,
            paragraph.getConstructor(String.class).newInstance(line));
      }
      docCls.getMethod("close").invoke(document);
      return true;
    } catch (Throwable t){
      return false;
    }
  }

  private static void printFallback(){
    try {
      java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
      if (job.printDialog()){
        job.print();
      }
    } catch (Exception ignored){}
  }
}

