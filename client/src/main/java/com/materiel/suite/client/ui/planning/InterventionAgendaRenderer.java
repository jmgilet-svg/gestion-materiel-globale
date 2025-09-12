package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import java.util.List;

import java.awt.*;

/** Rendu compact pour une intervention dans la vue Agenda. */
final class InterventionAgendaRenderer {

  Rectangle menuRect(Rectangle r){
    int w = 28;
    int h = 22;
    return new Rectangle(r.x + r.width - w - 8, r.y + 6, w, h);
  }

  private static final int GAP = 8;

  void paint(Graphics2D g2, Rectangle r, Intervention it, boolean hover, boolean selected){
    // Ombre + fond
    g2.setColor(PlanningUx.TILE_SHADOW);
    g2.fillRoundRect(r.x+2, r.y+2, r.width-4, r.height-4, PlanningUx.RADIUS, PlanningUx.RADIUS);
    g2.setColor(Color.WHITE);
    g2.fillRoundRect(r.x, r.y, r.width, r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);

    if (hover || selected){
      g2.setColor(hover? PlanningUx.TILE_HOVER : PlanningUx.TILE_SELECT);
      g2.fillRoundRect(r.x, r.y, r.width, r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);
    }

    int x = r.x + 12;
    int y = r.y + 10;
    Font f0 = g2.getFont();

    // Ligne 1: heures + status + agence + menu
    Font fTime = f0.deriveFont(Font.BOLD, 16f);
    g2.setFont(fTime);
    g2.setColor(new Color(0x0F172A));
    String time = it.prettyTimeRange();
    g2.drawString(time, x, y+4);
    int timeW = g2.getFontMetrics().stringWidth(time);
    g2.setFont(f0);

    // Status
    String status = it.getStatus()==null? "PLANNED" : it.getStatus().toUpperCase();
    Color sBg = switch(status){
      case "CONFIRMED" -> new Color(0xD1FAE5);
      case "DONE"      -> new Color(0xDBEAFE);
      case "CANCELED"  -> new Color(0xFEE2E2);
      default          -> new Color(0xE5E7EB);
    };
    int wStatus = Math.max(88, g2.getFontMetrics().stringWidth(status)+22);
    int statusX = x + timeW + GAP;

    // Agence
    String ag = it.getAgency()==null? "Agence ?" : it.getAgency();
    int wAgency = Math.max(72, g2.getFontMetrics().stringWidth(ag)+22);
    int agencyX = statusX + wStatus + GAP;

    // Si pas assez de place sur la ligne, on passe la/les pastilles dessous
    int rightLimit = r.x + r.width - 40;
    boolean wrapStatus = statusX + wStatus > rightLimit;
    boolean wrapAgency = agencyX + wAgency > rightLimit;
    int pillsY = y - 10;
    if (wrapStatus){ statusX = x; pillsY += 22; }
    if (wrapAgency){ agencyX = wrapStatus ? x + wStatus + GAP : agencyX; }

    PlanningUx.pill(g2, new Rectangle(statusX, pillsY, wStatus, 24), sBg, new Color(0x0F172A), status);
    PlanningUx.pill(g2, new Rectangle(agencyX, pillsY, wAgency, 24), new Color(0xEEF2FF), new Color(0x0F172A), ag);

    // Ajuste Y si on a wrap pour éviter collisions avec ligne suivante
    if (wrapStatus || wrapAgency){ y += 22; }

    // menu (3 points)
    Rectangle mr = menuRect(r);
    g2.setColor(new Color(0x8A8FA0));
    int dotY = mr.y + 6;
    for (int i=0;i<3;i++) g2.fillOval(mr.x + 4 + i*7, dotY, 4,4);

    // Chauffeur + badge
    y += 28;
    paintBadge(g2, x, y, initials(it));
    g2.setColor(new Color(0x1F2937));
    g2.drawString("Chauffeur : " + nonEmpty(it.getDriverName(),"—"), x+38, y+6);

    // Chips docs
    y += 22;
    wrapChips(g2, r, x, y, List.of(
        chipSpec(it.getQuoteNumber(), new Color(0xD1FAE5), new Color(0x176E43), "Devis "),
        chipSpec(it.getOrderNumber(), new Color(0xFEF3C7), new Color(0xA16207), "Commande "),
        chipSpec(it.getDeliveryNumber(), new Color(0xE5E7EB), new Color(0x374151), "BL "),
        chipSpec(it.getInvoiceNumber(), new Color(0xE5E7EB), new Color(0x374151), "Fact. ")
    ));

    // Cadenas si verrouillé
    if (it.isLocked()) {
      g2.setColor(new Color(0x374151));
      paintLock(g2, r.x+8, r.y+8);
    }
  }

  private record Chip(String text, Color bg, Color fg){}
  private Chip chipSpec(String value, Color bg, Color fg, String prefix){
    String text = prefix + ((value==null || value.isBlank())? "—" : value);
    return new Chip(text, bg, fg);
  }
  private void wrapChips(Graphics2D g2, Rectangle r, int x, int y, List<Chip> chips){
    int cx = x, cy = y;
    int maxX = r.x + r.width - 12;
    for (Chip c : chips){
      int w = Math.max(90, g2.getFontMetrics().stringWidth(c.text)+24);
      if (cx + w > maxX){ cx = x; cy += 28; }
      PlanningUx.pill(g2, new Rectangle(cx, cy, w, 24), c.bg, c.fg, c.text);
      cx += w + 6;
    }
  }

  private static String nonEmpty(String s, String fallback){ return (s==null || s.isBlank())? fallback : s; }
  private static String initials(Intervention it){
    String d = it.getDriverName();
    if (d==null || d.isBlank()) return "—";
    String[] p = d.trim().split("\\s+");
    String ini = String.valueOf(Character.toUpperCase(p[0].charAt(0)));
    if (p.length>1) ini += Character.toUpperCase(p[1].charAt(0));
    return ini;
    }
  private static void paintBadge(Graphics2D g2, int x, int y, String txt){
    int w = Math.max(28, g2.getFontMetrics().stringWidth(txt)+14);
    g2.setColor(new Color(0xE5E7EB));
    g2.fillOval(x, y-2, w, 22);
    g2.setColor(new Color(0x9CA3AF));
    g2.drawOval(x, y-2, w, 22);
    g2.setColor(new Color(0x111827));
    int tx = x + (w - g2.getFontMetrics().stringWidth(txt))/2;
    int ty = y + g2.getFontMetrics().getAscent()/2 + 2;
    g2.drawString(txt, tx, ty);
  }
  private static void paintLock(Graphics2D g2, int x, int y){
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y+6, 12, 10);
    g2.drawArc(x+1, y, 10, 12, 0, 180);
  }
}

