package com.materiel.suite.client.ui.common;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Render a PDF (in bytes) to images for quick previews inside Swing dialogs.
 */
public class PdfPreviewPanel extends JPanel {
  private final JPanel pages;
  private double dpi = 110d;

  public PdfPreviewPanel() {
    super(new BorderLayout());
    pages = new JPanel(new GridLayout(0, 1, 0, 8));
    pages.setOpaque(true);
    pages.setBackground(Color.WHITE);
    add(new JScrollPane(pages), BorderLayout.CENTER);
  }

  /** Adjust rendering density (min 72 DPI). */
  public void setDpi(double dpi) {
    this.dpi = Math.max(72d, dpi);
  }

  /** Display the provided PDF bytes as images. */
  public void setPdf(byte[] pdf) {
    pages.removeAll();
    if (pdf == null || pdf.length == 0) {
      revalidate();
      repaint();
      return;
    }
    try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
      PDFRenderer renderer = new PDFRenderer(document);
      int count = document.getNumberOfPages();
      for (int index = 0; index < count; index++) {
        BufferedImage image = renderer.renderImageWithDPI(index, (float) dpi);
        JLabel label = new JLabel(new ImageIcon(image));
        label.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        pages.add(label);
      }
    } catch (Exception ex) {
      JLabel error = new JLabel("Erreur preview PDF : " + ex.getMessage());
      error.setForeground(Color.RED.darker());
      pages.add(error);
    }
    revalidate();
    repaint();
  }
}
