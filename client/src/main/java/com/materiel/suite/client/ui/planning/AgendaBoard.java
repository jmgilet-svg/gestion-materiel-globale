package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.commands.CommandBus;
import com.materiel.suite.client.ui.commands.MoveResizeInterventionCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mode Agenda : heures à la verticale, jours en colonnes; DnD vertical pour l'heure, horizontal pour le jour.
 */
public class AgendaBoard extends JComponent {
  private LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  private int days = 7;
  private int dayWidth = 140;
  private int hourHeight = 40; // 40 px = 1h
  private int snapMinutes = 15;

  private List<Resource> resources = List.of();
  private List<Intervention> interventions = List.of();
  private Map<UUID, List<Intervention>> byResource = new HashMap<>();
  // lanes par jour : clé "itId|dayIdx" -> lane index; et "resId|dayIdx" -> max lanes
  private Map<String, Integer> dayLaneIndex = new HashMap<>();
  private Map<String, Integer> dayLaneMax = new HashMap<>();

  private Map<UUID, Integer> rowHeights = new HashMap<>();
  private int totalHeight = 0;

  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingTop, resizingBottom;
  private Point dragStart;
  private UUID dragOverResource;
  private LocalDateTime dragStartStart, dragStartEnd;

  public AgendaBoard(){
    setOpaque(true);
    setFont(new Font("Inter", Font.PLAIN, 12));
    MouseAdapter ma = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e){ onPress(e); }
      @Override public void mouseDragged(MouseEvent e){ onDrag(e); }
      @Override public void mouseReleased(MouseEvent e){ onRelease(e); }
    };
    addMouseListener(ma);
    addMouseMotionListener(ma);
  }

  public void setStartDate(LocalDate d){ startDate = d; reload(); }
  public LocalDate getStartDate(){ return startDate; }
  public void setDays(int d){ days = d; reload(); }
  public int getDayWidth(){ return dayWidth; }
  public void setDayWidth(int w){ dayWidth = Math.max(80, Math.min(240, w)); revalidate(); repaint(); }
  public void setSnapMinutes(int m){ snapMinutes = Math.max(5, Math.min(60, m)); }

  public void reload(){
    resources = ServiceFactory.planning().listResources();
    interventions = ServiceFactory.planning().listInterventions(startDate, startDate.plusDays(days-1));
    byResource = interventions.stream().collect(Collectors.groupingBy(Intervention::getResourceId));
    computeDayLanes();
    computeHeights();
    revalidate(); repaint();
  }

  private void computeHeights(){
    rowHeights.clear(); totalHeight=0;
    int base = hourHeight * 24 + 20; // 24h + padding
    for (Resource r : resources){
      rowHeights.put(r.getId(), base);
      totalHeight += base;
    }
  }

  private void computeDayLanes(){
    dayLaneIndex.clear(); dayLaneMax.clear();
    for (Resource r : resources){
      for (int d=0; d<days; d++){
        LocalDate day = startDate.plusDays(d);
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.atTime(LocalTime.MAX);
        List<Intervention> list = byResource.getOrDefault(r.getId(), List.of()).stream()
            .filter(it -> !(it.getDateHeureFin().isBefore(dayStart) || it.getDateHeureDebut().isAfter(dayEnd)))
            .sorted(Comparator.comparing(Intervention::getDateHeureDebut))
            .collect(Collectors.toList());
        List<LocalDateTime> laneEnds = new ArrayList<>();
        int maxLane = 0;
        for (Intervention it : list){
          LocalDateTime s = it.getDateHeureDebut().isBefore(dayStart)? dayStart : it.getDateHeureDebut();
          LocalDateTime e = it.getDateHeureFin().isAfter(dayEnd)? dayEnd : it.getDateHeureFin();
          int lane = -1;
          for (int i=0;i<laneEnds.size();i++){
            if (laneEnds.get(i).isBefore(s)) { lane = i; break; }
          }
          if (lane==-1){ lane = laneEnds.size(); laneEnds.add(e); }
          else { laneEnds.set(lane, e); }
          dayLaneIndex.put(it.getId()+"|"+d, lane);
          maxLane = Math.max(maxLane, lane+1);
        }
        dayLaneMax.put(r.getId()+"|"+d, Math.max(1, maxLane));
      }
    }
  }
  @Override public Dimension getPreferredSize(){
    return new Dimension(days*dayWidth, Math.max(totalHeight, 400));
  }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.WHITE);
    g2.fillRect(0,0,getWidth(),getHeight());

    // colonnes jours
    int x=0;
    for (int d=0; d<days; d++){
      g2.setColor((d%2==0)? new Color(0xFBFBFB) : new Color(0xF4F4F4));
      g2.fillRect(x,0,dayWidth,getHeight());
      g2.setColor(new Color(0xDDDDDD));
      g2.drawLine(x,0,x,getHeight());
      x += dayWidth;
    }
    // lignes heures
    for (int h=0; h<=24; h++){
      int y = h*hourHeight;
      g2.setColor(h%6==0? new Color(0xCCCCCC) : new Color(0xE8E8E8));
      g2.drawLine(0,y,getWidth(),y);
    }

    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.get(r.getId());
      g2.setColor(new Color(0xE0E0E0));
      g2.drawLine(0,y+rowH-1,getWidth(),y+rowH-1);

      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        paintTile(g2, it, rect);
      }
      y += rowH;
    }

    if (dragItem!=null && dragRect!=null){
      g2.setColor(new Color(0,0,0,40));
      g2.fill(dragRect);
      g2.setColor(new Color(0,0,0,120));
      g2.draw(dragRect);
    }
    g2.dispose();
  }

  private void paintTile(Graphics2D g2, Intervention it, Rectangle r){
    Color base = parseColor(it.getColor(), new Color(0x8FBCBB));
    g2.setColor(base);
    g2.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,10,10);
    g2.setColor(base.darker());
    g2.drawRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,10,10);
    g2.setColor(new Color(20,20,20));
    g2.setClip(r.x+8, r.y+4, r.width-16, r.height-8);
    g2.drawString(it.getLabel(), r.x+10, r.y + Math.min(r.height-6, 16));
    g2.setClip(null);
  }

  private Rectangle rectOf(Intervention it, int rowTop){
    // colonne = jour, y = heure
    int dayIdx = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, it.getDateHeureDebut().toLocalDate());
    dayIdx = Math.max(0, Math.min(days-1, dayIdx));
    int xBase = dayIdx * dayWidth;
    int y = rowTop + (int) Math.round(it.getDateHeureDebut().getHour()*hourHeight + it.getDateHeureDebut().getMinute()*(hourHeight/60.0));
    int y2 = rowTop + (int) Math.round(it.getDateHeureFin().getHour()*hourHeight + it.getDateHeureFin().getMinute()*(hourHeight/60.0));
    int h = Math.max(12, y2 - y);
    int lane = dayLaneIndex.getOrDefault(it.getId()+"|"+dayIdx, 0);
    int max = dayLaneMax.getOrDefault(it.getResourceId()+"|"+dayIdx, 1);
    int inner = dayWidth - 12;
    int colW = Math.max(40, inner / Math.max(1, max));
    int pad = 4;
    int x = xBase + 6 + lane * colW + pad;
    int w = Math.max(30, colW - 2*pad);
    return new Rectangle(x, y, w, h);

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
            dragOverResource = r.getId();
            dragStartStart = it.getDateHeureDebut();
            dragStartEnd = it.getDateHeureFin();
            resizingTop = Math.abs(p.y - rect.y) < 8;
            resizingBottom = Math.abs(p.y - (rect.y+rect.height)) < 8;
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
    Rectangle r = new Rectangle(dragRect);
    if (resizingTop){
      r.y += dy; r.height -= dy;
      if (r.height<12){ r.height=12; r.y = dragRect.y + (dragRect.height - 12); }
    } else if (resizingBottom){
      r.height += dy; if (r.height<12) r.height = 12;
    } else {
      r.x += dx; r.y += dy;
    }
    // snap ressource (vertical segment)
    int rowTop = 0; UUID over = dragOverResource;
    for (Resource res : resources){
      int rh = rowHeights.get(res.getId());
      if (r.y>=rowTop && r.y<rowTop+rh){ over = res.getId(); break; }
      rowTop += rh;
    }
    dragOverResource = over;

    // snap minutes
    int minutePx = (int)Math.round(hourHeight/60.0);
    int mod = r.y % (snapMinutes*minutePx);
    r.y -= mod;
    dragRect = r;
    repaint();
  }

  private void onRelease(MouseEvent e){
    if (dragItem==null) return;
    // delta jours
    int startDay = Math.max(0, Math.min(days-1, dragRect.x / dayWidth));
    int deltaDay = startDay - (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, dragStartStart.toLocalDate());
    // delta minutes
    int dy = dragRect.y - rectOf(dragItem, rowTopOf(dragOverResource)).y;
    int minutesDelta = (int) Math.round(dy * (60.0/hourHeight));

    var newStart = dragStartStart.plusDays(deltaDay).plusMinutes(minutesDelta);
    var newEnd = dragStartEnd.plusDays(deltaDay);
    if (resizingTop){ newEnd = dragStartEnd; }
    if (resizingBottom){ newStart = dragStartStart; newEnd = newStart.plusMinutes(Math.max(30, (int)Math.round(dragRect.height * (60.0/hourHeight)))); }
    if (!newEnd.isAfter(newStart)) newEnd = newStart.plusMinutes(30);

    CommandBus.get().submit(new MoveResizeInterventionCommand(dragItem, dragOverResource, newStart, newEnd));
    dragItem=null; dragRect=null; resizingTop=resizingBottom=false;
    reload();
  }

  private int rowTopOf(UUID resourceId){
    int y=0;
    for (Resource r : resources){
      if (r.getId().equals(resourceId)) return y;
      y += rowHeights.get(r.getId());
    }
    return 0;
  }

  private Color parseColor(String hex, Color def){
    try {
      if (hex==null || hex.isBlank()) return def;
      return new Color(Integer.parseInt(hex.replace("#",""), 16));
    } catch(Exception e){ return def; }
  }
}
