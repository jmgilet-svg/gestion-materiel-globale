package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import java.util.List;

import java.awt.*;

/** Rendu "carte" riche pour une intervention (vue Planning). */
final class InterventionTileRenderer {
  private static final int CHIP_H = 24;
  private static final int CHIP_GAP = 8;
  private boolean compact = false;
  private PlanningBoard.Density density = PlanningBoard.Density.NORMAL;
  private double scaleY = 1.0;

  int heightBase(){ return (int)Math.round(PlanningUx.TILE_CARD_H * scaleY); }
  int height(){ return heightBase(); }
  void setCompact(boolean c){ compact = c; }
  void setDensity(PlanningBoard.Density d){
    density = d;
    scaleY = switch(d){
      case COMPACT -> 0.88;
      case ROOMY -> 1.15;
      default -> 1.0;
    };
  }
  int heightFor(Intervention it, int widthPx){
    int base = compact? (int)Math.round(84 * scaleY) : heightBase();
    base += wrappedTextExtraHeight(it, widthPx);
    int chips = chipLines(it, widthPx);
    return base + (chips>0? (CHIP_H + CHIP_GAP)*(chips-1) : 0);
  }

  private int wrappedTextExtraHeight(Intervention it, int widthPx){ return 0; }

  private static final int GAP = 10;
  void paint(Graphics2D g2, Rectangle r, Intervention it, boolean hover, boolean selected){
    if (compact){
      paintCompact(g2, r, it, hover, selected);
      return;
    }
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Police harmonisée
    g2.setFont(PlanningUx.fontRegular(g2));

    // Ombre
    g2.setColor(PlanningUx.TILE_SHADOW);
    g2.fillRoundRect(r.x+3,r.y+3,r.width-6,r.height-6, PlanningUx.RADIUS, PlanningUx.RADIUS);
    // Fond
    g2.setColor(Color.WHITE);
    g2.fillRoundRect(r.x,r.y,r.width,r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);
    g2.setColor(new Color(0xDADEE3));
    g2.drawRoundRect(r.x,r.y,r.width,r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);

    // Barre accent à gauche
    Color accent = PlanningUx.colorOr(it.getColor(), new Color(0x3B82F6));
    g2.setColor(accent);
    g2.fillRoundRect(r.x-8, r.y+8, 10, r.height-16, 6, 6);

    // Overlays hover/selected
    if (hover || selected){
      g2.setColor(hover? PlanningUx.TILE_HOVER : PlanningUx.TILE_SELECT);
      g2.fillRoundRect(r.x,r.y,r.width,r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);
    }

    int x = r.x + 16;
    int y = r.y + 14;

    // Ligne 1 : heure, status, favoris, agence, menu
    Font f0 = g2.getFont();
    Font fTime = PlanningUx.fontLarge(g2);
    g2.setFont(fTime);
    String time = it.prettyTimeRange()==null? "—" : it.prettyTimeRange();
    g2.setColor(new Color(0x0F172A));
    g2.drawString(time, x, y+2);
    int timeW = g2.getFontMetrics().stringWidth(time);
    g2.setFont(f0);

    int right = r.x + r.width - 12;
    // menu (3 points)
    g2.setColor(new Color(0x8A8FA0));
    int dotY = y-6;
    for (int i=0;i<3;i++) g2.fillOval(right - i*10, dotY, 4,4);

    // Favori (étoile simple)
    int starX = x + 160;
    paintStar(g2, starX, y-10, 16, new Color(0x1F2937));

    // Status pill
    String status = it.getStatus()==null? "PLANNED" : it.getStatus().toUpperCase();
    Color sBg = switch(status){
      case "CONFIRMED" -> new Color(0xD1FAE5);
      case "DONE"      -> new Color(0xDBEAFE);
      case "CANCELED"  -> new Color(0xFEE2E2);
      default          -> new Color(0xE5E7EB);
    };
    Color sFg = new Color(0x0F172A);
    int wStatus = Math.max(96, g2.getFontMetrics().stringWidth(status)+24);
    int statusX = x + timeW + GAP;

    // Agence pill
    String agency = it.getAgency()==null? "Agence ?" : it.getAgency();
    int wAgency = Math.max(80, g2.getFontMetrics().stringWidth(agency)+24);
    int agencyX = statusX + wStatus + GAP;

    // Wrap si collision avec bord droit
    int rightLimit = r.x + r.width - 80;
    boolean wrap = agencyX + wAgency > rightLimit;
    if (wrap){ statusX = x; agencyX = x + wStatus + GAP; y += 22; }

    PlanningUx.pill(g2, new Rectangle(statusX, y-18, wStatus, 28), sBg, sFg, status);

    PlanningUx.pill(g2, new Rectangle(agencyX, y-18, wAgency, 28), new Color(0xEEF2FF), new Color(0x0F172A), agency);

    // Ligne 2 : client
    y += 24;
    g2.setFont(f0.deriveFont(Font.BOLD, 16f));
    String client = it.getClientName()==null? (it.getLabel()==null? "—" : it.getLabel()) : it.getClientName();
    g2.setColor(new Color(0x111827));
    g2.drawString(client, x, y);
    y += 18;
    g2.setFont(PlanningUx.fontRegular(g2));

    g2.setColor(new Color(0x374151));
    g2.drawString("Chantier : " + nullToDash(it.getSiteLabel()), x, y);
    y += 16;

    // Séparateur
    g2.setColor(new Color(0xE5E7EB));
    g2.drawLine(x, y, r.x + r.width - 16, y);

    // Ligne 4 : grue / camion
    y += 10;
    int colW = (r.width - 48)/2;
    paintCraneIcon(g2, x, y+4);
    g2.setColor(new Color(0x111827));
    g2.drawString("Grue : " + nullToDash(it.getCraneName()), x+28, y+8);
    paintTruckIcon(g2, x + colW, y+4);
    g2.drawString("Camion : " + nullToDash(it.getTruckName()), x+colW+28, y+8);

    // Séparateur 2
    y += 22;
    g2.setColor(new Color(0xE5E7EB));
    g2.drawLine(x, y, r.x + r.width - 16, y);

    // Ligne 5 : chauffeur + initiales
    y += 10;
    paintBadge(g2, x, y, it.driverInitials());
    g2.setColor(new Color(0x1F2937));
    g2.drawString("Chauffeur : " + nullToDash(it.getDriverName()), x+40, y+8);

    // Ligne 6 : chips documents
    y += 26;
    wrapChips(g2, r, x, y, List.of(
        chipSpec(it.getQuoteNumber(), new Color(0xD1FAE5), new Color(0x176E43), "Devis "),
        chipSpec(it.getOrderNumber(), new Color(0xFEF3C7), new Color(0xA16207), "Commande "),
        chipSpec(it.getDeliveryNumber(), new Color(0xE5E7EB), new Color(0x374151), "BL "),
        chipSpec(it.getInvoiceNumber(), new Color(0xE5E7EB), new Color(0x374151), "Fact. ")
    ));
  }

  private record Chip(String text, Color bg, Color fg){}
  private Chip chipSpec(String value, Color bg, Color fg, String prefix){
    String text = prefix + ((value==null || value.isBlank())? "—" : value);
    return new Chip(text, bg, fg);
  }
  private void wrapChips(Graphics2D g2, Rectangle r, int x, int y, List<Chip> chips){
    int cx = x, cy = y;
    int maxX = r.x + r.width - 16;
    for (Chip c : chips){
      int w = Math.max(90, g2.getFontMetrics().stringWidth(c.text)+24);
      if (cx + w > maxX){ cx = x; cy += 30; }
      PlanningUx.pill(g2, new Rectangle(cx, cy, w, 28), c.bg, c.fg, c.text);
      cx += w + 8;
    }
  }

  private int chipLines(Intervention it, int widthPx){
    String[] labels = chipLabels(it);
    if (labels.length==0) return 0;
    int max = Math.max(80, widthPx - 32);
    int line=1, x=0;
    for (String s : labels){
      int w = approxChipWidth(s);
      if (x>0 && x + w > max){ line++; x=0; }
      x += (x==0? w : w + CHIP_GAP);
    }
    return line;
  }

  private String[] chipLabels(Intervention it){
    return new String[]{
        "Devis " + nullToDash(it.getQuoteNumber()),
        "Commande " + nullToDash(it.getOrderNumber()),
        "BL " + nullToDash(it.getDeliveryNumber()),
        "Fact. " + nullToDash(it.getInvoiceNumber())
    };
  }

  private int approxChipWidth(String s){
    int text = (s==null? 1 : Math.max(1, s.length()));
    return Math.max(90, text*7 + 24);
  }

  private void paintCompact(Graphics2D g2, Rectangle r, Intervention it, boolean hover, boolean selected){
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setFont(PlanningUx.fontRegular(g2));

    g2.setColor(PlanningUx.TILE_SHADOW);
    g2.fillRoundRect(r.x+3,r.y+3,r.width-6,r.height-6, PlanningUx.RADIUS, PlanningUx.RADIUS);
    g2.setColor(Color.WHITE);
    g2.fillRoundRect(r.x,r.y,r.width,r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);
    g2.setColor(new Color(0xDADEE3));
    g2.drawRoundRect(r.x,r.y,r.width,r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);
    Color accent = PlanningUx.colorOr(it.getColor(), new Color(0x3B82F6));
    g2.setColor(accent);
    g2.fillRoundRect(r.x-8, r.y+8, 10, r.height-16, 6, 6);
    if (hover || selected){
      g2.setColor(hover? PlanningUx.TILE_HOVER : PlanningUx.TILE_SELECT);
      g2.fillRoundRect(r.x,r.y,r.width,r.height, PlanningUx.RADIUS, PlanningUx.RADIUS);
    }

    int x = r.x + 16;
    int y = r.y + 14;

    Font f0 = g2.getFont();
    Font fTime = PlanningUx.fontLarge(g2);
    g2.setFont(fTime);
    String time = it.prettyTimeRange()==null? "—" : it.prettyTimeRange();
    g2.setColor(new Color(0x0F172A));
    g2.drawString(time, x, y+2);
    int timeW = g2.getFontMetrics().stringWidth(time);
    g2.setFont(f0);

    String status = it.getStatus()==null? "PLANNED" : it.getStatus().toUpperCase();
    Color sBg = switch(status){
      case "CONFIRMED" -> new Color(0xD1FAE5);
      case "DONE"      -> new Color(0xDBEAFE);
      case "CANCELED"  -> new Color(0xFEE2E2);
      default          -> new Color(0xE5E7EB);
    };
    Color sFg = new Color(0x0F172A);
    int wStatus = Math.max(96, g2.getFontMetrics().stringWidth(status)+24);
    int statusX = x + timeW + GAP;

    String agency = it.getAgency()==null? "Agence ?" : it.getAgency();
    int wAgency = Math.max(80, g2.getFontMetrics().stringWidth(agency)+24);
    int agencyX = statusX + wStatus + GAP;
    int rightLimit = r.x + r.width - 80;
    boolean wrap = agencyX + wAgency > rightLimit;
    if (wrap){ statusX = x; agencyX = x + wStatus + GAP; y += 22; }

    PlanningUx.pill(g2, new Rectangle(statusX, y-18, wStatus, 28), sBg, sFg, status);
    PlanningUx.pill(g2, new Rectangle(agencyX, y-18, wAgency, 28), new Color(0xEEF2FF), new Color(0x0F172A), agency);

    y += 24;
    g2.setFont(f0.deriveFont(Font.BOLD, 15f));
    String client = it.getClientName()==null? (it.getLabel()==null? "—" : it.getLabel()) : it.getClientName();
    g2.setColor(new Color(0x111827));
    String line = client;
    String site = it.getSiteLabel()==null? "" : " — " + it.getSiteLabel();
    int space = r.width - 32;
    if (g2.getFontMetrics().stringWidth(line + site) < space){ line += site; }
    g2.drawString(line, x, y);
    y += 18;

    wrapChips(g2, r, x, y, List.of(
        chipSpec(it.getQuoteNumber(), new Color(0xD1FAE5), new Color(0x176E43), "Devis "),
        chipSpec(it.getOrderNumber(), new Color(0xFEF3C7), new Color(0xA16207), "Commande "),
        chipSpec(it.getDeliveryNumber(), new Color(0xE5E7EB), new Color(0x374151), "BL "),
        chipSpec(it.getInvoiceNumber(), new Color(0xE5E7EB), new Color(0x374151), "Fact. ")
    ));
  }

  /* Helpers de rendu */
  private static String nullToDash(String s){ return (s==null || s.isBlank())? "—" : s; }

  private static void paintStar(Graphics2D g2, int cx, int cy, int r, Color c){
    Polygon p = new Polygon();
    for (int i=0;i<5;i++){
      double a = Math.toRadians(-90 + i*72);
      double a2 = Math.toRadians(-90 + i*72 + 36);
      p.addPoint((int)(cx + r*Math.cos(a)), (int)(cy + r*Math.sin(a)));
      p.addPoint((int)(cx + (r/2.2)*Math.cos(a2)), (int)(cy + (r/2.2)*Math.sin(a2)));
    }
    g2.setColor(c);
    g2.drawPolygon(p);
  }
  private static void paintCraneIcon(Graphics2D g2, int x, int y){
    g2.setColor(new Color(0x0F172A));
    g2.drawRect(x, y, 16, 12);
    g2.drawLine(x+2, y+8, x+14, y+8);
    g2.fillOval(x+2, y+12, 4,4);
    g2.fillOval(x+10, y+12, 4,4);
  }
  private static void paintTruckIcon(Graphics2D g2, int x, int y){
    g2.setColor(new Color(0x0F172A));
    g2.drawRect(x+6, y, 16, 10);
    g2.drawRect(x, y+4, 8, 8);
    g2.fillOval(x+4, y+12, 4,4);
    g2.fillOval(x+18, y+12, 4,4);
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
}
