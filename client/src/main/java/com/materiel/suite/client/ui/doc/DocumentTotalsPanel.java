package com.materiel.suite.client.ui.doc;

import com.materiel.suite.client.model.DocumentTotals;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class DocumentTotalsPanel extends JPanel {
  private final JLabel lHT = new JLabel("0,00 €");
  private final JLabel lTVA = new JLabel("0,00 €");
  private final JLabel lTTC = new JLabel("0,00 €");
  private final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.FRANCE);
  public DocumentTotalsPanel(){
    super(new GridBagLayout());
    setBorder(new EmptyBorder(8,8,8,8));
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4,4,4,4);
    c.gridx=0;c.gridy=0; add(new JLabel("Total HT:"), c);
    c.gridx=1; add(lHT, c);
    c.gridx=0;c.gridy=1; add(new JLabel("TVA:"), c);
    c.gridx=1; add(lTVA, c);
    c.gridx=0;c.gridy=2; add(new JLabel("Total TTC:"), c);
    c.gridx=1; add(lTTC, c);
  }
  public void setTotals(DocumentTotals t){
    lHT.setText(CURRENCY.format(t.getTotalHT()));
    lTVA.setText(CURRENCY.format(t.getTotalTVA()));
    lTTC.setText(CURRENCY.format(t.getTotalTTC()));
  }
}
