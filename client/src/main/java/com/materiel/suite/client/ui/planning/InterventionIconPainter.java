package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.ResourceRef;

import java.awt.*;
import java.util.List;

/** Utilitaire de rendu pour afficher une série d'icônes de ressources sur une intervention. */
public final class InterventionIconPainter {
  private InterventionIconPainter(){}

  public static void paintIcons(Graphics2D g2, Rectangle bounds, List<ResourceRef> resources){
    if (resources==null || resources.isEmpty()) return;
    Font old = g2.getFont();
    float size = Math.max(10f, Math.min(bounds.height * 0.6f, 14f));
    g2.setFont(old.deriveFont(size));
    int pad = 6;
    int x = bounds.x + pad;
    int baseline = bounds.y + bounds.height - pad;
    for (ResourceRef ref : resources){
      if (ref==null) continue;
      String icon = ref.getIcon();
      if (icon==null || icon.isBlank()) icon = "🏷️";
      g2.drawString(icon, x, baseline);
      x += g2.getFontMetrics().stringWidth(icon) + 6;
      if (x > bounds.x + bounds.width - 10) break;
    }
    g2.setFont(old);
  }
}
