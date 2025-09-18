package com.materiel.suite.client.ui.planning;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SpinnerDateModel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.commands.CommandBus;
import com.materiel.suite.client.ui.MainFrame;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.ui.interventions.InterventionDialog;
import com.materiel.suite.client.ui.interventions.PreDevisUtil;
import com.materiel.suite.client.ui.interventions.QuoteGenerator;

public class PlanningPanel extends JPanel {
  private static final DateTimeFormatter SIMPLE_DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter SIMPLE_DAY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private final PlanningBoard board = new PlanningBoard();
  private final AgendaBoard agenda = new AgendaBoard();
  private final JButton bulkQuoteBtn = new JButton("Générer devis (sélection)", IconRegistry.small("file"));
  private JButton conflictsBtn;
  private JPanel ganttContainer;
  private JTabbedPane tabs;
  private final InterventionView calendarView = new InterventionCalendarView();
  private final InterventionView tableView = new InterventionTableView();
  private JToggleButton modeToggle;
  private boolean agendaMode;
  private boolean updatingModeToggle;
  private JComboBox<String> simplePeriod;
  private JSpinner simpleRefDate;
  private boolean updatingSimpleRange;

  public PlanningPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);

    var scroll = new JScrollPane(board, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    DayHeader header = new DayHeader(board);
    scroll.setColumnHeaderView(header);
    scroll.getHorizontalScrollBar().addAdjustmentListener(e -> header.repaint());

    var scrollAgenda = new JScrollPane(agenda);

    JPanel center = new JPanel(new CardLayout());
    center.add(scroll, "gantt");
    center.add(scrollAgenda, "agenda");

    JComponent rowHeader = new JComponent(){
      @Override public Dimension getPreferredSize(){ return new Dimension(240, board.getPreferredSize().height); }
      @Override protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(0xF7F7F7));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(new Color(0xDDDDDD));
        g2.drawLine(getWidth()-1,0,getWidth()-1,getHeight());
        int y=0;
        java.util.List<Resource> rs = board.getResourcesList();
        for (Resource r : rs){
          int rowH = board.rowHeight(r.getId());
          g2.setColor(new Color(0xF7F7F7));
          g2.fillRect(0,y,getWidth(),rowH);
          g2.setColor(Color.DARK_GRAY);
          // libellé (wrap simple si manque de place)
          String name = r.getName()==null? "—" : r.getName();
          FontMetrics fm = g2.getFontMetrics();
          int textY = y + Math.max(fm.getAscent()+6, rowH/2 + fm.getAscent()/2);
          g2.drawString(name, 12, textY);
          g2.setColor(new Color(0xE0E0E0));
          g2.drawLine(0, y+rowH-1, getWidth(), y+rowH-1);
          y+=rowH;
        }
      }
    };
    scroll.setRowHeaderView(rowHeader);

    // Repeindre le header quand le layout du board change
    board.addPropertyChangeListener("layout", e -> {
      rowHeader.revalidate();
      rowHeader.repaint();
    });

    calendarView.setOnOpen(this::openInterventionEditor);
    calendarView.setOnMove(this::moveIntervention);
    calendarView.setOnMoveDateTime(this::moveInterventionDateTime);
    calendarView.setOnResizeDateTime(this::resizeInterventionEndDateTime);
    tableView.setOnOpen(this::openInterventionEditor);

    ganttContainer = center;
    tabs = new JTabbedPane();
    tabs.addTab("Planning", IconRegistry.small("task"), center);
    tabs.addTab("Calendrier", IconRegistry.small("calendar"), calendarView.getComponent());
    tabs.addTab("Liste", IconRegistry.small("file"), tableView.getComponent());
    tabs.addChangeListener(e -> updateModeToggleState());
    add(tabs, BorderLayout.CENTER);
    updateModeToggleState();

    reload();

    putUndoRedoKeymap();
  }

  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    JButton prev = new JButton("◀ Semaine");
    JButton next = new JButton("Semaine ▶");
    JButton today = new JButton("Aujourd'hui");
    JLabel zoomL = new JLabel("Zoom (slot):");
    JSlider zoom = new JSlider(6,24,board.getSlotWidth());
    JLabel granL = new JLabel("Pas:");
    JComboBox<String> gran = new JComboBox<>(new String[]{"5 min","10 min","15 min","30 min","60 min"});
    gran.setSelectedItem(board.getSlotMinutes()+" min");
    JLabel densL = new JLabel("Densité:");
    JComboBox<String> density = new JComboBox<>(new String[]{"COMPACT","NORMAL","SPACIOUS"});
    modeToggle = new JToggleButton("Agenda");
    conflictsBtn = new JButton("Conflits (0)");
    JButton toAgenda = new JButton("↔ Agenda");
    JButton addI = new JButton("+ Intervention", IconRegistry.small("task"));
    simplePeriod = new JComboBox<>(new String[]{"Semaine","Mois"});
    LocalDate initialRef = board.getStartDate();
    if (initialRef == null){
      initialRef = LocalDate.now().with(DayOfWeek.MONDAY);
    }
    simpleRefDate = new JSpinner(new SpinnerDateModel(toDate(initialRef), null, null, Calendar.DAY_OF_MONTH));
    simpleRefDate.setEditor(new JSpinner.DateEditor(simpleRefDate, "dd/MM/yyyy"));
    JButton simplePrev = new JButton("◀");
    JButton simpleToday = new JButton("Aujourd'hui");
    JButton simpleNext = new JButton("▶");

    modeToggle.addActionListener(e -> {
      if (updatingModeToggle) return;
      switchMode(modeToggle.isSelected());
    });
    conflictsBtn.addActionListener(e -> openConflictsDialog());
    density.setSelectedItem(board.getDensity().name());
    density.addActionListener(e -> {
      board.setDensity(UiDensity.fromString(String.valueOf(density.getSelectedItem())));
      revalidate(); repaint();
    });

    prev.addActionListener(e -> {
      LocalDate start = board.getStartDate();
      if (start == null){
        start = LocalDate.now().with(DayOfWeek.MONDAY);
      }
      LocalDate newStart = start.minusDays(7);
      board.setStartDate(newStart);
      agenda.setStartDate(board.getStartDate());
      updateSimpleReference(board.getStartDate());
    });
    next.addActionListener(e -> {
      LocalDate start = board.getStartDate();
      if (start == null){
        start = LocalDate.now().with(DayOfWeek.MONDAY);
      }
      LocalDate newStart = start.plusDays(7);
      board.setStartDate(newStart);
      agenda.setStartDate(board.getStartDate());
      updateSimpleReference(board.getStartDate());
    });
    today.addActionListener(e -> {
      LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
      board.setStartDate(monday);
      agenda.setStartDate(board.getStartDate());
      updateSimpleReference(monday);
    });
    zoom.addChangeListener(e -> {
      int w = zoom.getValue();
      board.setZoom(w);
      agenda.setDayWidth(w*10);
      revalidate(); repaint();
    });
    gran.addActionListener(e -> {
      String s = String.valueOf(gran.getSelectedItem());
      int m = Integer.parseInt(s.replace(" min",""));
      board.setSlotMinutes(m);
      agenda.setSnapMinutes(m);
      revalidate(); repaint();
    });
    addI.addActionListener(e -> addInterventionDialog());
    toAgenda.addActionListener(e -> navigate("agenda"));
    simplePeriod.addActionListener(e -> onSimpleRangeChanged());
    simpleRefDate.addChangeListener(e -> onSimpleRangeChanged());
    simplePrev.addActionListener(e -> shiftSimpleRange(-1));
    simpleNext.addActionListener(e -> shiftSimpleRange(1));
    simpleToday.addActionListener(e -> updateSimpleReference(LocalDate.now()));

    bar.add(prev); bar.add(next); bar.add(today); bar.add(modeToggle);
    bar.add(Box.createHorizontalStrut(16)); bar.add(zoomL); bar.add(zoom);
    bar.add(Box.createHorizontalStrut(12)); bar.add(granL); bar.add(gran);
    bar.add(Box.createHorizontalStrut(12)); bar.add(densL); bar.add(density);
    bar.add(Box.createHorizontalStrut(8)); bar.add(conflictsBtn);
    bar.add(Box.createHorizontalStrut(12)); bar.add(toAgenda);
    JLabel simpleLabel = new JLabel("Période calendrier:");
    bar.add(Box.createHorizontalStrut(16)); bar.add(simpleLabel); bar.add(simplePeriod); bar.add(simpleRefDate);
    bar.add(simplePrev); bar.add(simpleToday); bar.add(simpleNext);
    bar.add(Box.createHorizontalStrut(16)); bar.add(addI);
    bar.add(Box.createHorizontalStrut(8)); bar.add(bulkQuoteBtn);
    bulkQuoteBtn.addActionListener(e -> generateQuotesForSelection());
    return bar;
  }

  private void navigate(String key){
    var w = SwingUtilities.getWindowAncestor(this);
    if (w instanceof MainFrame mf) mf.openCard(key);
  }

  private void switchMode(boolean agendaMode){
    this.agendaMode = agendaMode;
    if (ganttContainer != null){
      CardLayout cl = (CardLayout) ganttContainer.getLayout();
      cl.show(ganttContainer, agendaMode ? "agenda" : "gantt");
    }
    if (tabs != null && ganttContainer != null && tabs.getSelectedComponent() != ganttContainer){
      tabs.setSelectedComponent(ganttContainer);
    }
    updateModeToggleState();
  }

  private void updateModeToggleState(){
    if (modeToggle == null){
      return;
    }
    boolean onPlanningTab = tabs != null && ganttContainer != null && tabs.getSelectedComponent() == ganttContainer;
    updatingModeToggle = true;
    try {
      modeToggle.setEnabled(onPlanningTab);
      modeToggle.setSelected(onPlanningTab && agendaMode);
    } finally {
      updatingModeToggle = false;
    }
  }

  private void openConflictsDialog(){
    var from = board.getStartDate();
    var to = from.plusDays(6);
    java.util.List<Conflict> conflicts = ServiceFactory.planning().listConflicts(from, to);
    conflictsBtn.setText("Conflits ("+conflicts.size()+")");
    if (conflicts.isEmpty()){
      JOptionPane.showMessageDialog(this, "Aucun conflit sur la période.", "Conflits", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    DefaultListModel<Conflict> model = new DefaultListModel<>();
    conflicts.forEach(model::addElement);
    JList<Conflict> list = new JList<>(model);
    list.setCellRenderer((jl,c,idx,sel,focus)->{
      JLabel l = new JLabel("Ressource "+c.getResourceId()+" — "+c.getA()+" ↔ "+c.getB());
      if(sel) l.setOpaque(true);
      return l;
    });
    list.setVisibleRowCount(12);

    JButton shift = new JButton("Décaler +30 min");
    JButton reassign = new JButton("Changer ressource…");
    JButton split = new JButton("Couper à…");
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(shift); actions.add(reassign); actions.add(split);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JScrollPane(list), BorderLayout.CENTER);
    panel.add(actions, BorderLayout.SOUTH);

    JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Conflits détectés", Dialog.ModalityType.APPLICATION_MODAL);
    dlg.setContentPane(panel);
    dlg.setSize(560,420);
    dlg.setLocationRelativeTo(this);

    PlanningService svc = ServiceFactory.planning();

    shift.addActionListener(e->{
      Conflict c=list.getSelectedValue(); if(c==null) return;
      svc.resolveShift(c.getB(),30); refreshPlanning(); conflictsBtn.setText("Conflits (?)");
    });
    reassign.addActionListener(e->{
      Conflict c=list.getSelectedValue(); if(c==null) return;
      var rs=ServiceFactory.planning().listResources();
      String[] names=rs.stream().map(Resource::getName).toArray(String[]::new);
      int idx=JOptionPane.showOptionDialog(dlg,"Choisir la ressource cible :","Reassigner",JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.QUESTION_MESSAGE,null,names,names[0]);
      if(idx>=0){ svc.resolveReassign(c.getB(), rs.get(idx).getId()); refreshPlanning(); conflictsBtn.setText("Conflits (?)"); }
    });
    split.addActionListener(e->{
      Conflict c=list.getSelectedValue(); if(c==null) return;
      String at=JOptionPane.showInputDialog(dlg,"Heure de coupe (HH:mm) :","10:00");
      try{
        LocalTime t=LocalTime.parse(at);
        LocalDateTime dt=from.atTime(t);
        svc.resolveSplit(c.getB(), dt); refreshPlanning(); conflictsBtn.setText("Conflits (?)");
      }catch(Exception ex){ JOptionPane.showMessageDialog(dlg,"Format invalide."); }
    });

    dlg.setVisible(true);
  }

  public void reload(){
    refreshPlanning();
  }

  private void generateQuotesForSelection(){
    if (tabs == null || tabs.getSelectedComponent() != tableView.getComponent()){
      Toasts.info(this, "Sélectionnez les interventions dans l'onglet Liste pour générer les devis.");
      return;
    }
    List<Intervention> selection = tableView.getSelection();
    if (selection.isEmpty()){
      Toasts.info(this, "Aucune intervention sélectionnée");
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    var quoteService = ServiceFactory.quotes();
    if (planning == null || quoteService == null){
      Toasts.error(this, "Service indisponible pour générer les devis");
      return;
    }
    Map<UUID, Resource> catalog = new HashMap<>();
    for (Resource resource : planning.listResources()){
      if (resource != null && resource.getId() != null){
        catalog.put(resource.getId(), resource);
      }
    }
    int created = 0, skipped = 0, failed = 0;
    for (Intervention intervention : selection){
      if (intervention == null){
        continue;
      }
      if (intervention.hasQuote()){
        skipped++;
        continue;
      }
      try {
        List<BillingLine> lines = ensureBillingLines(intervention, catalog);
        if (lines.isEmpty()){
          skipped++;
          continue;
        }
        Quote draft = QuoteGenerator.buildQuoteFromIntervention(intervention, lines);
        Quote createdQuote = quoteService.save(draft);
        if (createdQuote == null){
          throw new IllegalStateException("Réponse vide du service devis");
        }
        intervention.setBillingLines(lines);
        intervention.setQuoteDraft(QuoteGenerator.toDocumentLines(lines));
        intervention.setQuoteId(createdQuote.getId());
        intervention.setQuoteReference(createdQuote.getNumber());
        intervention.setQuoteNumber(createdQuote.getNumber());
        planning.saveIntervention(intervention);
        created++;
      } catch (Exception ex){
        failed++;
      }
    }
    refreshPlanning();
    String message = String.format("Devis générés: %d • ignorés: %d • erreurs: %d", created, skipped, failed);
    if (failed > 0){
      Toasts.error(this, message);
    } else if (created > 0){
      Toasts.success(this, message);
    } else {
      Toasts.info(this, message);
    }
  }

  private void refreshPlanning(){
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      calendarView.setMode(isMonthSelected() ? "Mois" : "Semaine");
      calendarView.setData(List.of());
      tableView.setData(List.of());
      return;
    }
    board.reload();
    agenda.reload();
    refreshSimpleViews(planning);
  }

  private void reloadSimpleViews(){
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      calendarView.setMode(isMonthSelected() ? "Mois" : "Semaine");
      calendarView.setData(List.of());
      tableView.setData(List.of());
      return;
    }
    refreshSimpleViews(planning);
  }

  private LocalDate[] currentSimpleRange(){
    LocalDate reference = null;
    if (simpleRefDate != null){
      Object value = simpleRefDate.getValue();
      if (value instanceof Date date){
        reference = toLocalDate(date);
      }
    }
    if (reference == null){
      reference = board.getStartDate();
    }
    if (reference == null){
      reference = LocalDate.now();
    }
    if (isMonthSelected()){
      LocalDate start = reference.withDayOfMonth(1);
      LocalDate end = start.plusMonths(1).minusDays(1);
      return new LocalDate[]{start, end};
    }
    LocalDate start = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate end = start.plusDays(6);
    return new LocalDate[]{start, end};
  }

  private boolean isMonthSelected(){
    return simplePeriod != null && "Mois".equals(String.valueOf(simplePeriod.getSelectedItem()));
  }

  private void onSimpleRangeChanged(){
    if (updatingSimpleRange){
      return;
    }
    reloadSimpleViews();
  }

  private void shiftSimpleRange(int direction){
    LocalDate[] range = currentSimpleRange();
    LocalDate target = isMonthSelected() ? range[0].plusMonths(direction) : range[0].plusWeeks(direction);
    updateSimpleReference(target);
  }

  private List<BillingLine> ensureBillingLines(Intervention intervention, Map<UUID, Resource> catalog){
    List<BillingLine> lines = intervention.getBillingLines();
    if (lines != null && !lines.isEmpty()){
      return lines;
    }
    List<ResourceRef> refs = intervention.getResources();
    List<BillingLine> generated = PreDevisUtil.fromResourceRefs(refs, catalog);
    intervention.setBillingLines(generated);
    return generated;
  }

  private void updateSimpleReference(LocalDate date){
    if (simpleRefDate == null || date == null){
      return;
    }
    updatingSimpleRange = true;
    try {
      simpleRefDate.setValue(toDate(date));
    } finally {
      updatingSimpleRange = false;
    }
    reloadSimpleViews();
  }

  private Date toDate(LocalDate date){
    LocalDate effective = date != null ? date : LocalDate.now();
    return Date.from(effective.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private LocalDate toLocalDate(Date date){
    if (date == null){
      return LocalDate.now();
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private LocalDateTime toLocalDateTime(Date date){
    if (date == null){
      return null;
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  private void refreshSimpleViews(PlanningService planning){
    List<Intervention> list = List.of();
    boolean success = true;
    LocalDate[] range = currentSimpleRange();
    LocalDate from = range[0];
    LocalDate to = range[1];
    try {
      List<Intervention> fetched = planning.listInterventions(from, to);
      if (fetched != null){
        list = fetched;
      }
    } catch (Exception ex){
      success = false;
      list = List.of();
      Toasts.error(this, "Impossible de charger les interventions");
    }
    calendarView.setMode(isMonthSelected() ? "Mois" : "Semaine");
    calendarView.setData(list);
    tableView.setData(list);
    if (success){
      Toasts.info(this, list.size() + " intervention(s) chargée(s)");
    }
  }

  private void moveIntervention(Intervention it, LocalDate targetDay){
    if (it == null || targetDay == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible");
      return;
    }
    LocalDateTime originalStart = it.getDateHeureDebut();
    LocalDateTime originalEnd = it.getDateHeureFin();
    try {
      LocalTime startTime = originalStart != null ? originalStart.toLocalTime() : LocalTime.of(8, 0);
      LocalDateTime newStart = targetDay.atTime(startTime);
      it.setDateHeureDebut(newStart);
      if (originalStart != null && originalEnd != null){
        long minutes = Math.max(0, Duration.between(originalStart, originalEnd).toMinutes());
        it.setDateHeureFin(newStart.plusMinutes(minutes));
      } else if (originalEnd != null){
        it.setDateHeureFin(targetDay.atTime(originalEnd.toLocalTime()));
      }
      planning.saveIntervention(it);
      Toasts.success(this, "Intervention déplacée au " + targetDay.format(SIMPLE_DAY_FORMAT));
    } catch (Exception ex){
      it.setDateHeureDebut(originalStart);
      it.setDateHeureFin(originalEnd);
      Toasts.error(this, "Impossible de déplacer l'intervention");
    }
    refreshPlanning();
  }

  private void moveInterventionDateTime(Intervention it, Date newStartDate){
    if (it == null || newStartDate == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible");
      return;
    }
    LocalDateTime originalStart = it.getDateHeureDebut();
    LocalDateTime originalEnd = it.getDateHeureFin();
    LocalDateTime newStart = toLocalDateTime(newStartDate);
    try {
      it.setDateHeureDebut(newStart);
      if (originalStart != null && originalEnd != null){
        long minutes = Math.max(0, Duration.between(originalStart, originalEnd).toMinutes());
        it.setDateHeureFin(newStart.plusMinutes(minutes));
      } else if (originalEnd != null){
        it.setDateHeureFin(newStart.toLocalDate().atTime(originalEnd.toLocalTime()));
      }
      planning.saveIntervention(it);
      if (newStart != null){
        Toasts.success(this, "Intervention déplacée au " + newStart.format(SIMPLE_DAY_TIME_FORMAT));
      }
    } catch (Exception ex){
      it.setDateHeureDebut(originalStart);
      it.setDateHeureFin(originalEnd);
      Toasts.error(this, "Impossible de déplacer l'intervention");
    }
    refreshPlanning();
  }

  private void resizeInterventionEndDateTime(Intervention it, Date newEndDate){
    if (it == null || newEndDate == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible");
      return;
    }
    LocalDateTime originalEnd = it.getDateHeureFin();
    LocalDateTime newEnd = toLocalDateTime(newEndDate);
    try {
      LocalDateTime start = it.getDateHeureDebut();
      if (start != null && newEnd != null && !newEnd.isAfter(start)){
        newEnd = start.plusMinutes(Math.max(15, board.getSlotMinutes()));
      }
      it.setDateHeureFin(newEnd);
      planning.saveIntervention(it);
      Toasts.success(this, "Durée mise à jour");
    } catch (Exception ex){
      it.setDateHeureFin(originalEnd);
      Toasts.error(this, "Impossible de mettre à jour la durée");
    }
    refreshPlanning();
  }

  private void addInterventionDialog(){
    var planning = ServiceFactory.planning();
    if (planning == null){
      JOptionPane.showMessageDialog(this, "Service planning indisponible", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }
    InterventionDialog dialog = new InterventionDialog(
        SwingUtilities.getWindowAncestor(this),
        planning,
        ServiceFactory.clients(),
        ServiceFactory.interventionTypes());
    dialog.setOnSave(intervention -> {
      planning.saveIntervention(intervention);
      refreshPlanning();
    });
    dialog.edit(new Intervention());
    dialog.setVisible(true);
  }

  private void openInterventionEditor(Intervention it){
    if (it == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      JOptionPane.showMessageDialog(this, "Service planning indisponible", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }
    InterventionDialog dialog = new InterventionDialog(
        SwingUtilities.getWindowAncestor(this),
        planning,
        ServiceFactory.clients(),
        ServiceFactory.interventionTypes());
    dialog.setOnSave(updated -> {
      planning.saveIntervention(updated);
      refreshPlanning();
    });
    dialog.edit(it);
    dialog.setVisible(true);
  }

  private void putUndoRedoKeymap(){
    int WHEN = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
    getInputMap(WHEN).put(KeyStroke.getKeyStroke("control Z"), "undo");
    getInputMap(WHEN).put(KeyStroke.getKeyStroke("control Y"), "redo");
    getActionMap().put("undo", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ CommandBus.get().undo(); refreshPlanning(); }});
    getActionMap().put("redo", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ CommandBus.get().redo(); refreshPlanning(); }});
  }
}
