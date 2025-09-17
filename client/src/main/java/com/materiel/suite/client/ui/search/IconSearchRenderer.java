package com.materiel.suite.client.ui.search;

import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * Renderer utilisé par la recherche globale : chaque élément peut afficher une icône contextuelle.
 */
public class IconSearchRenderer extends DefaultListCellRenderer {
  @Override
  public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    String iconKey = null;
    String textValue = value != null ? value.toString() : "";

    if (value instanceof SearchItem item) {
      iconKey = item.iconKey() != null ? item.iconKey() : mapType(item.type());
      label.setText(item.label());
      label.setToolTipText(item.subtitle());
    } else {
      int sep = textValue.indexOf(':');
      if (sep > 0) {
        iconKey = mapType(textValue.substring(0, sep));
        label.setText(textValue.substring(sep + 1).trim());
      }
    }

    Icon icon = iconKey != null ? IconRegistry.small(iconKey) : null;
    if (icon == null) {
      icon = IconRegistry.placeholder(16);
    }
    label.setIcon(icon);
    label.setIconTextGap(8);
    return label;
  }

  private String mapType(String type) {
    if (type == null) {
      return "search";
    }
    String normalized = type.toLowerCase(Locale.ROOT);
    if (normalized.contains("quote") || normalized.contains("devis")) {
      return "file";
    }
    if (normalized.contains("order") || normalized.contains("commande") || normalized.equals("bc")) {
      return "pallet";
    }
    if (normalized.contains("delivery") || normalized.contains("bl")) {
      return "truck";
    }
    if (normalized.contains("invoice") || normalized.contains("facture")) {
      return "invoice";
    }
    if (normalized.contains("client") || normalized.contains("customer")) {
      return "user";
    }
    if (normalized.contains("resource") || normalized.contains("ressource")) {
      return "wrench";
    }
    if (normalized.contains("agenda") || normalized.contains("planning") || normalized.contains("calendar")) {
      return "calendar";
    }
    return "search";
  }

  /**
   * Représentation légère d'un résultat pour la liste.
   */
  public static record SearchItem(String type, String label, String subtitle, String iconKey) {
  }
}
