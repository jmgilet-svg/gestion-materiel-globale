package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Utilitaire de rendu pour afficher une série d'icônes de ressources sur une intervention. */
public final class InterventionIconPainter {
  private InterventionIconPainter(){}

  public static void paintIcons(Graphics2D g2, Rectangle bounds, List<ResourceRef> resources){
    if (resources==null || resources.isEmpty()) return;
    Font old = g2.getFont();
    float fontSize = Math.max(10f, Math.min(bounds.height * 0.6f, 14f));
    g2.setFont(old.deriveFont(fontSize));
    int pad = 6;
    int x = bounds.x + pad;
    int baseline = bounds.y + bounds.height - pad;
    int iconSize = Math.round(Math.max(12f, Math.min(bounds.height * 0.7f, 20f)));
    for (ResourceRef ref : resources){
      if (ref==null) continue;
      String raw = ref.getIcon();
      Icon icon = IconRegistry.load(raw, iconSize);
      if (icon != null){
        int y = baseline - icon.getIconHeight();
        icon.paintIcon(null, g2, x, y);
        x += icon.getIconWidth() + 6;
      } else {
        String text = (raw==null || raw.isBlank())? "🏷️" : raw;
        g2.drawString(text, x, baseline);
        x += g2.getFontMetrics().stringWidth(text) + 6;
      }
      if (x > bounds.x + bounds.width - 10) break;
    }
    g2.setFont(old);
  }
}
