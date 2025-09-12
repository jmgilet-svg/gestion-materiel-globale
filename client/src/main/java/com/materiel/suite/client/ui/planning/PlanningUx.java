package com.materiel.suite.client.ui.planning;

import java.awt.*;

/** Design tokens et helpers graphiques pour Planning/Agenda. */
final class PlanningUx {
  private PlanningUx(){}

  // Palette
  static final Color BG = Color.WHITE;
  static final Color BG_ALT1 = new Color(0xFAFAFA);
  static final Color BG_ALT2 = new Color(0xF2F2F2);
  static final Color GRID = new Color(0xDDDDDD);
  static final Color HEADER_BG = new Color(0xF6F7F9);
  static final Color HEADER_TX = new Color(0x2D2D2D);
  static final Color ROW_DIV = new Color(0xE6E6E6);
  static final Color TILE_TX = new Color(0x111111);
  static final Color TILE_SHADOW = new Color(0,0,0,35);
  static final Color TILE_HOVER = new Color(0,0,0,20);
  static final Color TILE_SELECT = new Color(0,0,0,28);
  static final Color HATCH = new Color(0,0,0,50);

  // Métriques
  static final int COL_MIN = 80;
  static final int COL_MAX = 220;
  static final int TILE_H = 26;        // tuile compacte (agenda, liste)
  static final int TILE_CARD_H = 108;   // tuile "carte" du planning
  static final int LANE_GAP = 6;
  static final int ROW_GAP = 8;
  static final int RADIUS = 10;
  static final int PAD = 8;
  static final int HANDLE = 6;
  static final int DRAG_THRESHOLD = 6;
  static final int CREATE_THRESHOLD = 12;

  static Font uiFont(Component c){
    Font f = c.getFont();
    if (f == null) return new Font("Dialog", Font.PLAIN, 12);
    return f.deriveFont(Font.PLAIN, 12f);
  }

  /** Dessine un motif hachuré diagonal. */
  static void paintHatch(Graphics2D g2, Rectangle r){
    g2.setColor(new Color(0,0,0,30));
    int step = 8;
    for (int x = r.x - r.height; x < r.x + r.width; x += step){
      g2.drawLine(x, r.y, x + r.height, r.y + r.height);
    }
    g2.setColor(new Color(255,255,255,70));
    for (int x = r.x - r.height + 4; x < r.x + r.width + 4; x += step){
      g2.drawLine(x, r.y, x + r.height, r.y + r.height);
    }
  }

  /** Convertit un code couleur hex → Color, sinon renvoie le défaut. */
  static Color colorOr(String hex, Color def){
    try {
      if (hex==null || hex.isBlank()) return def;
      return new Color(Integer.parseInt(hex.replace("#",""), 16));
    } catch(Exception e){ return def; }
  }

  /** Coupe la chaîne pour qu'elle tienne dans maxW, ajoute une ellipse si nécessaire. */
  static String ellipsize(String s, FontMetrics fm, int maxW){
    if (s==null) return "";
    if (fm.stringWidth(s) <= maxW) return s;
    final String ell = "…";
    int ellW = fm.stringWidth(ell);
    if (ellW >= maxW) return "";
    StringBuilder b = new StringBuilder();
    for (int i=0;i<s.length();i++){
      if (fm.stringWidth(b.toString() + s.charAt(i)) + ellW > maxW) break;
      b.append(s.charAt(i));
    }
    return b.toString() + ell;
  }

  /** Pastille arrondie (status / agence / chips). */
  static void pill(Graphics2D g2, Rectangle r, Color bg, Color fg, String text){
    g2.setColor(bg);
    g2.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
    g2.setColor(new Color(bg.darker().getRGB() & 0x33FFFFFF, true));
    g2.drawRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
    g2.setColor(fg);
    FontMetrics fm = g2.getFontMetrics();
    int tx = r.x + (r.width - fm.stringWidth(text))/2;
    int ty = r.y + (r.height + fm.getAscent())/2 - 2;
    g2.drawString(text, tx, ty);
  }
}
