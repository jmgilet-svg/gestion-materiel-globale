package com.materiel.suite.client.ui.planning.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.ui.icons.IconRegistry;

/** Skin « propre » : coins 8px, bande statut, badge devis, pilule agence. */
public final class DefaultTileRenderer implements TileRenderer {
  private static final int RADIUS = 8;

  @Override
  public void paintTile(Graphics2D g2, Intervention it, Rectangle bounds, State state){
    if (g2 == null || it == null || bounds == null){
      return;
    }
    if (bounds.width <= 0 || bounds.height <= 0){
      return;
    }

    Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    RoundRectangle2D rr = new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height,
        RADIUS, RADIUS);
    // Fond
    Color base = new Color(0xF8FAFF);
    Color border = new Color(0xC7D2FE);
    if (state != null && state.selected()){
      base = new Color(0xEEF2FF);
      border = new Color(0x6366F1);
    }
    g2.setColor(base);
    g2.fill(rr);
    // Ombre légère par dégradé (appliquée par-dessus le fond)
    Paint paintBeforeShadow = g2.getPaint();
    Composite compositeBeforeShadow = g2.getComposite();
    g2.setComposite(AlphaComposite.SrcOver.derive(0.12f));
    g2.setPaint(new GradientPaint(bounds.x, bounds.y, new Color(0, 0, 0, 40),
        bounds.x, bounds.y + bounds.height, new Color(0, 0, 0, 0)));
    g2.fill(rr);
    g2.setComposite(compositeBeforeShadow);
    g2.setPaint(paintBeforeShadow);
    g2.setColor(border);
    g2.draw(rr);

    // Stripe statut (dégradé à gauche)
    Color stripe = new Color(0x60A5FA);
    if (state != null){
      String status = String.valueOf(state.status()).toUpperCase();
      stripe = switch (status) {
        case "DONE", "CONFIRMED" -> new Color(0x22C55E);
        case "PENDING", "DRAFT" -> new Color(0xF59E0B);
        case "CANCELLED", "CANCELED" -> new Color(0xEF4444);
        default -> new Color(0x60A5FA);
      };
    }
    int stripeWidth = 6;
    Paint paintBeforeStripe = g2.getPaint();
    Paint gradient = new LinearGradientPaint(
        bounds.x, bounds.y,
        bounds.x + stripeWidth, bounds.y,
        new float[]{0f, 1f},
        new Color[]{stripe.brighter(), stripe.darker()}
    );
    g2.setPaint(gradient);
    g2.fill(new RoundRectangle2D.Float(bounds.x, bounds.y, stripeWidth, bounds.height, RADIUS, RADIUS));
    g2.setPaint(paintBeforeStripe);

    // Titre + sous-titre
    String title = it.getLabel() != null && !it.getLabel().isBlank() ? it.getLabel()
        : (it.getClientName() != null ? it.getClientName() : "Intervention");
    String subtitle = it.getAddress() != null ? it.getAddress() : "";
    int tx = bounds.x + 10 + stripeWidth;
    int ty = bounds.y + 16;
    g2.setColor(new Color(0x0F172A));
    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
    g2.drawString(truncate(title, g2, bounds.width - stripeWidth - 20), tx, ty);
    g2.setColor(new Color(0x475569));
    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
    g2.drawString(truncate(subtitle, g2, bounds.width - stripeWidth - 20), tx, ty + 14);

    // Pilule agence (en haut à droite)
    if (state != null && state.agency() != null && !state.agency().isBlank()){
      String txt = state.agency();
      Font f = g2.getFont().deriveFont(Font.PLAIN, 10f);
      g2.setFont(f);
      int w = g2.getFontMetrics().stringWidth(txt) + 14;
      int h = 16;
      int px = bounds.x + bounds.width - w - 6;
      int py = bounds.y + 4;
      g2.setColor(new Color(0xE0E7FF));
      g2.fillRoundRect(px, py, w, h, 999, 999);
      g2.setColor(new Color(0x1E3A8A));
      g2.drawString(txt, px + 7, py + 11);
    }

    // Badge "devisé"
    if (state != null && state.hasQuote()){
      var ico = IconRegistry.small("badge");
      if (ico != null){
        int ix = bounds.x + 8;
        int iy = bounds.y + bounds.height - 18;
        ico.paintIcon(null, g2, ix, iy);
      } else {
        g2.setColor(new Color(0xDCFCE7));
        g2.fillRoundRect(bounds.x + 6, bounds.y + bounds.height - 18, 28, 14, 999, 999);
        g2.setColor(new Color(0x166534));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 9f));
        g2.drawString("DEV", bounds.x + 11, bounds.y + bounds.height - 7);
      }
    }

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
  }

  private static String truncate(String s, Graphics2D g2, int maxPx){
    if (s == null){
      return "";
    }
    var fm = g2.getFontMetrics();
    if (fm.stringWidth(s) <= maxPx){
      return s;
    }
    String ellipsis = "…";
    int wEll = fm.stringWidth(ellipsis);
    int width = 0;
    StringBuilder out = new StringBuilder();
    for (char c : s.toCharArray()){
      int next = width + fm.charWidth(c);
      if (next + wEll > maxPx){
        break;
      }
      out.append(c);
      width = next;
    }
    return out.append(ellipsis).toString();
  }
}
