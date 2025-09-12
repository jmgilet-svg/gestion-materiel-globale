package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class PlanningBoard extends JComponent {
  // Modèle de données & état
  private LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  private int days = 7;
  // Grille horizontale en slots de 15 min
  private int slotMinutes = 15;
  private int slotsPerDay = (60/slotMinutes)*24; // 96
  private int slotWidth = 12; // 12 px * 96 ≈ 1152 px / jour (scrollable)

  private java.util.List<Resource> resources = java.util.List.of();
  private java.util.List<Intervention> interventions = java.util.List.of();
  private Map<UUID, java.util.List<Intervention>> byResource = new HashMap<>();
  private Map<Intervention, LaneLayout.Lane> lanes = new HashMap<>();
  private Map<UUID, Integer> rowHeights = new HashMap<>();
  private Map<UUID, Integer> rowTileHeights = new HashMap<>();
  private int totalHeight = 0;
  private int lastLayoutHash = 0;

  // UX
  private boolean showIndispo = true;
  private String resourceFilter = "";
  private final Map<UUID,String> labelCache = new HashMap<>();
  private Intervention hovered;
  private Intervention selected;
  private final InterventionTileRenderer tile = new InterventionTileRenderer();
  private boolean compact = false;

  // DnD
  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingLeft, resizingRight;
  private Point dragStart;
  private boolean dragArmed;
  private UUID dragOverResource;
  private boolean dragging;

  public PlanningBoard(){
    setOpaque(true);
    setFont(PlanningUx.uiFont(this));

    MouseAdapter ma = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e){ onPress(e); }
      @Override public void mouseDragged(MouseEvent e){ onDrag(e); }
      @Override public void mouseReleased(MouseEvent e){ onRelease(e); }
      @Override public void mouseMoved(MouseEvent e){ onMove(e); }
      @Override public void mouseClicked(MouseEvent e){ onClick(e); }
    };
    addMouseListener(ma);
    addMouseMotionListener(ma);

    setComponentPopupMenu(buildContextMenu());
    setToolTipText(""); // active les tooltips
  }

  private JPopupMenu buildContextMenu(){
    JPopupMenu menu = new JPopupMenu();
    JMenuItem miEdit = new JMenuItem("Renommer…");
    miEdit.addActionListener(e -> {
      if (selected==null) return;
      String current = selected.getLabel()==null? "" : selected.getLabel();
      String s = JOptionPane.showInputDialog(this, "Libellé :", current);
      if (s!=null){
        selected.setLabel(s.trim());
        if (selected.getId()!=null) labelCache.put(selected.getId(), selected.getLabel());
        ServiceFactory.planning().saveIntervention(selected);
        repaint();
      }
    });
    JMenuItem miDelete = new JMenuItem("Supprimer");
    miDelete.addActionListener(e -> {
      if (selected==null) return;
      int ok = JOptionPane.showConfirmDialog(this, "Supprimer l'intervention « "+safeLabel(selected)+" » ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
      if (ok==JOptionPane.OK_OPTION){
        ServiceFactory.planning().deleteIntervention(selected.getId());
        reload();
      }
    });
    menu.add(miEdit);
    menu.add(miDelete);
    return menu;
  }

  @Override public String getToolTipText(MouseEvent e){
    Intervention hit = hitTile(e.getPoint());
    if (hit==null) return null;
    return String.format("<html><b>%s</b><br>%s → %s</html>",
        safeLabel(hit),
        hit.getDateDebut(), hit.getDateFin());
  }

  // API publique
  /** Expose les ressources visibles pour synchroniser le RowHeader. */
  public java.util.List<Resource> getResourcesList(){ return resources; }
  /** Hauteur d'une ligne (ressource). */
  public int rowHeight(UUID resId){ return rowHeights.getOrDefault(resId, tile.height() + PlanningUx.ROW_GAP); }
  /** Durée d'un slot en minutes. */
  public int getSlotMinutes(){ return slotMinutes; }
  public boolean isCompact(){ return compact; }
  public void setCompact(boolean c){ this.compact = c; this.tile.setCompact(c); reload(); }
  /** Largeur d'un slot en pixels. */
  public int getSlotWidth(){ return slotWidth; }
  /** Nombre de slots par jour. */
  public int getSlotsPerDay(){ return slotsPerDay; }
  /** Largeur d'un jour en pixels. */
  public int getDayPixelWidth(){ return slotsPerDay * slotWidth; }

  /** Zoom horizontal : taille d'un slot. */
  public void setZoom(int w){
    slotWidth = Math.max(6, Math.min(24, w));
    revalidate(); repaint();
    firePropertyChange("layout", 0, 1);
  }
  /** Compat : largeur d'un jour pour DayHeader. */
  public int getColWidth(){ return getDayPixelWidth(); }
  public LocalDate getStartDate(){ return startDate; }
  public void setStartDate(LocalDate d){ startDate = d; reload(); }
  public void setDays(int d){ days = d; reload(); }
  public void setShowIndispo(boolean b){ showIndispo = b; repaint(); }
  public void setResourceNameFilter(String f){ resourceFilter = f==null? "" : f.toLowerCase(); reload(); }
  public void setSlotMinutes(int minutes){
    slotMinutes = Math.max(5, Math.min(60, minutes));
    slotsPerDay = (60/slotMinutes)*24;
    revalidate(); repaint();
    firePropertyChange("layout", 0, 1);
  }
  public void setSnapMinutes(int m){ setSlotMinutes(m); }
  public int tileHeight(){ return tile.height(); }

  public void reload(){
    resources = ServiceFactory.planning().listResources().stream()
        .filter(r -> resourceFilter.isBlank() || r.getName().toLowerCase().contains(resourceFilter))
        .collect(Collectors.toList());
    interventions = ServiceFactory.planning().listInterventions(startDate, startDate.plusDays(days-1));
    for (Intervention it : interventions){ // FIX: preserve labels
      if (it.getLabel()!=null) labelCache.put(it.getId(), it.getLabel());
      else if (labelCache.containsKey(it.getId())) it.setLabel(labelCache.get(it.getId()));
    }
    byResource = interventions.stream().collect(Collectors.groupingBy(Intervention::getResourceId));
    for (Intervention it : interventions){
      if (it.getLabel()!=null) labelCache.put(it.getId(), it.getLabel()); // FIX: cache label
      else if (labelCache.containsKey(it.getId())) it.setLabel(labelCache.get(it.getId())); // FIX: restore label
    }
    computeLanesAndHeights();
    revalidate(); repaint();
    firePropertyChange("layout", 0, 1);
  }

  private void computeLanesAndHeights(){
    lanes.clear(); rowHeights.clear(); rowTileHeights.clear(); totalHeight = 0;
    for (Resource r : resources){
      List<Intervention> list = byResource.getOrDefault(r.getId(), List.of());
      Map<Intervention, LaneLayout.Lane> m = LaneLayout.computeLanes(list,
          Intervention::getStartDateTime, Intervention::getEndDateTime);
      lanes.putAll(m);
      int lanesCount = m.values().stream().mapToInt(l -> l.index).max().orElse(-1) + 1;

      int rowTileH = tile.height();
      for (Intervention it : list){
        int width = tilePixelWidthFor(it);
        rowTileH = Math.max(rowTileH, tile.heightFor(it, width));
      }
      rowTileHeights.put(r.getId(), rowTileH);

      int rowH = Math.max(rowTileH, lanesCount * (rowTileH + PlanningUx.LANE_GAP)) + PlanningUx.ROW_GAP;
      rowHeights.put(r.getId(), rowH);
      totalHeight += rowH;
    }
    // Permet de déclencher un repaint du header uniquement si nécessaire
    lastLayoutHash = Objects.hash(resources.size(), rowHeights.hashCode(), days, slotWidth, slotMinutes);
  }

  @Override public Dimension getPreferredSize(){
    return new Dimension(days*getDayPixelWidth(), Math.max(totalHeight, 400));
  }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(PlanningUx.BG);
    g2.fillRect(0,0,getWidth(),getHeight());

    // Background grid
    int x=0, dayW = getDayPixelWidth();
    for (int d=0; d<days; d++){
      g2.setColor((d%2==0)? PlanningUx.BG_ALT1 : PlanningUx.BG_ALT2);
      g2.fillRect(x,0,dayW,getHeight());
      g2.setColor(PlanningUx.GRID);
      g2.drawLine(x,0,x,getHeight());
      for (int h=0; h<24; h+=2){
        int px = x + h*60/slotMinutes*slotWidth;
        g2.setColor(new Color(0xEEEEEE));
        g2.drawLine(px,0,px,getHeight());
      }
      x += dayW;
    }

    // Rows + tiles
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tile.height()+PlanningUx.ROW_GAP);
      // Indispos (hachures)
      if (showIndispo){
        Rectangle hatch = new Rectangle(2*dayW, y, dayW*2, rowH-1);
        PlanningUx.paintHatch(g2, hatch);
      }
      // baseline
      g2.setColor(PlanningUx.ROW_DIV);
      g2.drawLine(0,y+rowH-1, getWidth(), y+rowH-1);
      // tiles for resource
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        tile.paint(g2, rect, it, it==hovered, it==selected);
      }
      y += rowH;
    }

    // Drag ghost
    if (dragItem!=null && dragRect!=null){
      g2.setColor(PlanningUx.TILE_SHADOW);
      g2.fill(dragRect);
      g2.setColor(new Color(0,0,0,120));
      g2.draw(dragRect);
    }
    g2.dispose();
  }

  private int slotIndex(LocalDateTime dt){
    int dayIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, dt.toLocalDate());
    int mins = dt.getHour()*60 + dt.getMinute();
    int slot = mins / slotMinutes;
    return dayIdx * slotsPerDay + slot;
  }
  private int xFromSlot(int slot){
    return slot * slotWidth;
  }
  private Rectangle rectOf(Intervention it, int baseY){
    LocalDateTime s = it.getDateHeureDebut()!=null? it.getDateHeureDebut() : it.getDateDebut().atStartOfDay();
    LocalDateTime e = it.getDateHeureFin()!=null? it.getDateHeureFin() : it.getDateFin().plusDays(1).atStartOfDay();
    int sIdx = Math.max(0, slotIndex(s));
    int eIdx = Math.min(days*slotsPerDay-1, slotIndex(e)-1);
    int x = xFromSlot(sIdx);
    int w = Math.max(slotWidth, (eIdx - sIdx + 1) * slotWidth);
    int lane = Optional.ofNullable(lanes.get(it)).map(l -> l.index).orElse(0);
    int rowTileH = rowTileHeights.getOrDefault(it.getResourceId(), tile.height());
    int y = baseY + lane * (rowTileH + PlanningUx.LANE_GAP);
    return new Rectangle(x, y, w, rowTileH);
  }

  private int tilePixelWidthFor(Intervention it){
    var s = it.getStartDateTime();
    var e = it.getEndDateTime();
    int sIdx = slotIndex(s);
    int eIdx = slotIndex(e) - 1;
    eIdx = Math.max(eIdx, sIdx);
    int slots = (eIdx - sIdx + 1);
    return Math.max(slotWidth, slots * slotWidth) - 8;
  }

  private String safeLabel(Intervention it){
    String label = it.getLabel();
    if (label == null || label.isBlank()) {
      if (it.getId()!=null) return labelCache.getOrDefault(it.getId(), "(sans titre)");
      return "(sans titre)";
    }
    return label;
  }

  private Intervention hitTile(Point p){
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tile.height()+PlanningUx.ROW_GAP);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        if (rect.contains(p)) return it;
      }
      y+=rowH;
    }
    return null;
  }

  // Helpers de snap robuste
  private int clamp(int v, int min, int max){ return Math.max(min, Math.min(max, v)); }
  private int slotAtLeft(int x){ return clamp((int)Math.floor(x/(double)slotWidth), 0, days*slotsPerDay-1); }
  private int slotAtRight(int x){ return clamp((int)Math.ceil(x/(double)slotWidth)-1, 0, days*slotsPerDay-1); }
  private int slotSnap(int x){ return clamp((int)Math.round(x/(double)slotWidth), 0, days*slotsPerDay-1); }

  // DnD handlers
  private void onPress(MouseEvent e){
    dragItem=null; dragRect=null; resizingLeft=false; resizingRight=false; dragArmed=false; dragOverResource=null; dragging=false;

    Point p = e.getPoint();
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tile.height()+PlanningUx.ROW_GAP);
      if (p.y>=y && p.y<y+rowH){
        for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
          Rectangle rect = rectOf(it, y);
          if (rect.contains(p)){
            dragItem = it;
            dragRect = new Rectangle(rect);
            dragStart = p;
            dragArmed = true;
            resizingLeft = Math.abs(p.x - rect.x) < PlanningUx.HANDLE;
            resizingRight = Math.abs(p.x - (rect.x+rect.width)) < PlanningUx.HANDLE;
            dragOverResource = r.getId();
            selected = it; // FIX: select tile on press
            return;
          }
        }
      }
      y += rowH;
    }
  }

  private void onDrag(MouseEvent e){
    if (dragItem==null) return;
    if (dragArmed && e.getPoint().distance(dragStart) < PlanningUx.DRAG_THRESHOLD){
      return; // FIX: ignore micro-mouvements
    } else {
      dragArmed = false;
    }
    int dx = e.getX() - dragStart.x;
    int dy = e.getY() - dragStart.y;
    if (!dragging && Math.hypot(dx, dy) < 6) return; // FIX: start threshold
    dragging = true; // FIX:
    Rectangle r = new Rectangle(dragRect);

    if (resizingLeft){
      int newStart = slotAtLeft(e.getX());
      int end = slotAtRight(r.x + r.width);
      if (newStart > end) newStart = end;
      r.x = xFromSlot(newStart);
      r.width = Math.max(slotWidth, xFromSlot(end+1) - r.x);
    } else if (resizingRight){
      int start = slotAtLeft(r.x);
      int newEnd = slotAtRight(e.getX());
      if (newEnd < start) newEnd = start;
      r.width = Math.max(slotWidth, xFromSlot(newEnd+1) - xFromSlot(start));
    } else {
      int start = slotSnap(r.x + dx);
      start = clamp(start, 0, days*slotsPerDay-1);
      int widthSlots = Math.max(1, (int)Math.round(r.width/(double)slotWidth));
      int end = clamp(start + widthSlots - 1, 0, days*slotsPerDay-1);
      start = end - widthSlots + 1;
      r.x = xFromSlot(start);
      r.y += dy;
    }
    // snap vertical à la ressource cible
    int y=0; UUID overRes = dragOverResource;
    for (Resource res : resources){
      int rowH = rowHeights.get(res.getId());
      if (r.y>=y && r.y<y+rowH){ overRes = res.getId(); r.y = y + (lanes.getOrDefault(dragItem, new LaneLayout.Lane(0)).index)*(tile.height()+PlanningUx.LANE_GAP); break; }
      y+=rowH;
    }
    dragOverResource = overRes;
    // snap horizontal aux slots
    int startSlot = slotAtLeft(r.x);
    int endSlot = slotAtRight(r.x + r.width);
    r.x = xFromSlot(startSlot);
    r.width = Math.max(slotWidth, xFromSlot(endSlot+1) - r.x);
    dragRect = r;
    repaint();
  }

  private void onRelease(MouseEvent e){
    if (dragItem==null){ return; }
    if (dragRect==null){ dragItem=null; return; }
    // compute new dates and/or resource
    int startSlot = slotAtLeft(dragRect.x);
    int endSlot = slotAtRight(dragRect.x + dragRect.width);
    int startDay = startSlot / slotsPerDay;
    int startMins = (startSlot % slotsPerDay) * slotMinutes;
    int endDay = endSlot / slotsPerDay;
    int endMins = ((endSlot % slotsPerDay) + 1) * slotMinutes;
    LocalDateTime s = startDate.plusDays(startDay).atStartOfDay().plusMinutes(startMins);
    LocalDateTime e2 = startDate.plusDays(endDay).atStartOfDay().plusMinutes(endMins);
    if (dragItem.getId()!=null && dragItem.getLabel()!=null && !dragItem.getLabel().isBlank()){
      labelCache.put(dragItem.getId(), dragItem.getLabel());
    }
    if (dragOverResource!=null) dragItem.setResourceId(dragOverResource);
    dragItem.setDateHeureDebut(s);
    dragItem.setDateHeureFin(e2);
    dragItem.setDateDebut(s.toLocalDate());
    dragItem.setDateFin(e2.toLocalDate());
    ServiceFactory.planning().saveIntervention(dragItem);
    dragItem = null; dragRect=null; resizingLeft=resizingRight=false; dragging=false;
    reload();
  }

  private void onMove(MouseEvent e){
    int y=0;
    hovered = null;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tile.height()+PlanningUx.ROW_GAP);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        if (rect.contains(e.getPoint())){
          hovered = it;
          if (Math.abs(e.getX()-rect.x) < PlanningUx.HANDLE || Math.abs(e.getX()-(rect.x+rect.width))<PlanningUx.HANDLE){
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
          } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          }
          repaint(rect.x-2, rect.y-2, rect.width+4, rect.height+4);
          return;
        }
      }
      y+=rowH;
    }
    setCursor(Cursor.getDefaultCursor());
    repaint();
  }

  private void onClick(MouseEvent e){
    if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==1){
      selected = hitTile(e.getPoint());
      repaint();
    }
  }
}
