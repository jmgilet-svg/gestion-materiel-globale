package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.interventions.InterventionDialog;
import com.materiel.suite.client.service.PlanningValidation;

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
import com.materiel.suite.client.ui.MainFrame;

/**
 * Mode Agenda : heures à la verticale, jours en colonnes; DnD vertical pour l'heure, horizontal pour le jour.
 */
public class AgendaBoard extends JComponent {
  private LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
  private int days = 7;
  private int dayWidth = 140;
  private int hourHeight = 40; // 40 px = 1h
  private int snapMinutes = 15;
  private static final int DRAG_THRESHOLD = PlanningUx.DRAG_THRESHOLD; // FIX: shared drag threshold
  private static final int CREATE_THRESHOLD = PlanningUx.CREATE_THRESHOLD; // FIX: shared create threshold

  private List<Resource> resources = List.of();
  private List<Intervention> interventions = List.of();
  private Map<UUID, List<Intervention>> byResource = new HashMap<>();
  // lanes par jour : clé "itId|dayIdx" -> lane index; et "resId|dayIdx" -> max lanes
  private Map<String, Integer> dayLaneIndex = new HashMap<>();
  private Map<String, Integer> dayLaneMax = new HashMap<>();
  private Map<UUID, Integer> rowHeights = new HashMap<>();
  private int totalHeight = 0;
  private Map<UUID, String> labelCache = new HashMap<>(); // FIX: cache labels

  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingTop, resizingBottom;
  private Point dragStart;
  private UUID dragOverResource;
  private LocalDateTime dragStartStart, dragStartEnd;
  private boolean dragging; // FIX: threshold control
  private boolean creating; // FIX: creation mode
  private Intervention contextItem;

  public AgendaBoard(){
    setOpaque(true);
    setFont(new Font("Inter", Font.PLAIN, 12));
    MouseAdapter ma = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e){ onPress(e); }
      @Override public void mouseDragged(MouseEvent e){ onDrag(e); }
      @Override public void mouseReleased(MouseEvent e){ onRelease(e); }
      @Override public void mouseClicked(MouseEvent e){ onClick(e); }
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
    for (Intervention it : interventions){ // FIX: preserve labels
      if (it.getLabel()!=null) labelCache.put(it.getId(), it.getLabel());
      else if (labelCache.containsKey(it.getId())) it.setLabel(labelCache.get(it.getId()));
    }
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
    g2.setColor(PlanningUx.BG); // FIX: palette
    g2.fillRect(0,0,getWidth(),getHeight());

    // colonnes jours
    int x=0;
    for (int d=0; d<days; d++){
      g2.setColor((d%2==0)? PlanningUx.BG_ALT1 : PlanningUx.BG_ALT2); // FIX: alternating bg
      g2.fillRect(x,0,dayWidth,getHeight());
      g2.setColor(PlanningUx.GRID); // FIX: grid color
      g2.drawLine(x,0,x,getHeight());
      x += dayWidth;
    }
    // lignes heures
    for (int h=0; h<=24; h++){
      int y = h*hourHeight;
      g2.setColor(h%6==0? new Color(0xCCCCCC) : new Color(0xE5E7EB));
      g2.drawLine(0,y,getWidth(),y);
    }

    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.get(r.getId());
      g2.setColor(PlanningUx.ROW_DIV); // FIX: row divider
      g2.drawLine(0,y+rowH-1,getWidth(),y+rowH-1);
      var list = byResource.getOrDefault(r.getId(), List.of());
      highlightConflicts(g2, list, y);
      for (Intervention it : list){
        Rectangle rect = rectOf(it, y);
        paintTile(g2, it, rect);
      }
      y += rowH;
    }

    if (dragItem!=null && dragRect!=null){
      g2.setColor(PlanningUx.TILE_SHADOW); // FIX: ghost tile shadow
      g2.fill(dragRect);
      g2.setColor(new Color(0,0,0,120));
      g2.draw(dragRect);
    }
    g2.dispose();
  }

  private void paintTile(Graphics2D g2, Intervention it, Rectangle r){
    Color base = PlanningUx.colorOr(it.getColor(), new Color(0x8FBCBB)); // FIX: parse color
    g2.setColor(base);
    g2.fillRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,10,10);
    g2.setColor(base.darker());
    g2.drawRoundRect(r.x+2,r.y+2,r.width-4,r.height-4,10,10);
    // Texte safe + ellipsize
    String label = it.getLabel();
    if (label == null || label.isBlank()) label = labelCache.getOrDefault(it.getId(), "(sans titre)"); // FIX: cache fallback
    int maxTextW = Math.max(0, r.width - 20);
    if (maxTextW > 0){
      g2.setClip(r.x+8, r.y+4, r.width-16, r.height-8);
      int textX = r.x + 10;
      int textY = r.y + 20;
      Font base2 = g2.getFont();
      // === CRM-INJECT BEGIN: agenda-board-client-text ===
      String client = it.getClientName();
      if (client!=null && !client.isBlank()){
        Font bold = base2.deriveFont(Font.BOLD, base2.getSize2D()+1f);
        g2.setFont(bold);
        g2.setColor(new Color(0x111827));
        String clientLine = PlanningUx.ellipsize(client, g2.getFontMetrics(), maxTextW);
        g2.drawString(clientLine, textX, textY);
        textY += g2.getFontMetrics().getHeight();
        g2.setFont(base2);
      }
      // === CRM-INJECT END ===
      g2.setColor(PlanningUx.TILE_TX); // FIX: text color
      String labelLine = PlanningUx.ellipsize(label, g2.getFontMetrics(), maxTextW); // FIX: shared ellipsize
      g2.drawString(labelLine, textX, textY);
      g2.setFont(base2);
      g2.setClip(null);
    }
  }

  /** Draw dashed red borders around overlapping interventions of a resource row. */
  private void highlightConflicts(Graphics2D g2, List<Intervention> list, int rowTop){
    for (int i=0;i<list.size();i++){
      Intervention a = list.get(i);
      LocalDateTime as = a.getDateHeureDebut();
      LocalDateTime ae = a.getDateHeureFin();
      for (int j=i+1;j<list.size();j++){
        Intervention b = list.get(j);
        LocalDateTime bs = b.getDateHeureDebut();
        LocalDateTime be = b.getDateHeureFin();
        boolean overlap = !ae.isBefore(bs) && !be.isBefore(as);
        if (overlap){
          Rectangle ra = rectOf(a, rowTop);
          Rectangle rb = rectOf(b, rowTop);
          Stroke old = g2.getStroke();
          g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f,4f}, 0f));
          g2.setColor(new Color(0xCC3333));
          g2.draw(ra);
          g2.draw(rb);
          g2.setStroke(old);
        }
      }
    }
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
            if (it.isLocked() || "DONE".equalsIgnoreCase(it.getStatus())){
              Toolkit.getDefaultToolkit().beep();
              return;
            }
            dragItem = it;
            dragRect = new Rectangle(rect);
            dragStart = p;
            dragOverResource = r.getId();
            dragStartStart = it.getDateHeureDebut();
            dragStartEnd = it.getDateHeureFin();
            resizingTop = Math.abs(p.y - rect.y) < PlanningUx.HANDLE; // FIX: handle zone
            resizingBottom = Math.abs(p.y - (rect.y+rect.height)) < PlanningUx.HANDLE; // FIX: handle zone
            dragging = false; // FIX: wait for threshold
            creating = false; // FIX: existing item
            return;
          }
        }
        dragItem = null; dragRect = new Rectangle(p.x, p.y, 0, 0); // FIX: prepare creation
        dragStart = p; dragOverResource = r.getId();
        dragging = false; creating = true; // FIX:
        return;
      }
      y += rowH;
    }
  }

  private void onDrag(MouseEvent e){
    if (dragItem!=null){
      int dx = e.getX() - dragStart.x;
      int dy = e.getY() - dragStart.y;
      if (!dragging && Math.hypot(dx, dy) < DRAG_THRESHOLD) return; // FIX: start threshold
      dragging = true; // FIX:
      Rectangle r = new Rectangle(dragRect);
      if (resizingTop){
        r.y += dy; r.height -= dy;
        if (r.height<12){ r.height=12; r.y = dragRect.y + (dragRect.height - 12); }
      } else if (resizingBottom){
        r.height += dy; if (r.height<12) r.height = 12;
      } else {
        r.x += dx; r.y += dy;
      }
      int pointerY = e.getY(); int rowTop = 0; UUID over = dragOverResource; // FIX: use pointer
      for (Resource res : resources){
        int rh = rowHeights.get(res.getId());
        if (pointerY>=rowTop && pointerY<rowTop+rh){ over = res.getId(); break; }
        rowTop += rh;
      }
      dragOverResource = over;
      int minutePx = (int)Math.round(hourHeight/60.0); int snap = snapMinutes*minutePx; // FIX: stable snap
      r.y = rowTop + ((r.y - rowTop)/snap)*snap;
      dragRect = r;
      repaint();
      return;
    }
    if (!creating) return;
    int dx = e.getX() - dragStart.x;
    int dy = e.getY() - dragStart.y;
    if (!dragging && Math.hypot(dx, dy) < CREATE_THRESHOLD) return; // FIX: creation threshold
    dragging = true; // FIX:
    int pointerY = e.getY(); int rowTop = 0;
    for (Resource res : resources){
      int rh = rowHeights.get(res.getId());
      if (pointerY>=rowTop && pointerY<rowTop+rh){ dragOverResource = res.getId(); break; }
      rowTop += rh;
    }
    int dayIdx = Math.max(0, Math.min(days-1, dragStart.x / dayWidth));
    int x = dayIdx * dayWidth + 6;
    int w = dayWidth - 12;
    int minutePx = (int)Math.round(hourHeight/60.0); int snap = snapMinutes*minutePx;
    int y1 = Math.min(dragStart.y, pointerY);
    int y2 = Math.max(dragStart.y, pointerY);
    y1 = rowTop + ((y1 - rowTop)/snap)*snap;
    y2 = rowTop + ((y2 - rowTop + snap -1)/snap)*snap;
    dragRect = new Rectangle(x, y1, w, Math.max(12, y2 - y1));
    repaint();
  }

  private void onRelease(MouseEvent e){
    if (dragItem!=null){
      if (!dragging){ dragItem=null; dragRect=null; resizingTop=resizingBottom=false; dragging=false; creating=false; repaint(); return; } // FIX: ignore click
      int startDay = Math.max(0, Math.min(days-1, dragRect.x / dayWidth));
      int deltaDay = startDay - (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, dragStartStart.toLocalDate());
      int dy = dragRect.y - rectOf(dragItem, rowTopOf(dragOverResource)).y;
      int minutesDelta = (int) Math.round(dy * (60.0/hourHeight));

      var newStart = dragStartStart.plusDays(deltaDay).plusMinutes(minutesDelta);
      var newEnd = dragStartEnd.plusDays(deltaDay);
      if (resizingTop){ newEnd = dragStartEnd; }
      if (resizingBottom){ newStart = dragStartStart; newEnd = newStart.plusMinutes(Math.max(30, (int)Math.round(dragRect.height * (60.0/hourHeight)))); }
      if (!newEnd.isAfter(newStart)) newEnd = newStart.plusMinutes(30);

      if (dragOverResource!=null) applyPrimaryResource(dragItem, dragOverResource); // FIX: apply resource
      dragItem.setDateHeureDebut(newStart); // FIX: persist new start
      dragItem.setDateHeureFin(newEnd); // FIX: persist new end
      dragItem.setDateDebut(newStart.toLocalDate());
      dragItem.setDateFin(newEnd.toLocalDate());
      if (dragItem.getId()!=null && dragItem.getLabel()!=null) labelCache.put(dragItem.getId(), dragItem.getLabel()); // FIX: refresh cache
      PlanningValidation v = ServiceFactory.planning().validate(dragItem);
      if (!v.ok && !v.suggestions.isEmpty()){
        Object[] opts = v.suggestions.stream().map(s -> s.label!=null? s.label : "Suggestion").toArray();
        int pick = JOptionPane.showOptionDialog(this, "Conflit détecté. Appliquer une suggestion ?", "Conflit",
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, opts, opts[0]);
        if (pick>=0){
          var s = v.suggestions.get(pick);
          if (s.resourceId!=null) applyPrimaryResource(dragItem, s.resourceId);
          if (s.startDateTime!=null && s.endDateTime!=null){
            dragItem.setDateHeureDebut(s.startDateTime);
            dragItem.setDateHeureFin(s.endDateTime);
            dragItem.setDateDebut(s.startDateTime.toLocalDate());
            dragItem.setDateFin(s.endDateTime.toLocalDate());
          }
        } else { dragItem=null; dragRect=null; resizingTop=resizingBottom=false; dragging=false; creating=false; reload(); return; }
      }
      ServiceFactory.planning().saveIntervention(dragItem); // FIX: save directly
      dragItem=null; dragRect=null; resizingTop=resizingBottom=false; dragging=false; creating=false; // FIX: reset
      reload();
      return;
    }
    if (creating && dragging){ // FIX: create by drag
      int dayIdx = Math.max(0, Math.min(days-1, dragStart.x / dayWidth));
      LocalDate day = startDate.plusDays(dayIdx);
      int rowTop = rowTopOf(dragOverResource);
      int startMin = (dragRect.y - rowTop) * 60 / hourHeight;
      int durMin = Math.max(30, (int)Math.round(dragRect.height * (60.0/hourHeight)));
      LocalDateTime newStart = day.atStartOfDay().plusMinutes(startMin);
      LocalDateTime newEnd = newStart.plusMinutes(durMin);
      String label = JOptionPane.showInputDialog(this, "Libellé"); // FIX: prompt label
      if (label != null){
        label = label.strip();
        if (!label.isEmpty()){
          var it = new Intervention();
          applyPrimaryResource(it, dragOverResource);
          it.setDateHeureDebut(newStart);
          it.setDateHeureFin(newEnd);
          it.setLabel(label);
          ServiceFactory.planning().saveIntervention(it);
          labelCache.put(it.getId(), label); // FIX: cache new label

          reload();
        }
      }
    }
    dragItem=null; dragRect=null; resizingTop=resizingBottom=false; dragging=false; creating=false; // FIX: reset state
    repaint(); // FIX: clear ghost
  }

  private void onClick(MouseEvent e){
    if (SwingUtilities.isRightMouseButton(e)){
      Intervention it = findAt(e.getPoint());
      if (it!=null) showTileMenu(e.getX(), e.getY(), it);
    // === CRM-INJECT BEGIN: agenda-board-double-click-edit ===
    } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount()==2){
      Intervention it = findAt(e.getPoint());
      if (it!=null) editIntervention(it);
    }
    // === CRM-INJECT END ===
  }

  private Intervention findAt(Point p){
    int y=0;
    for (Resource r : resources){
      int rowH = rowHeights.get(r.getId());
      if (p.y>=y && p.y<y+rowH){
        for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
          Rectangle rect = rectOf(it, y);
          if (rect.contains(p)) return it;
        }
        break;
      }
      y += rowH;
    }
    return null;
  }

  private void showTileMenu(int x, int y, Intervention it){
    contextItem = it;
    var menu = new JPopupMenu();
    // === CRM-INJECT BEGIN: agenda-board-edit-action ===
    JMenuItem edit = new JMenuItem("Modifier…");
    edit.addActionListener(a -> {
      if (contextItem!=null) editIntervention(contextItem);
    });
    menu.add(edit);
    menu.addSeparator();
    // === CRM-INJECT END ===
    JMenu open = new JMenu("Ouvrir…");
    JMenuItem openQ = new JMenuItem("Devis");
    JMenuItem openO = new JMenuItem("Commande");
    JMenuItem openD = new JMenuItem("Bon de livraison");
    JMenuItem openI = new JMenuItem("Facture");
    openQ.addActionListener(a -> navigate("quotes"));
    openO.addActionListener(a -> navigate("orders"));
    openD.addActionListener(a -> navigate("delivery"));
    openI.addActionListener(a -> navigate("invoices"));
    open.add(openQ); open.add(openO); open.add(openD); open.add(openI);
    JMenuItem dup  = new JMenuItem("Dupliquer");
    JMenuItem dupW = new JMenuItem("Dupliquer +1 semaine");
    JMenuItem lock = new JMenuItem("Verrouiller / Déverrouiller");
    dup.addActionListener(a -> duplicateContext(1));
    dupW.addActionListener(a -> duplicateContext(7));
    lock.addActionListener(a -> toggleLockContext());
    menu.add(open); menu.addSeparator(); menu.add(dup); menu.add(dupW); menu.addSeparator(); menu.add(lock);
    menu.show(this, x, y);
    contextItem = null;
  }

  private void duplicateContext(int days){
    if (contextItem==null) return;
    Intervention copy = new Intervention();
    copy.setResources(contextItem.getResources());
    applyPrimaryResource(copy, contextItem.getResourceId());
    copy.setLabel(contextItem.getLabel()+" (copie)");
    copy.setColor(contextItem.getColor());
    copy.setType(contextItem.getType());
    copy.setAddress(contextItem.getAddress());
    copy.setDescription(contextItem.getDescription());
    copy.setInternalNote(contextItem.getInternalNote());
    copy.setClosingNote(contextItem.getClosingNote());
    copy.setContacts(contextItem.getContacts());
    copy.setQuoteDraft(contextItem.getQuoteDraft());
    if (contextItem.getDateHeureDebut()!=null) copy.setDateHeureDebut(contextItem.getDateHeureDebut().plusDays(days));
    if (contextItem.getDateHeureFin()!=null) copy.setDateHeureFin(contextItem.getDateHeureFin().plusDays(days));
    if (contextItem.getDateDebut()!=null) copy.setDateDebut(contextItem.getDateDebut().plusDays(days));
    if (contextItem.getDateFin()!=null) copy.setDateFin(contextItem.getDateFin().plusDays(days));
    // === CRM-INJECT BEGIN: agenda-board-duplicate-client ===
    copy.setClientId(contextItem.getClientId());
    copy.setClientName(contextItem.getClientName());
    // === CRM-INJECT END ===
    copy.setStatus(contextItem.getStatus());
    copy.setFavorite(contextItem.isFavorite());
    copy.setLocked(contextItem.isLocked());
    ServiceFactory.planning().saveIntervention(copy);
    reload();
  }

  // === CRM-INJECT BEGIN: agenda-board-edit-dialog ===
  private void editIntervention(Intervention it){
    var planning = ServiceFactory.planning();
    if (planning == null || it == null){
      return;
    }
    InterventionDialog dialog = new InterventionDialog(
        SwingUtilities.getWindowAncestor(this),
        planning,
        ServiceFactory.clients(),
        ServiceFactory.interventionTypes());
    dialog.edit(it);
    dialog.setVisible(true);
    if (dialog.isSaved()){
      Intervention updated = dialog.getIntervention();
      planning.saveIntervention(updated);
      reload();
    }
  }
  // === CRM-INJECT END ===

  private void applyPrimaryResource(Intervention it, UUID resourceId){
    if (it==null) return;
    if (resourceId==null){
      it.setResourceId(null);
      it.setResources(List.of());
      return;
    }
    List<ResourceRef> refs = it.getResources();
    ResourceRef details = lookupResource(resourceId);
    if (refs.isEmpty()){
      it.setResources(details!=null? List.of(details) : List.of(new ResourceRef(resourceId, null, null)));
    } else {
      ResourceRef first = refs.get(0);
      String name = first.getName();
      String icon = first.getIcon();
      if (details!=null){
        name = details.getName();
        icon = details.getIcon();
      }
      refs.set(0, new ResourceRef(resourceId, name, icon));
      it.setResources(refs);
    }
    it.setResourceId(resourceId);
  }

  private ResourceRef lookupResource(UUID id){
    if (id==null) return null;
    for (Resource r : resources){
      if (id.equals(r.getId())){
        return new ResourceRef(r.getId(), r.getName(), typeIcon(r));
      }
    }
    return null;
  }

  private void toggleLockContext(){
    if (contextItem==null) return;
    contextItem.setLocked(!contextItem.isLocked());
    ServiceFactory.planning().saveIntervention(contextItem);
    reload();
  }

  private void navigate(String key){
    var w = SwingUtilities.getWindowAncestor(this);
    if (w instanceof MainFrame mf) mf.openCard(key);
  }

  private int rowTopOf(UUID resourceId){
    int y=0;
    for (Resource r : resources){
      if (r.getId().equals(resourceId)) return y;
      y += rowHeights.get(r.getId());
    }
    return 0;
  }

  private static String typeIcon(Resource resource){
    if (resource==null || resource.getType()==null) return null;
    return resource.getType().getIcon();
  }
}
