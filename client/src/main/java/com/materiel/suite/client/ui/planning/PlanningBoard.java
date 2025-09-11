package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PlanningBoard extends JComponent {
  private LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  private int days = 7;
  private int colWidth = 100;
  private int tileHeight = 22;
  private int rowGap = 6;

  private List<Resource> resources = List.of();
  private List<Intervention> interventions = List.of();
  private Map<UUID, List<Intervention>> byResource = new HashMap<>();
  private Map<Intervention, LaneLayout.Lane> lanes = new HashMap<>();
  private Map<UUID, Integer> rowHeights = new HashMap<>();
  private int totalHeight = 0;

  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingLeft, resizingRight;
  private Point dragStart;
  private UUID dragOverResource;

  public PlanningBoard(){
    setOpaque(true);
    setFont(new Font("SansSerif", Font.PLAIN, 12));
    MouseAdapter ma = new MouseAdapter(){
      @Override public void mousePressed(MouseEvent e){ onPress(e);} 
      @Override public void mouseDragged(MouseEvent e){ onDrag(e);} 
      @Override public void mouseReleased(MouseEvent e){ onRelease(e);} 
      @Override public void mouseMoved(MouseEvent e){ onMove(e);} };
    addMouseListener(ma); addMouseMotionListener(ma);
  }

  public void setZoom(int w){ colWidth = Math.max(60, Math.min(200, w)); revalidate(); repaint(); }
  public int getColWidth(){ return colWidth; }
  public LocalDate getStartDate(){ return startDate; }
  public void setStartDate(LocalDate d){ startDate=d; reload(); }
  public void setDays(int d){ days=d; reload(); }

  public void reload(){
    resources = ServiceFactory.planning().listResources();
    interventions = ServiceFactory.planning().listInterventions(startDate, startDate.plusDays(days-1));
    byResource = interventions.stream().collect(Collectors.groupingBy(Intervention::getResourceId));
    computeLanes();
    revalidate(); repaint();
  }

  private void computeLanes(){
    lanes.clear(); rowHeights.clear(); totalHeight=0;
    for (Resource r : resources){
      List<Intervention> list = byResource.getOrDefault(r.getId(), List.of());
      Map<Intervention, LaneLayout.Lane> m = LaneLayout.computeLanes(list, Intervention::getDateDebut, Intervention::getDateFin);
      lanes.putAll(m);
      int lanesCount = m.values().stream().mapToInt(l->l.index).max().orElse(-1) + 1;
      int rowH = Math.max(tileHeight, lanesCount*(tileHeight+4)) + rowGap;
      rowHeights.put(r.getId(), rowH); totalHeight += rowH;
    }
  }

  @Override public Dimension getPreferredSize(){ return new Dimension(days*colWidth, Math.max(totalHeight,400)); }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.WHITE); g2.fillRect(0,0,getWidth(),getHeight());
    int x=0;
    for(int d=0; d<days; d++){
      g2.setColor((d%2==0)? new Color(0xFAFAFA) : new Color(0xF2F2F2));
      g2.fillRect(x,0,colWidth,getHeight());
      g2.setColor(new Color(0xDDDDDD));
      g2.drawLine(x,0,x,getHeight());
      x+=colWidth;
    }
    int y=0;
    for(Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tileHeight+rowGap);
      g2.setColor(new Color(0xE0E0E0));
      g2.drawLine(0,y+rowH-1,getWidth(),y+rowH-1);
      for(Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it,y);
        paintTile(g2,it,rect);
      }
      y+=rowH;
    }
    if(dragItem!=null && dragRect!=null){
      g2.setColor(new Color(0,0,0,40)); g2.fill(dragRect);
      g2.setColor(new Color(0,0,0,120)); g2.draw(dragRect);
    }
    g2.dispose();
  }

  private void paintTile(Graphics2D g2, Intervention it, Rectangle r){
    Color fill = parseColor(it.getColor(), new Color(0xA3BE8C));
    Color stroke = fill.darker();
    g2.setColor(fill);
    g2.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,8,8);
    g2.setColor(stroke);
    g2.drawRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,8,8);
    g2.setColor(Color.DARK_GRAY);
    g2.setClip(r.x+4,r.y+4,r.width-8,r.height-8);
    g2.drawString(it.getLabel(), r.x+8, r.y+r.height/2+4);
    g2.setClip(null);
  }

  private Rectangle rectOf(Intervention it, int baseY){
    int startIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, it.getDateDebut());
    int endIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, it.getDateFin());
    startIdx = Math.max(0,startIdx); endIdx = Math.min(days-1,endIdx);
    int x = startIdx*colWidth;
    int w = (endIdx - startIdx + 1)*colWidth;
    int lane = Optional.ofNullable(lanes.get(it)).map(l->l.index).orElse(0);
    int y = baseY + lane*(tileHeight+4);
    return new Rectangle(x,y,w,tileHeight);
  }

  private Color parseColor(String hex, Color def){
    try { if(hex==null||hex.isBlank()) return def; return new Color(Integer.parseInt(hex.replace("#",""),16)); }
    catch(Exception e){ return def; }
  }

  private void onPress(MouseEvent e){
    Point p = e.getPoint();
    int y=0;
    for(Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tileHeight+rowGap);
      if(p.y>=y && p.y<y+rowH){
        for(Intervention it : byResource.getOrDefault(r.getId(), List.of())){
          Rectangle rect = rectOf(it,y);
          if(rect.contains(p)){
            dragItem=it; dragRect=new Rectangle(rect); dragStart=p; dragOverResource=r.getId();
            resizingLeft = Math.abs(p.x-rect.x)<8;
            resizingRight = Math.abs(p.x-(rect.x+rect.width))<8;
            return;
          }
        }
      }
      y+=rowH;
    }
  }

  private void onDrag(MouseEvent e){
    if(dragItem==null) return;
    int dx=e.getX()-dragStart.x; int dy=e.getY()-dragStart.y;
    Rectangle r=new Rectangle(dragRect);
    if(resizingLeft){ r.x+=dx; r.width-=dx; if(r.width<colWidth){ r.width=colWidth; r.x=dragRect.x+(dragRect.width-colWidth);} }
    else if(resizingRight){ r.width+=dx; if(r.width<colWidth) r.width=colWidth; }
    else { r.x+=dx; r.y+=dy; }
    int y=0; UUID over=dragOverResource;
    for(Resource res:resources){
      int rowH=rowHeights.get(res.getId());
      if(r.y>=y && r.y<y+rowH){ over=res.getId(); r.y=y; break; }
      y+=rowH;
    }
    dragOverResource=over;
    int startCol=Math.max(0,Math.min(days-1,Math.round(r.x/(float)colWidth)));
    int endCol=Math.max(startCol,Math.min(days-1,Math.round((r.x+r.width)/(float)colWidth)-1));
    r.x=startCol*colWidth; r.width=(endCol-startCol+1)*colWidth;
    dragRect=r; repaint();
  }

  private void onRelease(MouseEvent e){
    if(dragItem==null) return;
    int startCol=dragRect.x/colWidth;
    int endCol=(dragRect.x+dragRect.width)/colWidth-1;
    LocalDate newStart=startDate.plusDays(startCol);
    LocalDate newEnd=startDate.plusDays(Math.max(startCol,endCol));
    dragItem.setResourceId(dragOverResource);
    dragItem.setDateDebut(newStart);
    dragItem.setDateFin(newEnd);
    ServiceFactory.planning().saveIntervention(dragItem);
    dragItem=null; dragRect=null; resizingLeft=resizingRight=false;
    reload();
  }

  private void onMove(MouseEvent e){
    int y=0;
    for(Resource r:resources){
      int rowH=rowHeights.getOrDefault(r.getId(), tileHeight+rowGap);
      for(Intervention it:byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect=rectOf(it,y);
        if(rect.contains(e.getPoint())){
          if(Math.abs(e.getX()-rect.x)<8 || Math.abs(e.getX()-(rect.x+rect.width))<8)
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
          else setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          return;
        }
      }
      y+=rowH;
    }
    setCursor(Cursor.getDefaultCursor());
  }
}
