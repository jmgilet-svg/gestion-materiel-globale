package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Regroupement par journée pour une lecture "agenda" rapide. */
public class InterventionCalendarView extends JPanel implements InterventionView {
  private static final Color HEADER_BG = new Color(245, 245, 245);
  private static final Color DROP_TARGET_BG = new Color(232, 245, 233);
  private static final int START_HOUR = 6;
  private static final int END_HOUR = 20;
  private static final int HOUR_HEIGHT = 40;
  private static final int SLOT_MINUTES = 15;
  private static final int COLUMN_WIDTH = 180;
  private static final int RESIZE_HANDLE = 6;
  private static final Icon QUOTE_BADGE = IconRegistry.small("badge");

  private final JPanel root = new JPanel(new BorderLayout());
  private final JPanel days = new JPanel();
  private final JScrollPane dayScroll = new JScrollPane(days);
  private final JScrollPane weekScroll = new JScrollPane();
  private final InterventionTileRenderer tileRenderer = new InterventionTileRenderer();
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
  private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
  private Consumer<Intervention> onOpen = it -> {};
  private BiConsumer<Intervention, LocalDate> onMove = (it, day) -> {};
  private BiConsumer<Intervention, java.util.Date> onMoveDateTime = (it, date) -> {};
  private BiConsumer<Intervention, java.util.Date> onResizeDateTime = (it, date) -> {};
  private Intervention dragging;
  private JComponent dragSource;
  private List<Intervention> current = List.of();
  private String mode = "Semaine";

  public InterventionCalendarView(){
    days.setLayout(new BoxLayout(days, BoxLayout.Y_AXIS));
    days.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    dayScroll.setBorder(BorderFactory.createEmptyBorder());
    dayScroll.getVerticalScrollBar().setUnitIncrement(18);
    weekScroll.setBorder(BorderFactory.createEmptyBorder());
    weekScroll.getVerticalScrollBar().setUnitIncrement(18);
    root.add(dayScroll, BorderLayout.CENTER);
    days.addMouseListener(new MouseAdapter(){
      @Override public void mouseReleased(MouseEvent e){
        dragging = null;
        if (dragSource != null){
          dragSource.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          dragSource = null;
        }
      }
    });
  }

  @Override public JComponent getComponent(){
    return root;
  }

  @Override public void setData(List<Intervention> list){
    current = sanitize(list);
    renderCurrent();
  }

  @Override public void setOnOpen(Consumer<Intervention> onOpen){
    this.onOpen = onOpen != null ? onOpen : it -> {};
  }

  @Override public void setOnMove(BiConsumer<Intervention, LocalDate> onMove){
    this.onMove = onMove != null ? onMove : (it, day) -> {};
  }

  @Override public void setOnMoveDateTime(BiConsumer<Intervention, java.util.Date> onMoveDateTime){
    this.onMoveDateTime = onMoveDateTime != null ? onMoveDateTime : (it, date) -> {};
  }

  @Override public void setOnResizeDateTime(BiConsumer<Intervention, java.util.Date> onResizeDateTime){
    this.onResizeDateTime = onResizeDateTime != null ? onResizeDateTime : (it, date) -> {};
  }

  @Override public void setMode(String mode){
    this.mode = mode != null ? mode : "Semaine";
    root.removeAll();
    if (isWeekMode()){
      root.add(weekScroll, BorderLayout.CENTER);
    } else {
      root.add(dayScroll, BorderLayout.CENTER);
    }
    root.revalidate();
    root.repaint();
    renderCurrent();
  }

  private boolean isWeekMode(){
    return "Semaine".equalsIgnoreCase(mode);
  }

  private void renderCurrent(){
    if (isWeekMode()){
      renderWeek();
    } else {
      renderDays();
    }
  }

  private List<Intervention> sanitize(List<Intervention> list){
    if (list == null || list.isEmpty()){
      return List.of();
    }
    List<Intervention> data = new ArrayList<>();
    for (Intervention it : list){
      if (it != null){
        data.add(it);
      }
    }
    return data;
  }

  private void renderDays(){
    days.removeAll();
    dragging = null;
    dragSource = null;
    if (current.isEmpty()){
      days.add(emptyState());
      refreshDays();
      return;
    }
    Map<LocalDate, List<Intervention>> byDay = new TreeMap<>();
    for (Intervention it : current){
      LocalDate day = dayOf(it);
      byDay.computeIfAbsent(day, d -> new ArrayList<>()).add(it);
    }
    for (Map.Entry<LocalDate, List<Intervention>> entry : byDay.entrySet()){
      days.add(dayHeader(entry.getKey()));
      entry.getValue().stream()
          .sorted(Comparator.comparing(this::startDateTime))
          .forEach(it -> days.add(row(it)));
      days.add(Box.createVerticalStrut(6));
    }
    refreshDays();
  }

  private void refreshDays(){
    days.revalidate();
    days.repaint();
  }

  private JComponent emptyState(){
    JLabel label = new JLabel("Aucune intervention", JLabel.CENTER);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
    return label;
  }

  private JComponent dayHeader(LocalDate day){
    String text = dayFormatter.format(day);
    JLabel label = new JLabel(capitalize(text), IconRegistry.small("calendar"), JLabel.LEFT);
    label.setOpaque(true);
    label.setBackground(HEADER_BG);
    label.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
        BorderFactory.createEmptyBorder(6, 8, 4, 8)
    ));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.addMouseListener(new MouseAdapter(){
      @Override public void mouseEntered(MouseEvent e){
        if (dragging != null){
          label.setBackground(DROP_TARGET_BG);
        }
      }
      @Override public void mouseExited(MouseEvent e){
        label.setBackground(HEADER_BG);
      }
      @Override public void mouseReleased(MouseEvent e){
        if (dragging != null){
          try {
            onMove.accept(dragging, day);
          } finally {
            dragging = null;
          }
        }
        if (dragSource != null){
          dragSource.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          dragSource = null;
        }
        label.setBackground(HEADER_BG);
      }
    });
    return label;
  }

  private JComponent row(Intervention it){
    CalendarTile tile = new CalendarTile(it);
    String quote = quoteReference(it);
    if (quote != null){
      tile.setToolTipText("Devis " + quote);
    }
    tile.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPopupMenu menu = buildContextMenu(it);
    tile.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          if (!AccessControl.canEditInterventions()){
            Toasts.info(tile, "Ouverture en lecture seule");
          }
          onOpen.accept(it);
        }
      }
      @Override public void mouseEntered(MouseEvent e){
        tile.setHovered(true);
      }
      @Override public void mouseExited(MouseEvent e){
        tile.setHovered(false);
        if (dragging == null){
          tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      }
      @Override public void mousePressed(MouseEvent e){
        if (e.isPopupTrigger()){
          menu.show(tile, e.getX(), e.getY());
          return;
        }
        if (SwingUtilities.isRightMouseButton(e)){
          return;
        }
        dragging = it;
        dragSource = tile;
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      }
      @Override public void mouseReleased(MouseEvent e){
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)){
          menu.show(tile, e.getX(), e.getY());
          return;
        }
        if (dragging == it){
          dragging = null;
        }
        if (dragSource == tile){
          dragSource = null;
        }
      }
    });
    return tile;
  }

  private final class CalendarTile extends JComponent {
    private final Intervention intervention;
    private boolean hovered;

    CalendarTile(Intervention intervention){
      this.intervention = intervention;
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
    }

    void setHovered(boolean hovered){
      if (this.hovered != hovered){
        this.hovered = hovered;
        repaint();
      }
    }

    @Override public Dimension getPreferredSize(){
      Insets insets = getInsets();
      int width = widthHint();
      int height = tileRenderer.heightFor(intervention, width);
      return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
    }

    @Override public Dimension getMaximumSize(){
      Dimension pref = getPreferredSize();
      return new Dimension(Integer.MAX_VALUE, pref.height);
    }

    @Override public void setBounds(int x, int y, int width, int height){
      Insets insets = getInsets();
      int innerWidth = Math.max(200, width - insets.left - insets.right);
      int innerHeight = tileRenderer.heightFor(intervention, innerWidth);
      int totalHeight = innerHeight + insets.top + insets.bottom;
      super.setBounds(x, y, width, totalHeight);
    }

    private int widthHint(){
      int width = getWidth();
      if (width <= 0){
        Container parent = getParent();
        if (parent != null){
          width = parent.getWidth();
        }
      }
      if (width <= 0 && days.getParent() != null){
        width = days.getParent().getWidth();
      }
      if (width <= 0){
        width = days.getWidth();
      }
      if (width <= 0){
        width = 320;
      }
      Insets insets = getInsets();
      width -= insets.left + insets.right;
      return Math.max(200, width);
    }

    @Override protected void paintComponent(Graphics g){
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        Insets insets = getInsets();
        Rectangle r = new Rectangle(
            insets.left,
            insets.top,
            Math.max(0, getWidth() - insets.left - insets.right),
            Math.max(0, getHeight() - insets.top - insets.bottom));
        tileRenderer.paint(g2, intervention, r, false, hovered);
      } finally {
        g2.dispose();
      }
    }
  }

  private JPopupMenu buildContextMenu(Intervention intervention){
    JPopupMenu menu = new JPopupMenu();
    JMenuItem edit = new JMenuItem("Modifier…", IconRegistry.small("edit"));
    edit.addActionListener(e -> onOpen.accept(intervention));
    menu.add(edit);
    return menu;
  }

  private void renderWeek(){
    List<LocalDate> daysList = current.stream()
        .map(this::weekDay)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
    if (daysList.isEmpty()){
      JPanel p = new JPanel(new BorderLayout());
      p.add(emptyState(), BorderLayout.CENTER);
      weekScroll.setViewportView(p);
      weekScroll.revalidate();
      weekScroll.repaint();
      return;
    }

    JPanel header = new JPanel(new GridLayout(1, daysList.size() + 1));
    header.setBackground(Color.WHITE);
    header.add(new JLabel(""));
    for (LocalDate day : daysList){
      JLabel l = new JLabel(capitalize(dayFormatter.format(day)), SwingConstants.CENTER);
      l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(230, 230, 230)));
      header.add(l);
    }

    JPanel grid = new JPanel(new GridBagLayout());
    grid.setBackground(Color.WHITE);
    GridBagConstraints gc = new GridBagConstraints();
    gc.fill = GridBagConstraints.BOTH;
    gc.gridy = 0;
    gc.weighty = 1;
    gc.insets = new Insets(0, 0, 0, 0);

    gc.gridx = 0;
    gc.weightx = 0;
    grid.add(buildHourColumn(), gc);

    int column = 1;
    for (LocalDate day : daysList){
      gc.gridx = column++;
      gc.weightx = 1;
      grid.add(new DayColumn(day, filterForDay(day)), gc);
    }

    JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(header, BorderLayout.NORTH);
    wrap.add(grid, BorderLayout.CENTER);
    weekScroll.setViewportView(wrap);
    weekScroll.revalidate();
    weekScroll.repaint();
  }

  private JComponent buildHourColumn(){
    int totalHeight = (END_HOUR - START_HOUR) * HOUR_HEIGHT;
    return new JComponent(){
      @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        g.setColor(new Color(120, 120, 120));
        for (int h = START_HOUR; h <= END_HOUR; h++){
          int y = (h - START_HOUR) * HOUR_HEIGHT;
          g.drawString(String.format("%02d:00", h), 6, y + 12);
        }
      }
      @Override public Dimension getPreferredSize(){
        return new Dimension(60, totalHeight);
      }
    };
  }

  private List<Intervention> filterForDay(LocalDate day){
    return current.stream()
        .filter(it -> {
          LocalDateTime start = effectiveStart(it);
          return start != null && day.equals(start.toLocalDate());
        })
        .sorted(Comparator.comparing(this::effectiveStart))
        .collect(Collectors.toList());
  }

  private LocalDate weekDay(Intervention it){
    LocalDateTime start = effectiveStart(it);
    return start != null ? start.toLocalDate() : null;
  }

  private LocalDate dayOf(Intervention it){
    LocalDateTime start = it.getDateHeureDebut();
    if (start != null){
      return start.toLocalDate();
    }
    LocalDate day = it.getDateDebut();
    if (day != null){
      return day;
    }
    return LocalDate.of(1970, 1, 1);
  }

  private LocalDateTime startDateTime(Intervention it){
    LocalDateTime start = it.getDateHeureDebut();
    if (start != null){
      return start;
    }
    LocalDate day = it.getDateDebut();
    if (day != null){
      return day.atStartOfDay();
    }
    return LocalDateTime.MIN;
  }

  private LocalDateTime effectiveStart(Intervention it){
    LocalDateTime start = it.getDateHeureDebut();
    if (start != null){
      return start;
    }
    LocalDate day = it.getDateDebut();
    if (day != null){
      return day.atTime(START_HOUR, 0);
    }
    return null;
  }

  private LocalDateTime effectiveEnd(Intervention it, LocalDateTime start){
    LocalDateTime end = it.getDateHeureFin();
    if (end != null){
      return end;
    }
    LocalDate day = it.getDateFin();
    if (day != null){
      return day.atTime(END_HOUR, 0);
    }
    if (start != null){
      return start.plusHours(1);
    }
    return null;
  }

  private String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .trim();
  }

  private String capitalize(String value){
    if (value == null || value.isBlank()){
      return "";
    }
    return value.substring(0, 1).toUpperCase(Locale.FRENCH) + value.substring(1);
  }

  private javax.swing.border.Border badgeBorder(javax.swing.border.Border base, String quote){
    if (quote != null && !quote.isBlank() && QUOTE_BADGE != null){
      return BorderFactory.createCompoundBorder(new BadgeBorder(QUOTE_BADGE), base);
    }
    return base;
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

  private class DayColumn extends JPanel {
    private final LocalDate day;
    private Integer hoverGuideY;
    private final JLabel dragTip;

    DayColumn(LocalDate day, List<Intervention> items){
      super(null);
      this.day = day;
      this.hoverGuideY = null;
      this.dragTip = createDragTip();
      setOpaque(true);
      setBackground(Color.WHITE);
      setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(235, 235, 235)));
      setPreferredSize(new Dimension(COLUMN_WIDTH, (END_HOUR - START_HOUR) * HOUR_HEIGHT));
      for (Intervention it : items){
        LocalDateTime start = effectiveStart(it);
        if (start == null){
          continue;
        }
        LocalDateTime end = effectiveEnd(it, start);
        if (end == null){
          end = start.plusHours(1);
        }
        add(makeBlock(it, start, end));
      }
      add(dragTip);
      setComponentZOrder(dragTip, 0);
      addMouseMotionListener(new java.awt.event.MouseMotionAdapter(){
        @Override public void mouseMoved(MouseEvent e){
          setHoverGuide(snapY(e.getY()));
        }
      });
      addMouseListener(new MouseAdapter(){
        @Override public void mouseExited(MouseEvent e){
          setHoverGuide(null);
        }
      });
    }

    @Override protected void paintComponent(Graphics g){
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setColor(new Color(245, 245, 245));
      for (int hour = START_HOUR; hour <= END_HOUR; hour++){
        int y = (hour - START_HOUR) * HOUR_HEIGHT;
        g2.drawLine(0, y, getWidth(), y);
        if (hour < END_HOUR){
          g2.setColor(new Color(236, 236, 236));
          g2.drawLine(0, y + HOUR_HEIGHT / 2, getWidth(), y + HOUR_HEIGHT / 2);
          g2.setColor(new Color(245, 245, 245));
        }
      }
      if (hoverGuideY != null){
        int y = Math.max(0, Math.min(hoverGuideY, columnHeight()));
        g2.setColor(new Color(60, 60, 60, 90));
        g2.drawLine(0, y, getWidth(), y);
      }
      g2.dispose();
    }

    private JComponent makeBlock(Intervention it, LocalDateTime start, LocalDateTime end){
      LocalDateTime localStart = alignToDay(start);
      LocalDateTime localEnd = alignToDay(end);
      int y = snapY(toY(localStart));
      int h = snapHeight(Math.max(HOUR_HEIGHT / 2, toY(localEnd) - toY(localStart)));
      JPanel block = new JPanel(new BorderLayout()){
        private boolean resizing;
        private int pressY;
        private int pressHeight;
        private int pressBlockY;
        {
          setOpaque(true);
          setBackground(new Color(197, 225, 250));
          String quoteInfo = quoteReference(it);
          javax.swing.border.Border baseBorder = BorderFactory.createLineBorder(new Color(144, 202, 249));
          setBorder(badgeBorder(baseBorder, quoteInfo));
          if (quoteInfo != null){
            setToolTipText("Devis " + quoteInfo);
          }
          JLabel label = new JLabel(blockLabel(it, localStart, localEnd));
          InterventionType type = it.getType();
          if (type != null && type.getIconKey() != null){
            label.setIcon(IconRegistry.small(type.getIconKey()));
          }
          label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
          add(label, BorderLayout.CENTER);
          JPopupMenu menu = buildContextMenu(it);
          addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
              if (e.getClickCount() == 2){
                onOpen.accept(it);
              }
            }
            @Override public void mousePressed(MouseEvent e){
              if (e.isPopupTrigger()){
                menu.show(InterventionCalendarView.this, e.getX(), e.getY());
                return;
              }
              if (SwingUtilities.isRightMouseButton(e)){
                return;
              }
              pressY = e.getYOnScreen();
              pressHeight = getHeight();
              pressBlockY = getY();
              resizing = e.getY() >= getHeight() - RESIZE_HANDLE;
              setCursor(Cursor.getPredefinedCursor(resizing ? Cursor.S_RESIZE_CURSOR : Cursor.MOVE_CURSOR));
              updateDragTip(it, getBounds());
              setHoverGuide(resizing ? getY() + getHeight() : getY());
            }
            @Override public void mouseReleased(MouseEvent e){
              setCursor(Cursor.getDefaultCursor());
              boolean popup = e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e);
              if (popup){
                hideDragTip();
                setHoverGuide(null);
                menu.show(InterventionCalendarView.this, e.getX(), e.getY());
                return;
              }
              LocalDateTime baseStart = startOfY(getY());
              if (resizing){
                LocalDateTime newEnd = snapQuarter(baseStart.plusMinutes(heightToMinutes(getHeight())));
                onResizeDateTime.accept(it, toDate(newEnd));
              } else {
                LocalDateTime newStart = snapQuarter(baseStart);
                onMoveDateTime.accept(it, toDate(newStart));
              }
              hideDragTip();
              setHoverGuide(null);
            }
          });
          addMouseMotionListener(new java.awt.event.MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
              int dy = e.getYOnScreen() - pressY;
              int limit = columnHeight();
              if (resizing){
                int nh = clamp(pressHeight + dy, slotStep(), Math.max(slotStep(), limit - getY()));
                nh = snapHeight(nh);
                setSize(getWidth(), nh);
                Rectangle bounds = getBounds();
                updateDragTip(it, bounds);
                setHoverGuide(bounds.y + bounds.height);
              } else {
                int ny = clamp(pressBlockY + dy, 0, Math.max(0, limit - getHeight()));
                ny = snapY(ny);
                setLocation(getX(), ny);
                Rectangle bounds = getBounds();
                updateDragTip(it, bounds);
                setHoverGuide(bounds.y);
              }
              getParent().repaint();
            }
          });
        }
      };
      block.setBounds(8, y, COLUMN_WIDTH - 16, h);
      return block;
    }

    private int toY(LocalDateTime dateTime){
      LocalDateTime base = day.atTime(START_HOUR, 0);
      long minutes = java.time.Duration.between(base, dateTime).toMinutes();
      double pxPerMinute = HOUR_HEIGHT / 60.0;
      return (int) Math.round(minutes * pxPerMinute);
    }

    private LocalDateTime alignToDay(LocalDateTime value){
      if (value == null){
        return day.atTime(START_HOUR, 0);
      }
      if (!Objects.equals(value.toLocalDate(), day)){
        return day.atTime(value.toLocalTime());
      }
      return value;
    }

    private int heightToMinutes(int height){
      double minutes = height * (60.0 / HOUR_HEIGHT);
      return (int) Math.max(SLOT_MINUTES, Math.round(minutes));
    }

    private int snapY(int y){
      int step = slotStep();
      if (step <= 0){
        return y;
      }
      int snapped = (y / step) * step;
      int max = columnHeight() - step;
      return Math.max(0, Math.min(snapped, Math.max(0, max)));
    }

    private int snapHeight(int h){
      int step = slotStep();
      if (step <= 0){
        return h;
      }
      int snapped = (h / step) * step;
      if (snapped < step){
        snapped = step;
      }
      return snapped;
    }

    private int slotStep(){
      return (int) Math.max(1, Math.round(SLOT_MINUTES * (HOUR_HEIGHT / 60.0)));
    }

    private int clamp(int value, int min, int max){
      return Math.max(min, Math.min(max, value));
    }

    private int columnHeight(){
      int h = getHeight();
      if (h <= 0){
        h = getPreferredSize().height;
      }
      return Math.max(h, 0);
    }

    private void setHoverGuide(Integer y){
      Integer target = y;
      if (target != null){
        target = Math.max(0, Math.min(target, columnHeight()));
      }
      if (!Objects.equals(hoverGuideY, target)){
        hoverGuideY = target;
        repaint();
      }
    }

    private JLabel createDragTip(){
      JLabel tip = new JLabel("", SwingConstants.LEFT);
      tip.setOpaque(true);
      tip.setBackground(new Color(255, 255, 255, 230));
      tip.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(new Color(160, 160, 160)),
          BorderFactory.createEmptyBorder(3, 6, 3, 6)
      ));
      tip.setFont(tip.getFont().deriveFont(tip.getFont().getSize2D() - 1f));
      tip.setVisible(false);
      tip.setFocusable(false);
      return tip;
    }

    private void updateDragTip(Intervention it, Rectangle bounds){
      if (it == null || bounds == null){
        hideDragTip();
        return;
      }
      LocalDateTime start = snapQuarter(startOfY(bounds.y));
      LocalDateTime end = snapQuarter(start.plusMinutes(heightToMinutes(bounds.height)));
      String startText = start != null ? timeFormatter.format(start) : "";
      String endText = end != null ? timeFormatter.format(end) : "";
      String rawLabel = it.getLabel();
      if (rawLabel == null || rawLabel.isBlank()){
        rawLabel = "Intervention";
      }
      String label = escape(rawLabel);
      dragTip.setText("<html><b>" + startText + "–" + endText + "</b> • " + label + "</html>");
      Dimension pref = dragTip.getPreferredSize();
      int width = Math.max(getWidth(), COLUMN_WIDTH);
      int tx = Math.max(4, Math.min(bounds.x + 6, width - pref.width - 4));
      int ty = Math.max(4, bounds.y - pref.height - 6);
      dragTip.setBounds(tx, ty, pref.width, pref.height);
      dragTip.setVisible(true);
      setComponentZOrder(dragTip, 0);
      dragTip.repaint();
    }

    private void hideDragTip(){
      dragTip.setVisible(false);
    }

    private LocalDateTime startOfY(int y){
      int minutes = (int) Math.round(y * (60.0 / HOUR_HEIGHT));
      if (minutes < 0){
        minutes = 0;
      }
      LocalDateTime base = day.atTime(START_HOUR, 0).plusMinutes(minutes);
      LocalDateTime max = day.atTime(END_HOUR, 0);
      if (base.isAfter(max)){
        base = max.minusMinutes(SLOT_MINUTES);
      }
      return base;
    }

    private LocalDateTime snapQuarter(LocalDateTime dateTime){
      if (dateTime == null){
        return day.atTime(START_HOUR, 0);
      }
      int minute = dateTime.getMinute();
      int snapped = (minute / SLOT_MINUTES) * SLOT_MINUTES;
      LocalDateTime aligned = dateTime.withMinute(snapped).withSecond(0).withNano(0);
      LocalDateTime min = day.atTime(START_HOUR, 0);
      LocalDateTime max = day.atTime(END_HOUR, 0);
      if (aligned.isBefore(min)){
        return min;
      }
      if (aligned.isAfter(max)){
        return max;
      }
      return aligned;
    }

    private java.util.Date toDate(LocalDateTime dateTime){
      return java.util.Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String blockLabel(Intervention it, LocalDateTime start, LocalDateTime end){
      StringBuilder sb = new StringBuilder("<html><b>")
          .append(escape(it.getLabel()))
          .append("</b>");
      if (start != null){
        sb.append(" ").append(timeFormatter.format(start));
        if (end != null){
          sb.append("–").append(timeFormatter.format(end));
        }
      }
      String address = escape(it.getAddress());
      if (!address.isBlank()){
        sb.append("<br/><span style='color:#555555'>").append(address).append("</span>");
      }
      sb.append("</html>");
      return sb.toString();
    }
  }
}
