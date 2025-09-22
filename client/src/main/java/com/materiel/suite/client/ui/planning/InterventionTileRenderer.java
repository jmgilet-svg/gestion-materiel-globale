package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rendu d'une intervention dans la grille du planning. */
public final class InterventionTileRenderer {
  private static final int PAD = PlanningUx.PAD;
  private static final int R = PlanningUx.RADIUS;
  private static final int CHIP_H = 24;
  private static final int CHIP_GAP = 8;
  private static final Icon QUOTE_BADGE = IconRegistry.small("badge");
  private boolean compact = false;
  private UiDensity density = UiDensity.NORMAL;
  private double scaleY = 1.0;

  // States
  private static final Color SEL_BORDER = new Color(0x1F4FD8);
  private static final Color HOVER_BORDER = new Color(0x94A3B8);

  // Composant "tuile" (agenda compact)
  private static final Color CARD_BG = new Color(0xF7F9FC);
  private static final Color CARD_TEXT = new Color(0x1F2937);
  private static final Color CARD_SUBTEXT = new Color(0x6B7280);
  private static final Color CARD_SELECTION = new Color(0x3B82F6);
  private static final Color CARD_SELECTION_HALO = new Color(
      CARD_SELECTION.getRed(), CARD_SELECTION.getGreen(), CARD_SELECTION.getBlue(), 60);
  private static final Color STRIPE_PLANNED = new Color(0x6366F1);
  private static final Color STRIPE_IN_PROGRESS = new Color(0xF59E0B);
  private static final Color STRIPE_DONE = new Color(0x10B981);
  private static final Color STRIPE_CANCELLED = new Color(0xEF4444);
  private static final DateTimeFormatter HM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final int MICRO_THRESHOLD = 110;

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
  void setDensity(UiDensity d){
    density = d;
    scaleY = switch(d){
      case COMPACT -> 0.88;
      case SPACIOUS -> 1.15;
      default -> 1.0;
    };
  }

  /** Construit un composant léger représentant l'intervention (agenda compact). */
  public JComponent render(Intervention it, boolean selected, int widthPx){
    if (it == null){
      JPanel empty = new JPanel();
      empty.setOpaque(false);
      empty.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
      empty.setName("intervention-tile");
      return widthPx > 0 && widthPx < MICRO_THRESHOLD ? shrink(empty) : empty;
    }

    final boolean micro = widthPx > 0 && widthPx < MICRO_THRESHOLD;
    final boolean compactDensity = compact || density == UiDensity.COMPACT;
    final boolean spaciousDensity = density == UiDensity.SPACIOUS;
    final float titleSize = compactDensity ? 11f : (spaciousDensity ? 13.5f : 12.5f);
    final float subtitleSize = compactDensity ? 10.5f : (spaciousDensity ? 12f : 11.5f);
    final int headerIconSize = compactDensity ? 14 : (spaciousDensity ? 18 : 16);
    final int chipIconSize = compactDensity ? 12 : (spaciousDensity ? 15 : 14);
    final String status = nonBlankOr(it.getStatus(), "Planifiée");
    final Color stripe = stripeColor(status);

    JPanel root = new JPanel(new BorderLayout()){
      @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
          g2.setColor(CARD_BG);
          g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
          g2.setColor(stripe);
          g2.fillRoundRect(0, 0, 4, getHeight()-1, 8, 8);
          if (selected){
            g2.setColor(CARD_SELECTION_HALO);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8);
          }
        } finally {
          g2.dispose();
        }
      }
    };
    root.setOpaque(false);
    root.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    root.setName("intervention-tile");

    JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    head.setOpaque(false);
    Icon icon = iconForIntervention(it, headerIconSize);
    if (icon != null){
      head.add(new JLabel(icon));
    }
    JLabel title = new JLabel(ellipsize(titleOf(it), micro ? 14 : 28));
    title.setForeground(CARD_TEXT);
    title.setFont(title.getFont().deriveFont(Font.BOLD, titleSize));
    head.add(title);

    JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    badges.setOpaque(false);
    if (hasQuoteBadge(it)){
      badges.add(badge("📄"));
    }
    if (boolCall(it, "isHasConflicts")){
      badges.add(badge("⚠"));
    }
    if (boolCall(it, "isPriority") || it.isFavorite()){
      badges.add(badge("★"));
    }

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(head, BorderLayout.CENTER);
    if (badges.getComponentCount() > 0){
      top.add(badges, BorderLayout.EAST);
    }
    root.add(top, BorderLayout.NORTH);

    JLabel sub = new JLabel(ellipsize(clientLabel(it) + " · " + formatTime(it, micro), micro ? 18 : 40));
    sub.setForeground(CARD_SUBTEXT);
    Font subtitleFont = sub.getFont().deriveFont(subtitleSize);
    sub.setFont(subtitleFont);
    root.add(sub, BorderLayout.CENTER);

    JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
    chips.setOpaque(false);
    java.util.List<ResourceRef> refs = new ArrayList<>();
    for (ResourceRef ref : it.getResources()){
      if (ref != null){
        refs.add(ref);
      }
    }
    int limit = micro ? 3 : 6;
    int shown = 0;
    for (ResourceRef ref : refs){
      if (shown >= limit) break;
      Icon chipIcon = iconForResource(ref, chipIconSize);
      if (chipIcon != null){
        chips.add(new JLabel(chipIcon));
      }
      shown++;
    }
    if (refs.size() > shown){
      JLabel more = new JLabel("+" + (refs.size() - shown));
      more.setForeground(CARD_SUBTEXT);
      more.setFont(subtitleFont);
      chips.add(more);
    }
    root.add(chips, BorderLayout.SOUTH);

    root.setToolTipText(buildTooltip(it));
    return micro ? shrink(root) : root;
  }

  private static boolean hasQuoteBadge(Intervention it){
    if (it == null){
      return false;
    }
    if (boolCall(it, "isQuoteGenerated")){
      return true;
    }
    if (it.hasQuote()){
      return true;
    }
    String step = workflowStep(it);
    return step != null && step.equalsIgnoreCase("DEVIS");
  }

  private static boolean boolCall(Object target, String method){
    Boolean value = call(target, method, Boolean.class);
    return Boolean.TRUE.equals(value);
  }

  private static <T> T call(Object target, String method, Class<T> type){
    if (target == null || method == null || method.isBlank() || type == null){
      return null;
    }
    try {
      var m = target.getClass().getMethod(method);
      Object value = m.invoke(target);
      if (value == null){
        return null;
      }
      return type.cast(value);
    } catch (ReflectiveOperationException | ClassCastException ex){
      return null;
    }
  }

  private static String titleOf(Intervention it){
    if (it == null){
      return "Intervention";
    }
    String title = call(it, "getTitle", String.class);
    if (title != null && !title.isBlank()){
      return title.trim();
    }
    return nonBlankOr(it.getLabel(), "Intervention");
  }

  private static String clientLabel(Intervention it){
    if (it == null){
      return "—";
    }
    String client = nonBlankOr(it.getClientName(), null);
    if (client != null){
      return client;
    }
    return nonBlankOr(it.getLabel(), "—");
  }

  private static String typeLabel(Intervention it){
    if (it == null){
      return "Intervention";
    }
    var type = it.getType();
    if (type != null){
      String label = nonBlankOr(type.getLabel(), null);
      if (label != null){
        return label;
      }
      String code = nonBlankOr(type.getCode(), null);
      if (code != null){
        return code;
      }
    }
    String fallback = call(it, "getTypeLabel", String.class);
    if (fallback != null && !fallback.isBlank()){
      return fallback.trim();
    }
    fallback = call(it, "getTypeName", String.class);
    if (fallback != null && !fallback.isBlank()){
      return fallback.trim();
    }
    return "Intervention";
  }

  private static String ellipsize(String value, int max){
    if (value == null){
      return "";
    }
    if (max <= 0 || value.length() <= max){
      return value;
    }
    int cut = Math.max(0, max - 1);
    return value.substring(0, cut) + "…";
  }

  private static String nonBlankOr(String value, String fallback){
    if (value == null){
      return fallback;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? fallback : trimmed;
  }

  private static String formatTime(Intervention it, boolean micro){
    if (it == null){
      return "—";
    }
    LocalDateTime start = it.getDateHeureDebut();
    LocalDateTime end = it.getDateHeureFin();
    String startText = start != null ? HM_FORMATTER.format(start) : "—";
    if (micro){
      return startText;
    }
    String endText = end != null ? HM_FORMATTER.format(end) : "—";
    if ("—".equals(startText) && "—".equals(endText)){
      String pretty = it.prettyTimeRange();
      return pretty != null ? pretty : "—";
    }
    return startText + "–" + endText;
  }

  private static String tooltipTimeRange(Intervention it){
    if (it == null){
      return "—";
    }
    LocalDateTime start = it.getDateHeureDebut();
    LocalDateTime end = it.getDateHeureFin();
    if (start == null && end == null){
      String pretty = it.prettyTimeRange();
      return pretty != null ? pretty : "—";
    }
    String startText = start != null ? HM_FORMATTER.format(start) : "—";
    String endText = end != null ? HM_FORMATTER.format(end) : "—";
    return startText + " – " + endText;
  }

  private static String buildTooltip(Intervention it){
    if (it == null){
      return null;
    }
    String title = titleOf(it);
    String client = clientLabel(it);
    String type = typeLabel(it);
    String status = nonBlankOr(it.getStatus(), "Planifiée");
    String range = tooltipTimeRange(it);
    return "<html><b>" + escape(title) + "</b><br/>"
        + escape(client) + "<br/><i>" + escape(type) + "</i><br/>"
        + escape(range) + " · " + escape(status) + "</html>";
  }

  private static Color stripeColor(String status){
    if (status == null){
      return STRIPE_PLANNED;
    }
    String normalized = status.trim().toUpperCase(Locale.ROOT)
        .replace('-', '_').replace(' ', '_');
    if (normalized.contains("EN_COURS") || normalized.contains("IN_PROGRESS")){
      return STRIPE_IN_PROGRESS;
    }
    if (normalized.contains("TERMINEE") || normalized.contains("TERMINÉE")
        || normalized.contains("TERMINE") || normalized.contains("DONE")
        || normalized.contains("COMPLETED")){
      return STRIPE_DONE;
    }
    if (normalized.contains("ANNULEE") || normalized.contains("ANNULÉE")
        || normalized.contains("ANNULE") || normalized.contains("CANCELED")
        || normalized.contains("CANCELLED")){
      return STRIPE_CANCELLED;
    }
    return STRIPE_PLANNED;
  }

  private static Icon iconForIntervention(Intervention it, int size){
    if (size <= 0){
      return null;
    }
    String key = null;
    if (it != null){
      var type = it.getType();
      if (type != null){
        String iconKey = nonBlankOr(type.getIconKey(), null);
        if (iconKey != null){
          key = iconKey;
        } else {
          String label = nonBlankOr(type.getLabel(), null);
          if (label != null){
            key = mapToIconKey(label);
          } else {
            String code = nonBlankOr(type.getCode(), null);
            if (code != null){
              key = mapToIconKey(code);
            }
          }
        }
      }
      if (key == null){
        String fallback = call(it, "getTypeLabel", String.class);
        if (fallback != null && !fallback.isBlank()){
          key = mapToIconKey(fallback);
        }
      }
      if (key == null){
        String fallback = call(it, "getTypeName", String.class);
        if (fallback != null && !fallback.isBlank()){
          key = mapToIconKey(fallback);
        }
      }
    }
    if (key == null){
      key = "task";
    }
    return IconRegistry.loadOrPlaceholder(key, size);
  }

  private static Icon iconForResource(ResourceRef ref, int size){
    if (size <= 0){
      return null;
    }
    if (ref == null){
      return IconRegistry.placeholder(size);
    }
    String key = nonBlankOr(ref.getIcon(), null);
    if (key == null){
      key = mapToIconKey(ref.getName());
    }
    return IconRegistry.loadOrPlaceholder(key, size);
  }

  private static String mapToIconKey(String raw){
    if (raw == null){
      return "task";
    }
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()){
      return "task";
    }
    if (normalized.contains("grue")){
      return "crane";
    }
    if (normalized.contains("camion")){
      return "truck";
    }
    if (normalized.contains("manut")){
      return "forklift";
    }
    if (normalized.contains("conteneur")){
      return "container";
    }
    if (normalized.contains("pellete") || normalized.contains("excava")){
      return "excavator";
    }
    if (normalized.contains("generateur") || normalized.contains("générateur")){
      return "generator";
    }
    if (normalized.contains("crochet") || normalized.contains("levage")){
      return "hook";
    }
    if (normalized.contains("casque") || normalized.contains("chantier")
        || normalized.contains("securite") || normalized.contains("sécurité")){
      return "helmet";
    }
    if (normalized.contains("palette")){
      return "pallet";
    }
    if (normalized.contains("fact")){
      return "file";
    }
    return normalized;
  }

  private static String workflowStep(Intervention it){
    if (it == null){
      return null;
    }
    String step = call(it, "getWorkflowStep", String.class);
    if (step != null && !step.isBlank()){
      return step.trim();
    }
    return nonBlankOr(it.getWorkflowStage(), null);
  }

  private static String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private static JComponent shrink(JComponent component){
    if (component != null){
      component.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
    }
    return component;
  }

  private static JComponent badge(String text){
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(11f));
    return label;
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

  // --- Compatibilité avec anciens appels ---
  // Ancien ordre : (g2, rect, it, selected, hovered)
  void paint(Graphics2D g2, Rectangle r, Intervention it, boolean selected, boolean hovered){
    paint(g2, it, r, selected, hovered);
  }
  // Ancien ordre sans flags
  void paint(Graphics2D g2, Rectangle r, Intervention it){
    paint(g2, it, r, false, false);
  }
  // Nouveau ordre "canonique" sans flags
  void paint(Graphics2D g2, Intervention it, Rectangle r){
    paint(g2, it, r, false, false);
  }

  // Rendu principal avec états
  void paint(Graphics2D g2, Intervention it, Rectangle r, boolean selected, boolean hovered){
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    Tier tier = tierFor(r);

    // Carte
    g2.setColor(PlanningUx.TILE_BG);
    PlanningUx.roundRect(g2, r);
    // bord dynamique
    Color border = selected ? SEL_BORDER : (hovered ? HOVER_BORDER : PlanningUx.TILE_BORDER);
    g2.setColor(border);
    if (selected){
      var old = g2.getStroke();
      g2.setStroke(new BasicStroke(2f));
      PlanningUx.strokeRound(g2, r);
      g2.setStroke(old);
    } else {
      PlanningUx.strokeRound(g2, r);
    }

    if (QUOTE_BADGE != null){
      String quoteRef = quoteReference(it);
      if (quoteRef != null){
        int ix = r.x + r.width - QUOTE_BADGE.getIconWidth() - PAD;
        int iy = r.y + PAD;
        QUOTE_BADGE.paintIcon(null, g2, ix, iy);
      }
    }

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

    // Cadenas si verrouillé (si champ présent)
    boolean locked = false;
    try {
      var m = it.getClass().getMethod("isLocked");
      locked = Boolean.TRUE.equals(m.invoke(it));
    } catch(Exception ignore){}
    if (locked){
      int lx = r.x + r.width - 20;
      int ly = r.y + 8;
      g2.setColor(new Color(0x374151));
      g2.drawRect(lx, ly, 10, 8);
      g2.drawLine(lx+2, ly, lx+8, ly);
    }

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
    Rectangle iconZone = new Rectangle(r.x + PAD, r.y + r.height - 20, r.width - 2*PAD, 16);
    InterventionIconPainter.paintIcons(g2, iconZone, it.getResources());
  }

  /* Helpers de rendu */
  private static String nullToDash(String s){ return (s==null || s.isBlank())? "—" : s; }

  private String[] chipsFor(Intervention it){
    List<String> list = new ArrayList<>();
    String quote = quoteReference(it);
    if (quote != null)
      list.add("Devis " + quote);
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

  private String quoteReference(Intervention it){
    if (it == null){
      return null;
    }
    String ref = it.getQuoteReference();
    if (ref == null || ref.isBlank()){
      ref = it.getQuoteNumber();
    }
    if (ref == null || ref.isBlank()){
      return null;
    }
    return ref;
  }
}

