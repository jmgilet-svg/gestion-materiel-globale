package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningValidation;
import com.materiel.suite.client.ui.interventions.InterventionDialog;
import com.materiel.suite.client.ui.planning.render.TileRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import com.materiel.suite.client.ui.MainFrame;

public class PlanningBoard extends JComponent implements Scrollable {
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
  private int totalHeight = 0;
  private int lastLayoutHash = 0;
  private Map<UUID, Integer> rowTileHeights = new HashMap<>();
  private Map<UUID, Integer> rowTops = new HashMap<>();
  private java.util.List<UUID> resourceOrder = new java.util.ArrayList<>();
  private volatile int visibleStart = -1;
  private volatile int visibleEnd = -1;

  // UX
  private boolean showIndispo = true;
  private String resourceFilter = "";
  private final Map<UUID,String> labelCache = new HashMap<>();
  private Intervention hovered;
  private Intervention selected;
  private final InterventionTileRenderer tile = new InterventionTileRenderer();
  private TileRenderer tileRenderer;
  private int rowGap = PlanningUx.ROW_GAP;
  private boolean compact = false; // compat

  private UiDensity density = UiDensity.NORMAL;

  // DnD state
  private Intervention dragItem;
  private Rectangle dragRect;
  private boolean resizingLeft, resizingRight;
  private static final int DM_NONE=0, DM_MOVE=1, DM_RESIZE_L=2, DM_RESIZE_R=3;
  private int dragMode = DM_NONE;
  private int initialStartSlot, initialEndSlot, dragDurationSlots, pressOffsetX;
  private Point dragStart;
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

    JMenuItem miEdit = new JMenuItem("Renommer…");
    // === CRM-INJECT BEGIN: planning-board-edit-action ===
    miEdit.addActionListener(e -> {
      if (selected==null) return;
      openInterventionEditDialog(selected);
    });
    // === CRM-INJECT END ===
    JMenuItem miDelete = new JMenuItem("Supprimer");
    miDelete.addActionListener(e -> {
      if (selected==null) return;
      int ok = JOptionPane.showConfirmDialog(this, "Supprimer l'intervention « "+safeLabel(selected)+" » ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
      if (ok==JOptionPane.OK_OPTION){
        ServiceFactory.planning().deleteIntervention(selected.getId());
        reload();
      }
    });
    JMenuItem dup  = new JMenuItem("Dupliquer");
    JMenuItem dupW = new JMenuItem("Dupliquer +1 semaine");
    JMenuItem lock = new JMenuItem("Verrouiller / Déverrouiller");
    dup.addActionListener(a -> duplicateSelected(1));
    dupW.addActionListener(a -> duplicateSelected(7));
    lock.addActionListener(a -> toggleLockSelected());
    menu.add(open);
    menu.addSeparator();
    menu.add(miEdit);
    menu.add(miDelete);
    menu.addSeparator();
    menu.add(dup);
    menu.add(dupW);
    menu.addSeparator();
    menu.add(lock);
    return menu;
  }

  private void duplicateSelected(int days){
    if (selected==null) return;
    Intervention copy = new Intervention();
    copy.setResources(selected.getResources());
    copy.setResourceId(selected.getResourceId());
    copy.setLabel(selected.getLabel() + " (copie)");
    copy.setColor(selected.getColor());
    copy.setType(selected.getType());
    copy.setAddress(selected.getAddress());
    copy.setDescription(selected.getDescription());
    copy.setInternalNote(selected.getInternalNote());
    copy.setClosingNote(selected.getClosingNote());
    copy.setContacts(selected.getContacts());
    copy.setBillingLines(selected.getBillingLines());
    copy.setQuoteDraft(selected.getQuoteDraft());
    if (selected.getDateHeureDebut()!=null) copy.setDateHeureDebut(selected.getDateHeureDebut().plusDays(days));
    if (selected.getDateHeureFin()!=null) copy.setDateHeureFin(selected.getDateHeureFin().plusDays(days));
    if (selected.getDateDebut()!=null) copy.setDateDebut(selected.getDateDebut().plusDays(days));
    if (selected.getDateFin()!=null) copy.setDateFin(selected.getDateFin().plusDays(days));
    // === CRM-INJECT BEGIN: planning-board-duplicate-client ===
    copy.setClientId(selected.getClientId());
    copy.setClientName(selected.getClientName());
    // === CRM-INJECT END ===
    copy.setStatus(selected.getStatus());
    copy.setFavorite(selected.isFavorite());
    copy.setLocked(selected.isLocked());
    ServiceFactory.planning().saveIntervention(copy);
    reload();
  }

  // === CRM-INJECT BEGIN: planning-board-edit-dialog ===
  private void openInterventionEditDialog(Intervention it){
    var planning = ServiceFactory.planning();
    if (planning == null || it == null){
      return;
    }
    InterventionDialog dialog = new InterventionDialog(
        SwingUtilities.getWindowAncestor(this),
        planning,
        ServiceFactory.clients(),
        ServiceFactory.interventionTypes(),
        ServiceFactory.templates());
    dialog.setOnSave(updated -> {
      planning.saveIntervention(updated);
      selected = updated;
      reload();
    });
    dialog.edit(it);
    dialog.setVisible(true);
  }
  // === CRM-INJECT END ===

  private void toggleLockSelected(){
    if (selected==null) return;
    selected.setLocked(!selected.isLocked());
    ServiceFactory.planning().saveIntervention(selected);
    reload();
  }

  // API publique
  /** Expose les ressources visibles pour synchroniser le RowHeader. */
  public java.util.List<Resource> getResourcesList(){ return resources; }
  /** Hauteur d'une ligne (ressource). */
  public int rowHeight(UUID resId){ return rowHeights.getOrDefault(resId, tile.heightBase() + rowGap); }
  /** Durée d'un slot en minutes. */
  public int getSlotMinutes(){ return slotMinutes; }
  public boolean isCompact(){ return compact; }
  public void setCompact(boolean c){ this.compact = c; this.tile.setCompact(c); reload(); }
  public UiDensity getDensity(){ return density; }
  public void setDensity(UiDensity d){
    this.density = d;
    this.compact = (d == UiDensity.COMPACT);
    this.tile.setCompact(this.compact);
    this.tile.setDensity(d);
    reload();
  }
  public void setVisibleRowWindow(int startInclusive, int endExclusive){
    int newStart = startInclusive;
    int newEnd = endExclusive;
    if (newStart < 0 || newEnd < 0 || newEnd <= newStart){
      newStart = -1;
      newEnd = -1;
    }
    if (newStart == visibleStart && newEnd == visibleEnd){
      return;
    }
    visibleStart = newStart;
    visibleEnd = newEnd;
    repaint();
  }
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
  public int tileHeight(){ return tile.heightBase(); }

  public void setTileRenderer(TileRenderer renderer){
    this.tileRenderer = renderer;
    repaint();
  }

  public TileRenderer getTileRenderer(){
    return tileRenderer;
  }

  /**
   * API publique et stable pour trouver l'intervention sous un point pixel (coordonnées locales au board).
   * <p>
   * Implémentation tolérante : si le board dispose déjà d’un mécanisme interne
   * (ex. {@code findTileAt(Point)}, {@code getInterventionAt(int,int)}, {@code hitTest(Point)}),
   * on le réutilise par réflexion pour ne rien casser. À défaut, on retombe sur {@link #hitTile(Point)}.
   */
  public Intervention findInterventionAt(Point p){
    if (p == null){
      return null;
    }

    Intervention viaTile = tryTileBasedHit(p, "findTileAt");
    if (viaTile != null){
      return viaTile;
    }
    viaTile = tryTileBasedHit(p, "tileAt");
    if (viaTile != null){
      return viaTile;
    }

    Method direct = findCompatibleMethod(getClass(), "getInterventionAt", int.class, int.class);
    if (direct != null && direct.getDeclaringClass() != PlanningBoard.class){
      try {
        Object res = direct.invoke(this, p.x, p.y);
        if (res instanceof Intervention it){
          return it;
        }
      } catch (Exception ignore){
      }
    }

    Method hitTest = findCompatibleMethod(getClass(), "hitTest", Point.class);
    if (hitTest != null){
      try {
        Object hit = hitTest.invoke(this, p);
        if (hit instanceof Intervention it){
          return it;
        }
        if (hit != null){
          Method gi = findCompatibleMethod(hit.getClass(), "getIntervention");
          if (gi != null){
            Object res = gi.invoke(hit);
            if (res instanceof Intervention it){
              return it;
            }
          }
        }
      } catch (Exception ignore){
      }
    }

    return hitTile(p);
  }

  /** Surcharge utilitaire : signature simple au format (x,y). */
  public Intervention getInterventionAt(int x, int y){
    return findInterventionAt(new Point(x, y));
  }

  /** Essaie des helpers « tile » renvoyant un objet qui expose getIntervention(). */
  private Intervention tryTileBasedHit(Point p, String methodName){
    Method m = findCompatibleMethod(getClass(), methodName, Point.class);
    if (m == null || m.getDeclaringClass() == PlanningBoard.class){
      return null;
    }
    try {
      Object tile = m.invoke(this, p);
      if (tile instanceof Intervention it){
        return it;
      }
      if (tile != null){
        Method gi = findCompatibleMethod(tile.getClass(), "getIntervention");
        if (gi != null){
          Object res = gi.invoke(tile);
          if (res instanceof Intervention ii){
            return ii;
          }
        }
      }
    } catch (Exception ignore){
    }
    return null;
  }

  /* ==================== Scrollable ==================== */
  @Override public Dimension getPreferredScrollableViewportSize(){
    return getPreferredSize();
  }

  @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
    if (orientation == SwingConstants.VERTICAL){
      int laneHeight = tile.heightBase() + rowGap;
      return Math.max(24, Math.min(laneHeight, 96));
    }
    return Math.max(24, slotWidth * 4);
  }

  @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
    if (orientation == SwingConstants.VERTICAL){
      int base = tile.heightBase() + rowGap;
      int viewport = visibleRect == null ? 0 : visibleRect.height;
      return Math.max(Math.max(base, viewport - 32), 120);
    }
    int viewport = visibleRect == null ? 0 : visibleRect.width;
    return Math.max(getDayPixelWidth(), Math.max(viewport - 64, 160));
  }

  /** Le board occupe toute la largeur disponible pour éviter le scroll horizontal. */
  @Override public boolean getScrollableTracksViewportWidth(){
    return true;
  }

  /** Autorise le scroll vertical lorsque le contenu dépasse la hauteur visible. */
  @Override public boolean getScrollableTracksViewportHeight(){
    return false;
  }

  private Method findCompatibleMethod(Class<?> type, String name, Class<?>... parameterTypes){
    if (type == null || name == null){
      return null;
    }
    try {
      return type.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException ignore){
    } catch (Exception ignore){
    }
    Class<?> current = type;
    while (current != null){
      try {
        Method declared = current.getDeclaredMethod(name, parameterTypes);
        declared.setAccessible(true);
        return declared;
      } catch (NoSuchMethodException ignore){
      } catch (Exception ignore){
      }
      current = current.getSuperclass();
    }
    return null;
  }

  private Method findCompatibleMethod(Class<?> type, String name){
    return findCompatibleMethod(type, name, new Class<?>[0]);
  }

  public void reload(){
    visibleStart = -1;
    visibleEnd = -1;
    resources = ServiceFactory.planning().listResources().stream()
        .filter(r -> resourceFilter.isBlank() || r.getName().toLowerCase().contains(resourceFilter))
        .collect(Collectors.toList());
    interventions = filterByAgency(ServiceFactory.planning().listInterventions(startDate, startDate.plusDays(days-1)));
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

  private List<Intervention> filterByAgency(List<Intervention> list){
    if (list == null || list.isEmpty()){
      return List.of();
    }
    List<Intervention> filtered = new ArrayList<>();
    for (Intervention intervention : list){
      if (intervention == null){
        continue;
      }
      if (AgencyContext.matchesCurrentAgency(intervention)){
        filtered.add(intervention);
      }
    }
    return filtered.isEmpty() ? List.of() : List.copyOf(filtered);
  }

  private void computeLanesAndHeights(){
    lanes.clear(); rowHeights.clear(); rowTileHeights.clear(); rowTops.clear(); resourceOrder.clear(); totalHeight = 0;
    for (Resource r : resources){
      resourceOrder.add(r.getId());
      List<Intervention> list = byResource.getOrDefault(r.getId(), List.of());
      Map<Intervention, LaneLayout.Lane> m = LaneLayout.computeLanes(list,
          Intervention::getStartDateTime, Intervention::getEndDateTime);
      lanes.putAll(m);
      int lanesCount = m.values().stream().mapToInt(l -> l.index).max().orElse(-1) + 1;

      int rowTileH = tile.heightBase();
      for (Intervention it : list){
        int width = tilePixelWidthFor(it);
        rowTileH = Math.max(rowTileH, tile.heightFor(it, width));
      }
      rowTileHeights.put(r.getId(), rowTileH);

      rowTops.put(r.getId(), totalHeight);
      int rowH = Math.max(rowTileH, lanesCount * (rowTileH + PlanningUx.LANE_GAP)) + rowGap;
      rowHeights.put(r.getId(), rowH);
      totalHeight += rowH;
    }
    lastLayoutHash = Objects.hash(resources.size(), rowHeights.hashCode(), days, slotWidth, slotMinutes);
  }

  @Override public Dimension getPreferredSize(){
    Dimension base = super.getPreferredSize();
    int width = getDayPixelWidth() * days;
    if (base != null){
      width = Math.max(width, base.width);
    }
    int rows = resources == null ? 0 : resources.size();
    int estimated = rows * (tile.heightBase() + rowGap);
    int height = Math.max(totalHeight, estimated);
    height = Math.max(height + 64, 400);
    if (base != null){
      height = Math.max(height, base.height);
    }
    return new Dimension(width, height);
  }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(PlanningUx.BG);
    g2.fillRect(0,0,getWidth(),getHeight());
    Rectangle clip = g2.getClipBounds();
    if (clip == null){
      clip = new Rectangle(0, 0, getWidth(), getHeight());
    }

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

    // Rows + tiles (virtualization)
    int total = resources == null ? 0 : resources.size();
    if (total > 0){
      int startIndex = 0;
      int endIndex = total;
      int windowStart = visibleStart;
      int windowEnd = visibleEnd;
      if (windowStart >= 0 && windowEnd >= 0){
        startIndex = Math.max(0, Math.min(windowStart, total - 1));
        endIndex = Math.max(startIndex + 1, Math.min(windowEnd, total));
      }
      int y = 0;
      for (int i = 0; i < startIndex; i++){
        Resource r = resources.get(i);
        y += rowHeights.getOrDefault(r.getId(), tile.heightBase() + rowGap);
      }
      for (int i = startIndex; i < endIndex; i++){
        Resource r = resources.get(i);
        int rowH = rowHeights.getOrDefault(r.getId(), tile.heightBase() + rowGap);
        if (y + rowH < clip.y){
          y += rowH;
          continue;
        }
        if (y > clip.y + clip.height){
          break;
        }
        if (showIndispo){
          Rectangle hatch = new Rectangle(2 * dayW, y, dayW * 2, rowH - 1);
          PlanningUx.paintHatch(g2, hatch);
        }
        g2.setColor(PlanningUx.ROW_DIV);
        g2.drawLine(0, y + rowH - 1, getWidth(), y + rowH - 1);
        for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
          Rectangle rect = rectOf(it, y);
          if (!rect.intersects(new Rectangle(clip.x - 200, y, clip.width + 400, rowH))){
            continue;
          }
          if (tileRenderer != null){
            TileRenderer.State inferred = tileRenderer.inferState(it, it == selected);
            TileRenderer.State state = inferred == null
                ? new TileRenderer.State(
                    it == selected,
                    it == hovered,
                    it.hasQuote(),
                    it.getStatus(),
                    it.getAgencyName(),
                    null)
                : new TileRenderer.State(
                    inferred.selected(),
                    it == hovered,
                    inferred.hasQuote(),
                    inferred.status(),
                    inferred.agency(),
                    inferred.smallIconKey());
            Graphics2D tileG = (Graphics2D) g2.create();
            try {
              tileRenderer.paintTile(tileG, it, rect, state);
            } finally {
              tileG.dispose();
            }
          } else {
            tile.paint(g2, rect, it, it == hovered, it == selected);
          }
        }
        y += rowH;
      }
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

  private UUID resourceAtY(int yPx){
    int acc = 0;
    for (UUID id : resourceOrder){
      int rowH = rowHeights.getOrDefault(id, tile.heightBase()+rowGap);
      if (yPx >= acc && yPx < acc + rowH) return id;
      acc += rowH;
    }
    return resourceOrder.isEmpty()? null : resourceOrder.get(resourceOrder.size()-1);
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
    int rowTileH = rowTileHeights.getOrDefault(it.getResourceId(), tile.heightBase());
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
      int rowH = rowHeights.getOrDefault(r.getId(), tile.heightBase()+rowGap);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        if (rect.contains(p)) return it;
      }
      y+=rowH;
    }
    return null;
  }

  private void navigate(String key){
    var w = SwingUtilities.getWindowAncestor(this);
    if (w instanceof MainFrame mf) mf.openCard(key);
  }

  // DnD handlers
  private void onPress(MouseEvent e){
    dragItem=null; dragRect=null; resizingLeft=false; resizingRight=false; dragMode=DM_NONE; dragOverResource=null;

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
            resizingLeft = Math.abs(p.x - rect.x) < PlanningUx.HANDLE;
            resizingRight = Math.abs(p.x - (rect.x+rect.width)) < PlanningUx.HANDLE;
            pressOffsetX = p.x - rect.x;
            dragOverResource = r.getId();

            int sSlot = rect.x / slotWidth;
            int eSlot = (rect.x + rect.width) / slotWidth - 1;
            initialStartSlot = sSlot;
            initialEndSlot = eSlot;
            dragDurationSlots = Math.max(1, eSlot - sSlot + 1);

            dragMode = resizingLeft? DM_RESIZE_L : (resizingRight? DM_RESIZE_R : DM_MOVE);
            selected = it;
            return;
          }
        }
      }
      y += rowH;
    }
  }

  private void onDrag(MouseEvent e){
    if (dragItem==null) return;
    Rectangle r = new Rectangle(dragRect);

    if (dragMode == DM_MOVE){
      int dx = e.getX() - dragStart.x;
      int deltaSlots = Math.round(dx / (float)slotWidth);
      int newStart = initialStartSlot + deltaSlots;
      newStart = Math.max(0, Math.min(days*slotsPerDay - dragDurationSlots, newStart));
      int newEnd = newStart + dragDurationSlots - 1;
      r.x = xFromSlot(newStart);
      r.width = Math.max(slotWidth, (newEnd - newStart + 1) * slotWidth);

      UUID over = resourceAtY(e.getY());
      if (over!=null){
        dragOverResource = over;
        int baseY = rowTops.getOrDefault(over, 0);
        int rowTileH = rowTileHeights.getOrDefault(over, tile.heightBase());
        r.y = baseY;
        r.height = rowTileH;
      }
    } else if (dragMode == DM_RESIZE_L){
      int rawStart = (e.getX() - pressOffsetX) / slotWidth;
      int newStart = Math.min(initialEndSlot, Math.max(0, Math.round(rawStart)));
      if (initialEndSlot - newStart + 1 < 1) newStart = initialEndSlot;
      r.x = xFromSlot(newStart);
      r.width = Math.max(slotWidth, (initialEndSlot - newStart + 1) * slotWidth);
      dragDurationSlots = initialEndSlot - newStart + 1;
    } else if (dragMode == DM_RESIZE_R){
      int rawEnd = (e.getX()) / slotWidth - 1;
      int newEnd = Math.max(initialStartSlot, Math.min(days*slotsPerDay-1, Math.round(rawEnd)));
      if (newEnd - initialStartSlot + 1 < 1) newEnd = initialStartSlot;
      r.x = xFromSlot(initialStartSlot);
      r.width = Math.max(slotWidth, (newEnd - initialStartSlot + 1) * slotWidth);
      dragDurationSlots = newEnd - initialStartSlot + 1;
    }

    dragRect = r;
    repaint();
  }

  private void onRelease(MouseEvent e){
    if (dragItem==null){ return; }
    if (dragRect==null){ dragItem=null; return; }
    int startSlot = dragRect.x / slotWidth;
    int endSlot = (dragRect.x + dragRect.width) / slotWidth - 1;
    int dayIdxStart = startSlot / slotsPerDay;
    int minutesStart = (startSlot % slotsPerDay) * slotMinutes;
    int dayIdxEnd = endSlot / slotsPerDay;
    int minutesEnd = ((endSlot % slotsPerDay) + 1) * slotMinutes; // end is exclusive

    LocalDateTime sdt = startDate.plusDays(dayIdxStart).atStartOfDay().plusMinutes(minutesStart);
    LocalDateTime edt = startDate.plusDays(dayIdxEnd).atStartOfDay().plusMinutes(minutesEnd);
    if (dragItem.getId()!=null && dragItem.getLabel()!=null && !dragItem.getLabel().isBlank()){
      labelCache.put(dragItem.getId(), dragItem.getLabel());
    }
    if (dragOverResource!=null) applyPrimaryResource(dragItem, dragOverResource);
    dragItem.setDateHeureDebut(sdt);
    dragItem.setDateHeureFin(edt);
    dragItem.setDateDebut(sdt.toLocalDate());
    dragItem.setDateFin(edt.minusMinutes(1).toLocalDate());
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
          dragItem.setDateFin(s.endDateTime.minusMinutes(1).toLocalDate());
        }
      } else {
        dragItem = null; dragRect=null; resizingLeft=resizingRight=false; dragMode=DM_NONE; reload(); return;
      }
    }
    ServiceFactory.planning().saveIntervention(dragItem);
    dragItem = null; dragRect=null; resizingLeft=resizingRight=false; dragMode=DM_NONE;
    reload();
  }

  private void onMove(MouseEvent e){
    int y=0;
    hovered = null;
    boolean anyHover = false;
    for (Resource r : resources){
      int rowH = rowHeights.getOrDefault(r.getId(), tile.heightBase()+rowGap);
      for (Intervention it : byResource.getOrDefault(r.getId(), List.of())){
        Rectangle rect = rectOf(it, y);
        if (rect.contains(e.getPoint())){
          hovered = it;
          setToolTipText("<html><b>"+safe(it.getClientName())+"</b><br/>Chantier : "
              +safe(it.getSiteLabel())+"<br/>"+safe(it.prettyTimeRange())+"</html>");
          anyHover = true;
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
    if (!anyHover) setToolTipText(null);
    setCursor(Cursor.getDefaultCursor());
    repaint();
  }

  private static String safe(String s){ return (s==null? "—" : s); }

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

  private void onClick(MouseEvent e){
    if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==1){
      selected = hitTile(e.getPoint());
      repaint();
    }
  }
  private static String typeIcon(Resource resource){
    if (resource==null || resource.getType()==null) return null;
    return resource.getType().getIcon();
  }
}
