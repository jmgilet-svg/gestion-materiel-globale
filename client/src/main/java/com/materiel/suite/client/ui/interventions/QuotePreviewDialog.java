package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.QuoteV2;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Aperçu simple d'un devis v2 pour consultation rapide. */
public class QuotePreviewDialog extends JDialog {
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.FRANCE);

  public QuotePreviewDialog(Window parent, QuoteV2 quote){
    super(parent, titleFor(quote), ModalityType.MODELESS);
    setSize(420, 220);
    setLocationRelativeTo(parent);
    setResizable(false);
    setContentPane(buildContent(quote));
  }

  private static String titleFor(QuoteV2 quote){
    if (quote == null){
      return "Devis";
    }
    String ref = quote.getReference();
    return ref == null || ref.isBlank() ? "Devis" : "Devis " + ref;
  }

  private JPanel buildContent(QuoteV2 quote){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("Référence"), gc);
    gc.gridx = 1;
    panel.add(new JLabel(valueOrDash(quote != null ? quote.getReference() : null)), gc);
    gc.gridx = 0;
    gc.gridy++;
    panel.add(new JLabel("Statut"), gc);
    gc.gridx = 1;
    panel.add(new JLabel(valueOrDash(quote != null ? quote.getStatus() : null)), gc);
    gc.gridx = 0;
    gc.gridy++;
    panel.add(new JLabel("Total HT"), gc);
    gc.gridx = 1;
    panel.add(new JLabel(formatCurrency(quote != null ? quote.getTotalHt() : null)), gc);
    gc.gridx = 0;
    gc.gridy++;
    panel.add(new JLabel("Total TTC"), gc);
    gc.gridx = 1;
    panel.add(new JLabel(formatCurrency(quote != null ? quote.getTotalTtc() : null)), gc);
    gc.gridx = 0;
    gc.gridy++;
    gc.gridwidth = 2;
    gc.anchor = GridBagConstraints.CENTER;
    JButton close = new JButton("Fermer");
    close.addActionListener(e -> dispose());
    panel.add(close, gc);
    return panel;
  }

  private static String valueOrDash(String value){
    return value == null || value.isBlank() ? "—" : value;
  }

  private static String formatCurrency(BigDecimal value){
    if (value == null){
      return "—";
    }
    return CURRENCY.format(value);
  }
}
