package com.materiel.suite.client.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Map;

public class StatusBadgeRenderer extends DefaultTableCellRenderer {
  private static final Map<String, Color> COLORS = Map.ofEntries(
      Map.entry("Brouillon", new Color(0xE0E0E0)),
      Map.entry("Envoyé", new Color(0xD6E4FF)),
      Map.entry("Accepté", new Color(0xC8E6C9)),
      Map.entry("Refusé", new Color(0xF8BBD0)),
      Map.entry("Expiré", new Color(0xFFE0B2)),
      Map.entry("Confirmé", new Color(0xC8E6C9)),
      Map.entry("Annulé", new Color(0xFFCDD2)),
      Map.entry("Signé", new Color(0xD1C4E9)),
      Map.entry("Verrouillé", new Color(0xB0BEC5)),
      Map.entry("Envoyée", new Color(0xD6E4FF)),
      Map.entry("Partiellement payée", new Color(0xFFF59D)),
      Map.entry("Payée", new Color(0xC8E6C9))
  );
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    l.setHorizontalAlignment(SwingConstants.CENTER);
    l.setOpaque(true);
    String s = value==null? "": value.toString();
    Color bg = COLORS.getOrDefault(s, new Color(0xEEEEEE));
    if (isSelected) {
      l.setForeground(table.getSelectionForeground());
      l.setBackground(table.getSelectionBackground());
    } else {
      l.setForeground(Color.DARK_GRAY);
      l.setBackground(bg);
    }
    l.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
    return l;
  }
}
