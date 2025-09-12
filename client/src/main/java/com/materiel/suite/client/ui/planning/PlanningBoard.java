package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class PlanningBoard extends JComponent {
  // Modèle de données & état
  private LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  private int days = 7;
  private int colWidth = 120; // zoomable

  private java.util.List<Resource> resources = java.util.List.of();
  private java.util.List<Intervention> interventions = java.util.List.of();
  private Map<UUID, java.util.List<Intervention>> byResource = new HashMap<>();
  private Map<Intervention, LaneLayout.Lane> lanes = new HashMap<>();
  private Map<UUID, Integer> rowHeights = new HashMap<>();
  private int totalHeight = 0;
  private Map<UUID, String> labelCache = new HashMap<>(); // FIX: cache labels

  // UX
  private boolean showIndispo = true;
  private String resourceFilter = "";
  private final Map<UUID,String> labelCache = new HashMap<>();
  private Intervention hovered;
  private Intervention selected;

  // DnD
  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingLeft, resizingRight;
  private Point dragStart;
  private boolean dragArmed;
  private UUID dragOverResource;

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
  public void setZoom(int w){ colWidth = Math.max(PlanningUx.COL_MIN, Math.min(PlanningUx.COL_MAX, w)); revalidate(); repaint(); }
  public int getColWidth(){ return colWidth; }
  public LocalDate getStartDate(){ return startDate; }
  public void setStartDate(LocalDate d){ startDate = d; reload(); }
  public void setDays(int d){ days = d; reload(); }
  public void setShowIndispo(boolean b){ showIndispo = b; repaint(); }
  public void setResourceNameFilter(String f){ resourceFilter = f==null? "" : f.toLowerCase(); reload(); }

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
  }

  private void computeLanesAndHeights(){
    lanes.clear(); rowHeights.clear(); totalHeight = 0;
    for (Resource r : resources){
      List<Intervention> list = byResource.getOrDefault(r.getId(), List.of());
      Map<Intervention, LaneLayout.Lane> m = LaneLayout.computeLanes(list, Intervention::getDateDebut, Intervention::getDateFin);
      lanes.putAll(m);
      int lanesCount = m.values().stream().mapToInt(l -> l.index).max().orElse(-1) + 1;
      int rowH = Math.max(PlanningUx.TILE_H, lanesCount * (PlanningUx.TILE_H + PlanningUx.LANE_GAP)) + PlanningUx.ROW_GAP;
      rowHeights.put(r.getId(), rowH);
      totalHeight += rowH;
    }
  }

  @Override public Dimension getPreferredSize(){
    return new Dimension(days*colWidth, Math.max(totalHeight, 400));
  }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(PlanningUx.BG);
    g2.fillRect(0,0,getWidth(),getHeight());

    // Background grid
    int x=0;
    for (int d=0; d<days; d++){
      g2.setColor((d%2==0)? PlanningUx.BG_ALT1 : PlanningUx.BG_ALT2);
      g2.fillRect(x,0,colWidth,getHeight());
      g2.setColor(PlanningUx.GRID);
      g2.drawLine(x,0,x,getHeight());
      x += colWidth;
    }

    // Rows + tiles
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), PlanningUx.TILE_H+PlanningUx.ROW_GAP);
      // Indispos (hachures)
      if (showIndispo){
        Rectangle hatch = new Rectangle(2*colWidth, y, colWidth*2, rowH-1);
        PlanningUx.paintHatch(g2, hatch);
      }
      // baseline
      g2.setColor(PlanningUx.ROW_DIV);
      g2.drawLine(0,y+rowH-1, getWidth(), y+rowH-1);
      // tiles for resource
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        paintTile(g2, it, rect, it==hovered, it==selected);
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

  private void paintTile(Graphics2D g2, Intervention it, Rectangle r, boolean hover, boolean sel){
    Color fill = PlanningUx.colorOr(it.getColor(), new Color(0xA3BE8C));
    Color stroke = fill.darker();
    g2.setColor(PlanningUx.TILE_SHADOW);
    g2.fillRoundRect(r.x+3,r.y+3,r.width-6,r.height-6, PlanningUx.RADIUS, PlanningUx.RADIUS);
    g2.setColor(fill);
    g2.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height-4, PlanningUx.RADIUS, PlanningUx.RADIUS);
    g2.setColor(stroke);
    g2.drawRoundRect(r.x+2,r.y+2,r.width-4,r.height-4, PlanningUx.RADIUS, PlanningUx.RADIUS);
    if (hover || sel){
      g2.setColor(hover? PlanningUx.TILE_HOVER : PlanningUx.TILE_SELECT);
      g2.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height-4, PlanningUx.RADIUS, PlanningUx.RADIUS);
    }
    // Texte sûr + élision
    String label = safeLabel(it);
    int maxTextW = Math.max(0, r.width - 2*PlanningUx.PAD);
    if (maxTextW > 0){
      g2.setColor(PlanningUx.TILE_TX);
      g2.setClip(r.x+PlanningUx.PAD, r.y+4, r.width-2*PlanningUx.PAD, r.height-8);
      label = PlanningUx.ellipsize(label, g2.getFontMetrics(), maxTextW);
      int baseline = r.y + (r.height + g2.getFontMetrics().getAscent())/2 - 2;
      g2.drawString(label, r.x+PlanningUx.PAD, baseline);
      g2.setClip(null);
    }
    g2.setColor(stroke);
    g2.fillRect(r.x+1, r.y+4, 2, r.height-8);
    g2.fillRect(r.x+r.width-3, r.y+4, 2, r.height-8);
  }

  private Rectangle rectOf(Intervention it, int baseY){
    int startIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, it.getDateDebut());
    int endIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, it.getDateFin());
    startIdx = Math.max(0, startIdx); endIdx = Math.min(days-1, endIdx);
    int x = startIdx * colWidth;
    int w = ((endIdx - startIdx) + 1) * colWidth;
    int lane = Optional.ofNullable(lanes.get(it)).map(l -> l.index).orElse(0);
    int y = baseY + lane * (PlanningUx.TILE_H + PlanningUx.LANE_GAP);
    return new Rectangle(x, y, w, PlanningUx.TILE_H);
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
      int rowH = rowHeights.getOrDefault(r.getId(), PlanningUx.TILE_H+PlanningUx.ROW_GAP);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        if (rect.contains(p)) return it;
      }
      y+=rowH;
    }
    return null;
  }

  // DnD handlers
  private void onPress(MouseEvent e){
    dragItem=null; dragRect=null; resizingLeft=false; resizingRight=false; dragArmed=false; dragOverResource=null;

    Point p = e.getPoint();
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), PlanningUx.TILE_H+PlanningUx.ROW_GAP);
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
      r.x += dx; r.width -= dx;
      if (r.width<colWidth) { r.width = colWidth; r.x = dragRect.x + (dragRect.width-colWidth); }
    } else if (resizingRight){
      r.width += dx;
      if (r.width<colWidth) r.width = colWidth;
    } else {
      r.x += dx; r.y += dy;
    }
    // snap vertical to resource row
    int y=0; UUID overRes = dragOverResource;
    for (Resource res : resources){
      int rowH = rowHeights.getOrDefault(res.getId(), PlanningUx.TILE_H+PlanningUx.ROW_GAP);
      if (r.y>=y && r.y<y+rowH){
        overRes = res.getId();
        int lane = Optional.ofNullable(lanes.get(dragItem)).map(l -> l.index).orElse(0);
        r.y = y + lane*(PlanningUx.TILE_H + PlanningUx.LANE_GAP);
        break;
      }
      y+=rowH;
    }
    dragOverResource = overRes;
    // snap horizontal to day grid
    int startCol = Math.max(0, Math.min(days-1, Math.round(r.x/(float)colWidth)));
    int endCol = Math.max(startCol, Math.min(days-1, Math.round((r.x+r.width)/(float)colWidth)-1));
    r.x = startCol * colWidth; r.width = (endCol-startCol+1) * colWidth;
    dragRect = r;
    repaint();
  }

  private void onRelease(MouseEvent e){
    if (dragItem==null){ return; }
    if (dragRect==null){ dragItem=null; return; }
    int startCol = dragRect.x / colWidth;
    int endCol = (dragRect.x + dragRect.width) / colWidth - 1;
    LocalDate newStart = startDate.plusDays(startCol);
    LocalDate newEnd = startDate.plusDays(Math.max(startCol, endCol));
    if (dragItem.getId()!=null && dragItem.getLabel()!=null && !dragItem.getLabel().isBlank()){
      labelCache.put(dragItem.getId(), dragItem.getLabel()); // FIX: refresh cache
    }
    if (dragOverResource!=null) dragItem.setResourceId(dragOverResource);
    dragItem.setDateDebut(newStart);
    dragItem.setDateFin(newEnd);
    ServiceFactory.planning().saveIntervention(dragItem);
    dragItem = null; dragRect=null; resizingLeft=resizingRight=false;
    reload();
  }

  private void onMove(MouseEvent e){
    int y=0;
    hovered = null;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), PlanningUx.TILE_H+PlanningUx.ROW_GAP);
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
