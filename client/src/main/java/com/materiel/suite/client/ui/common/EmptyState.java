package com.materiel.suite.client.ui.common;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/** Empty state générique avec message et action optionnelle. */
public class EmptyState extends JPanel {
  public EmptyState(String title, String subtitle, String ctaLabel, Runnable action){
    super(new GridBagLayout());
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.CENTER;
    gc.insets = new Insets(8, 8, 8, 8);

    JLabel titleLabel = new JLabel("<html><h2 style='margin:0'>" + escape(title) + "</h2></html>", SwingConstants.CENTER);
    add(titleLabel, gc);

    if (subtitle != null && !subtitle.isBlank()){
      gc.gridy++;
      JLabel subtitleLabel = new JLabel("<html><div style='color:#666;text-align:center'>" + escape(subtitle) + "</div></html>", SwingConstants.CENTER);
      add(subtitleLabel, gc);
    }

    if (ctaLabel != null && !ctaLabel.isBlank()){
      gc.gridy++;
      JButton button = new JButton(ctaLabel);
      if (action != null){
        button.addActionListener(e -> action.run());
      } else {
        button.setEnabled(false);
      }
      add(button, gc);
    }
  }

  private static String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
