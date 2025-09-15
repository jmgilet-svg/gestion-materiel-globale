package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Rendu d'une intervention dans la grille du planning. */
final class InterventionTileRenderer {
  private static final int PAD = PlanningUx.PAD;
  private static final int R = PlanningUx.RADIUS;
  private static final int CHIP_H = 24;
  private static final int CHIP_GAP = 8;
  private boolean compact = false;
  private PlanningBoard.Density density = PlanningBoard.Density.NORMAL;
  private double scaleY = 1.0;

  // Tiers de rendu selon l'espace disponible
  enum Tier { XS, SM, MD, LG }
  private Tier tierFor(Rectangle r){
    int w = r.width;
    int h = r.height;
    if (w < 240 || h < 110) return Tier.XS;
    if (w < 320) return Tier.SM;
    if (w < 420) return Tier.MD;
    return Tier.LG;
  }

  int heightBase(){ return (int)Math.round(PlanningUx.TILE_CARD_H * scaleY); }
  void setCompact(boolean c){ compact = c; }
  void setDensity(PlanningBoard.Density d){
    density = d;
    scaleY = switch(d){
      case COMPACT -> 0.88;
      case ROOMY -> 1.15;
      default -> 1.0;
    };
  }

  /** Calcul de hauteur dynamique (wrap + chips). */
  int heightFor(Intervention it, int widthPx){
    int base = compact ? (int)Math.round(84 * scaleY) : heightBase();
    Tier t = (widthPx < 240)? Tier.XS : (widthPx<320? Tier.SM : (widthPx<420? Tier.MD : Tier.LG));
    boolean showDetails = !compact && (t==Tier.MD || t==Tier.LG);
    int extraWrap = showDetails ? wrappedTextExtraHeight(it, widthPx)
        : wrappedTextExtraHeightCompact(it, widthPx);
    base += extraWrap;
    int allowedChips = (t==Tier.XS? 0 : t==Tier.SM? 1 : t==Tier.MD? 2 : Integer.MAX_VALUE);
    int chips = chipLines(it, widthPx, allowedChips);
    return base + (chips>0? (CHIP_H + 8) * (chips-1) : 0);
  }

  private int chipLines(Intervention it, int widthPx){ return chipLines(it, widthPx, Integer.MAX_VALUE); }
  private int chipLines(Intervention it, int widthPx, int limit){
    String[] all = chipsFor(it);
    String[] labels = all;
    if (limit < all.length){
      labels = new String[limit];
      System.arraycopy(all, 0, labels, 0, limit);
    }
    if (labels.length==0) return 0;
    int max = Math.max(80, widthPx - 2*PAD);
    int line=1, x=0;
    for (String s : labels){
      int w = approxChipWidth(s);
      if (x>0 && x + w > max){ line++; x=0; }
      x += (x==0? w : w + CHIP_GAP);
    }
    return line;
  }

  private static int approxCharW(boolean bold){ return bold ? 8 : 7; }
  private static int lineH(boolean bold){ return bold ? 18 : 16; }

  private int wrappedTextExtraHeight(Intervention it, int widthPx){
    int max = Math.max(120, widthPx - 2*PAD);
    String client = it.getClientName()==null? (it.getLabel()==null? "—" : it.getLabel()) : it.getClientName();
    int linesClient = wrapCount(client, max, true);
    int extra = 0;
    if (linesClient > 1) extra += (linesClient-1) * lineH(true);
    String site = "Chantier : " + nullToDash(it.getSiteLabel());
    int linesSite = wrapCount(site, max, false);
    if (!compact && linesSite > 1) extra += (linesSite-1) * lineH(false);
    return extra;
  }

  private int wrappedTextExtraHeightCompact(Intervention it, int widthPx){
    int max = Math.max(120, widthPx - 2*PAD);
    String client = it.getClientName()==null? (it.getLabel()==null? "—" : it.getLabel()) : it.getClientName();
    String combined = client + (it.getSiteLabel()==null? "" : " — " + it.getSiteLabel());
    int lines = wrapCount(combined, max, true);
    int extra = 0;
    if (lines > 1) extra += (lines-1) * lineH(true);
    return extra;
  }

  private static String ellipsis(Graphics2D g2, String s, int maxWidth){
    if (s==null) return "—";
    if (g2.getFontMetrics().stringWidth(s) <= maxWidth) return s;
    String dots = "…";
    int wDots = g2.getFontMetrics().stringWidth(dots);
    StringBuilder b = new StringBuilder();
    for (char c : s.toCharArray()){
      if (g2.getFontMetrics().stringWidth(b.toString()+c) > maxWidth - wDots) break;
      b.append(c);
    }
    return b.toString()+dots;
  }

  private int wrapCount(String s, int maxWidth, boolean bold){
    if (s==null) return 1;
    int w = approxCharW(bold);
    int est = Math.max(1, (int)Math.ceil((s.length()*w) / (double)maxWidth));
    return est;
  }

  private String[] wrapText(Graphics2D g2, String s, int maxWidth, boolean bold){
    if (s==null) return new String[]{ "—" };
    Font f = bold? g2.getFont().deriveFont(Font.BOLD, 16f) : g2.getFont();
    FontMetrics fm = g2.getFontMetrics(f);
    List<String> lines = new ArrayList<>();
    String[] words = s.split(" ");
    StringBuilder line = new StringBuilder();
    for (String word : words){
      String candidate = line.length()==0? word : line + " " + word;
      if (fm.stringWidth(candidate) > maxWidth && line.length()>0){
        lines.add(line.toString());
        line = new StringBuilder(word);
      } else {
        if (line.length()>0) line.append(" ");
        line.append(word);
      }
    }
    if (line.length()>0) lines.add(line.toString());
    return lines.toArray(new String[0]);
  }

  void paint(Graphics2D g2, Intervention it, Rectangle r){
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    Tier tier = tierFor(r);

    // Carte
    g2.setColor(PlanningUx.TILE_BG);
    PlanningUx.roundRect(g2, r);
    g2.setColor(PlanningUx.TILE_BORDER);
    PlanningUx.strokeRound(g2, r);

    int x = r.x + PAD;
    int y = r.y + PAD + 12;
    Font f0 = g2.getFont();
    Font fTime = PlanningUx.fontLarge(g2);
    g2.setFont(fTime);
    String time = it.prettyTimeRange()==null? "—" : it.prettyTimeRange();
    g2.setColor(new Color(0x0F172A));
    g2.drawString(time, x, y+2);
    int timeW = g2.getFontMetrics().stringWidth(time);
    g2.setFont(f0);

    // Pills status / agence si place
    int cursorX = x + (tier==Tier.XS? 8 : timeW + 12);
    cursorX = paintPill(g2, cursorX, y-10, it.getStatus()==null? "PLANNED" : it.getStatus(),
        new Color(0xE5F0FF), new Color(0x1F4FD8));
    cursorX = paintPill(g2, cursorX, y-10, it.getAgency(),
        new Color(0xEEF2FF), new Color(0x0F172A));

    // Contenu principal
    x = r.x + PAD; y = r.y + 44;
    String client = it.getClientName()==null? (it.getLabel()==null? "—" : it.getLabel()) : it.getClientName();
    int maxTextW = r.width - 2*PAD;

    switch (tier){
      case XS -> {
        g2.setFont(f0.deriveFont(Font.BOLD, 15f));
        g2.setColor(new Color(0x111827));
        String line = ellipsis(g2, client, maxTextW);
        g2.drawString(line, x, y);
        y += 18;
        g2.setFont(PlanningUx.fontRegular(g2));
        g2.setColor(new Color(0x6B7280));
        String site = nullToDash(it.getSiteLabel());
        g2.drawString(ellipsis(g2, site, maxTextW), x, y);
        y += 14;
      }
      case SM -> {
        g2.setFont(f0.deriveFont(Font.BOLD, 16f));
        g2.setColor(new Color(0x111827));
        for (String ln : wrapText(g2, client, maxTextW, true)){
          g2.drawString(ln, x, y); y += 18;
        }
        g2.setFont(PlanningUx.fontRegular(g2));
        g2.setColor(new Color(0x374151));
        String site = "Chantier : " + nullToDash(it.getSiteLabel());
        g2.drawString(ellipsis(g2, site, maxTextW), x, y); y += 16;
      }
      case MD, LG -> {
        g2.setFont(f0.deriveFont(Font.BOLD, 16f));
        g2.setColor(new Color(0x111827));
        for (String ln : wrapText(g2, client, maxTextW, true)){
          g2.drawString(ln, x, y); y += 18;
        }
        g2.setFont(PlanningUx.fontRegular(g2));
        g2.setColor(new Color(0x374151));
        String site = "Chantier : " + nullToDash(it.getSiteLabel());
        for (String ln : wrapText(g2, site, maxTextW, false)){
          g2.drawString(ln, x, y); y += 16;
        }
        g2.setColor(new Color(0x374151));
        g2.drawString("Grue : " + nullToDash(it.getCraneName()), x, y);
        int x2 = x + Math.max(120, g2.getFontMetrics().stringWidth("Grue : XXXXXXX")+10);
        g2.drawString("Camion : " + nullToDash(it.getTruckName()), x2, y);
        y += 16;
        g2.drawString("Chauffeur : " + nullToDash(it.getDriverName()), x, y);
        y += 18;
      }
    }

    int allow = switch (tier){
      case XS -> 0;
      case SM -> 1;
      case MD -> 2;
      case LG -> Integer.MAX_VALUE;
    };
    paintChipsWrapped(g2, it, x, y, r.width - 2*PAD, allow);
  }

  /* Helpers de rendu */
  private static String nullToDash(String s){ return (s==null || s.isBlank())? "—" : s; }

  private String[] chipsFor(Intervention it){
    List<String> list = new ArrayList<>();
    if (it.getQuoteNumber()!=null && !it.getQuoteNumber().isBlank())
      list.add("Devis " + it.getQuoteNumber());
    if (it.getOrderNumber()!=null && !it.getOrderNumber().isBlank())
      list.add("Commande " + it.getOrderNumber());
    if (it.getDeliveryNumber()!=null && !it.getDeliveryNumber().isBlank())
      list.add("BL " + it.getDeliveryNumber());
    if (it.getInvoiceNumber()!=null && !it.getInvoiceNumber().isBlank())
      list.add("Fact. " + it.getInvoiceNumber());
    return list.toArray(new String[0]);
  }

  private int approxChipWidth(String s){
    int text = (s==null? 1 : Math.max(1, s.length()));
    return Math.min(320, text*7 + 28); // padding + coins arrondis
  }

  private int paintPill(Graphics2D g2, int x, int y, String text, Color bg, Color fg){
    if (text==null || text.isBlank()) return x;
    int w = approxChipWidth(text);
    PlanningUx.pill(g2, new Rectangle(x, y, w, CHIP_H), bg, fg, text);
    return x + w + CHIP_GAP;
  }

  private void paintChipsWrapped(Graphics2D g2, Intervention it, int x, int y, int maxWidth, int limit){
    String[] labels = chipsFor(it);
    if (limit < labels.length){
      String[] cut = new String[limit];
      System.arraycopy(labels, 0, cut, 0, limit);
      labels = cut;
    }
    if (labels.length==0) return;
    int curX = x, curY = y;
    for (String s : labels){
      int w = approxChipWidth(s);
      if (curX > x && curX + w > x + maxWidth){
        curX = x;
        curY += CHIP_H + CHIP_GAP;
      }
      PlanningUx.pill(g2, new Rectangle(curX, curY, w, CHIP_H),
          new Color(0xE5E7EB), new Color(0x374151), s);
      curX += w + CHIP_GAP;
    }
  }
}

