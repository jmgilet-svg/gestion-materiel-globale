package com.materiel.suite.client.ui.planning;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.JComponent;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.commands.CommandBus;
import com.materiel.suite.client.ui.commands.MoveResizeInterventionCommand;

public class PlanningBoard extends JComponent {
  private LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  private int days = 7;
  private int colWidth = 100; // zoomable 60–200
  private int tileHeight = 24;
  private int rowGap = 6;
  private int snapMinutes = 15;

  private List<Resource> resources = List.of();
  private List<Intervention> interventions = List.of();
  private Map<UUID, List<Intervention>> byResource = new HashMap<>();
  private Map<Intervention, LaneLayout.Lane> lanes = new HashMap<>();
  private Map<UUID, Integer> rowHeights = new HashMap<>();
  private int totalHeight = 0;
  private Map<UUID, String> labelCache = new HashMap<>(); // FIX: cache labels

  // DnD state
  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingLeft, resizingRight;
  private Point dragStart;
  private UUID dragOverResource;
  private LocalDateTime dragStartStart, dragStartEnd;
  private boolean dragging; // FIX: threshold control

  public PlanningBoard(){
    setOpaque(true);
    setFont(new Font("Inter", Font.PLAIN, 12));
    MouseAdapter ma = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e){ onPress(e); }
      @Override public void mouseDragged(MouseEvent e){ onDrag(e); }
      @Override public void mouseReleased(MouseEvent e){ onRelease(e); }
      @Override public void mouseMoved(MouseEvent e){ onMove(e); }
    };
    addMouseListener(ma);
    addMouseMotionListener(ma);
  }

  public void setZoom(int w){ colWidth = Math.max(60, Math.min(200, w)); revalidate(); repaint(); }
  public int getColWidth(){ return colWidth; }
  public LocalDate getStartDate(){ return startDate; }
  public void setStartDate(LocalDate d){ startDate = d; reload(); }
  public void setDays(int d){ days = d; reload(); }
  public void setSnapMinutes(int m){ snapMinutes = Math.max(5, Math.min(60, m)); }
  private int pxPerHour(){ return Math.max(1, colWidth / 24); }

  public void reload(){
    resources = ServiceFactory.planning().listResources();
    interventions = ServiceFactory.planning().listInterventions(startDate, startDate.plusDays(days-1));
    for (Intervention it : interventions){ // FIX: preserve labels
      if (it.getLabel()!=null) labelCache.put(it.getId(), it.getLabel());
      else if (labelCache.containsKey(it.getId())) it.setLabel(labelCache.get(it.getId()));
    }
    byResource = interventions.stream().collect(Collectors.groupingBy(Intervention::getResourceId));
    computeLanesAndHeights();
    revalidate(); repaint();
  }

  private void computeLanesAndHeights(){
    lanes.clear(); rowHeights.clear(); totalHeight = 0;
    for (Resource r : resources){
      List<Intervention> list = byResource.getOrDefault(r.getId(), List.of());
      Map<Intervention, LaneLayout.Lane> m = LaneLayout.computeLanes(list, Intervention::getDateHeureDebut, Intervention::getDateHeureFin);
      lanes.putAll(m);
      int lanesCount = m.values().stream().mapToInt(l -> l.index).max().orElse(-1) + 1;
      int rowH = Math.max(tileHeight, lanesCount * (tileHeight + 4)) + rowGap;
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
    g2.setColor(Color.WHITE);
    g2.fillRect(0,0,getWidth(),getHeight());

    // Background grid
    int x=0;
    for (int d=0; d<days; d++){
      g2.setColor((d%2==0)? new Color(0xFAFAFA) : new Color(0xF2F2F2));
      g2.fillRect(x,0,colWidth,getHeight());
      g2.setColor(new Color(0xDDDDDD));
      g2.drawLine(x,0,x,getHeight());
      x += colWidth;
    }

    // Rows + tiles
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tileHeight+rowGap);
      g2.setColor(new Color(0xE0E0E0));
      g2.drawLine(0,y+rowH-1, getWidth(), y+rowH-1);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        paintTile(g2, it, rect);
      }
      y += rowH;
    }

    // Drag ghost
    if (dragItem!=null && dragRect!=null){
      g2.setColor(new Color(0,0,0,40));
      g2.fill(dragRect);
      g2.setColor(new Color(0,0,0,120));
      g2.draw(dragRect);
    }
    g2.dispose();
  }

  private void paintTile(Graphics2D g2, Intervention it, Rectangle r){
    Color base = parseColor(it.getColor(), new Color(0x88C0D0));
    Color dark = base.darker();
    Color light = new Color(
        Math.min(255, (int)(base.getRed()*1.08)),
        Math.min(255, (int)(base.getGreen()*1.08)),
        Math.min(255, (int)(base.getBlue()*1.08)));
    g2.setPaint(new GradientPaint(r.x, r.y, light, r.x, r.y+r.height, base));
    g2.fillRoundRect(r.x+1,r.y+1,r.width-2,r.height-2,10,10);
    g2.setColor(dark);
    g2.drawRoundRect(r.x+1,r.y+1,r.width-2,r.height-2,10,10);
    g2.setColor(new Color(0,0,0,30));
    g2.fillRoundRect(r.x+3, r.y+r.height-3, r.width-6, 3, 6,6);
    // Texte sûr et élidé si nécessaire
    String label = it.getLabel();
    if (label == null || label.isBlank()) label = "(sans titre)";
    int maxTextW = Math.max(0, r.width - 16);
    if (maxTextW > 0){
      g2.setColor(new Color(20,20,20));
      g2.setClip(r.x+8, r.y+4, r.width-16, r.height-8);
      label = ellipsize(label, g2.getFontMetrics(), maxTextW);
      int baseline = r.y + Math.max(14, Math.min(r.height-6, (r.height + g2.getFontMetrics().getAscent())/2));
      g2.drawString(label, r.x+10, baseline);
      g2.setClip(null);
    }
  }

  private Rectangle rectOf(Intervention it, int baseY){
    LocalDateTime sdt = it.getDateHeureDebut();
    LocalDateTime edt = it.getDateHeureFin();
    int startIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate.atStartOfDay(), sdt.toLocalDate().atStartOfDay());
    int endIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate.atStartOfDay(), edt.toLocalDate().atStartOfDay());
    startIdx = Math.max(0, Math.min(days-1, startIdx));
    endIdx = Math.max(startIdx, Math.min(days-1, endIdx));
    int x = startIdx * colWidth + (int) Math.round((sdt.getHour() + sdt.getMinute()/60.0) * (colWidth/24.0));
    int endX = endIdx * colWidth + (int) Math.round((edt.getHour() + edt.getMinute()/60.0) * (colWidth/24.0));
    int w = Math.max(pxPerHour(), endX - x);
    int lane = Optional.ofNullable(lanes.get(it)).map(l -> l.index).orElse(0);
    int y = baseY + lane * (tileHeight + 4);
    return new Rectangle(x, y, w, tileHeight);
  }

  private LocalDateTime snap(LocalDateTime dt){
    int m = dt.getMinute();
    int q = (m + snapMinutes/2) / snapMinutes;
    return dt.withMinute(0).withSecond(0).withNano(0).plusMinutes((long) q * snapMinutes);
  }

  private Color parseColor(String hex, Color def){
    try {
      if (hex==null || hex.isBlank()) return def;
      return new Color(Integer.parseInt(hex.replace("#",""), 16));
    } catch(Exception e){ return def; }
  }

  private static String ellipsize(String s, FontMetrics fm, int maxW){
    if (fm.stringWidth(s) <= maxW) return s;
    String ell = "…";
    int ellW = fm.stringWidth(ell);
    if (ellW >= maxW) return "";
    StringBuilder b = new StringBuilder();
    for (int i=0;i<s.length();i++){
      if (fm.stringWidth(b.toString() + s.charAt(i)) + ellW > maxW) break;
      b.append(s.charAt(i));
    }
    return b.toString() + ell;
  }

  private void onPress(MouseEvent e){
    Point p = e.getPoint();
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.get(r.getId());
      if (p.y>=y && p.y<y+rowH){
        for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
          Rectangle rect = rectOf(it, y);
          if (rect.contains(p)){
            dragItem = it;
            dragRect = new Rectangle(rect);
            dragStart = p;
            dragStartStart = it.getDateHeureDebut();
            dragStartEnd = it.getDateHeureFin();
            resizingLeft = Math.abs(p.x - rect.x) < 8;
            resizingRight = Math.abs(p.x - (rect.x+rect.width)) < 8;
            dragOverResource = r.getId();
            dragging = false; // FIX: wait for threshold
            return;
          }
        }
      }
      y += rowH;
    }
  }

  private void onDrag(MouseEvent e){
    if (dragItem==null) return;
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
    int pointerY = e.getY(); int y=0; UUID overRes = dragOverResource; // FIX: use pointer
    for (Resource res : resources){
      int rowH = rowHeights.get(res.getId());
      if (pointerY>=y && pointerY<y+rowH){
        overRes = res.getId();
        r.y = y + (lanes.getOrDefault(dragItem, new LaneLayout.Lane(0)).index)*(tileHeight+4);
        break;
      }
      y+=rowH;
    }
    dragOverResource = overRes;
    dragRect = r;
    repaint();
  }

  private void onRelease(MouseEvent e){
    if (dragItem==null || !dragging){ // FIX: ignore simple click
      dragItem = null; dragRect=null; resizingLeft=resizingRight=false; dragging=false; repaint(); return;
    }
    int startCol = Math.max(0, Math.min(days-1, (int)Math.floor(dragRect.x / (double)colWidth)));
    int startOffsetPx = dragRect.x - startCol * colWidth;
    double startHours = startOffsetPx / (colWidth/24.0);
    int endCol = Math.max(startCol, Math.min(days-1, (int)Math.floor((dragRect.x + dragRect.width) / (double)colWidth)));
    int endOffsetPx = (dragRect.x + dragRect.width) - endCol * colWidth;
    double endHours = endOffsetPx / (colWidth/24.0);
    LocalDateTime newStart = startDate.atStartOfDay().plusDays(startCol).plusMinutes((long)Math.round(startHours*60));
    LocalDateTime newEnd = startDate.atStartOfDay().plusDays(endCol).plusMinutes((long)Math.round(endHours*60));
    if (resizingLeft) {
      newEnd = dragStartEnd;
    } else if (resizingRight) {
      newStart = dragStartStart;
    }
    if (!newEnd.isAfter(newStart)) newEnd = newStart.plusMinutes(30);
    newStart = snap(newStart); newEnd = snap(newEnd);
    UUID newRes = (dragOverResource!=null? dragOverResource : dragItem.getResourceId());
    CommandBus.get().submit(new MoveResizeInterventionCommand(dragItem, newRes, newStart, newEnd));
    dragItem = null; dragRect=null; resizingLeft=resizingRight=false; dragging=false; // FIX: reset flag
    reload();
  }

  private void onMove(MouseEvent e){
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tileHeight+rowGap);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        if (rect.contains(e.getPoint())){
          if (Math.abs(e.getX()-rect.x) < 8 || Math.abs(e.getX()-(rect.x+rect.width)) < 8)
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
          else setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          return;
        }
      }
      y += rowH;
    }
    setCursor(Cursor.getDefaultCursor());
  }
}
