package com.materiel.suite.client.ui.planning;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.DocumentTotals;
import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.TimelineEvent;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.SalesService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TimelineService;
import com.materiel.suite.client.settings.EmailSettings;
import com.materiel.suite.client.ui.common.Accessible;
import com.materiel.suite.client.ui.common.EmailPreviewDialog;
import com.materiel.suite.client.ui.common.KeymapUtil;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.common.Tooltips;
import com.materiel.suite.client.ui.commands.CommandBus;
import com.materiel.suite.client.ui.MainFrame;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.ui.interventions.InterventionDialog;
import com.materiel.suite.client.ui.interventions.PreDevisUtil;
import com.materiel.suite.client.ui.interventions.QuoteGenerator;
import com.materiel.suite.client.util.MailSender;
import org.apache.commons.text.StringEscapeUtils;

public class PlanningPanel extends JPanel {
  private enum QuoteFilter {
    TOUS("Tous"),
    A_DEVISER("À deviser"),
    DEJA_DEVISE("Déjà devisé");

    private final String label;

    QuoteFilter(String label){
      this.label = label;
    }

    @Override public String toString(){
      return label;
    }
  }
  private static final DateTimeFormatter SIMPLE_DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter SIMPLE_DAY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private final PlanningBoard board = new PlanningBoard();
  private final AgendaBoard agenda = new AgendaBoard();
  private final JButton bulkQuoteBtn = new JButton("Générer devis", IconRegistry.small("file-plus"));
  private final JButton exportIcsBtn = new JButton("Exporter .ics", IconRegistry.small("calendar"));
  private final JButton exportMissionBtn = new JButton("Ordre de mission (PDF)", IconRegistry.colored("file"));
  private final JButton sendBtn = new JButton("Envoyer aux ressources", IconRegistry.colored("info"));
  private final JButton dispatcherBtn = new JButton("Mode Dispatcher", IconRegistry.colored("task"));
  private final JButton dryRunBtn = new JButton("Prévisualiser", IconRegistry.small("calculator"));
  private final JComboBox<QuoteFilter> quoteFilter = new JComboBox<>(QuoteFilter.values());
  private final JTextField search = new JTextField(18);
  private final JPanel bulkBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
  private final JLabel selCountLabel = new JLabel("0 interventions");
  private final JLabel bulkBadges = new JLabel();
  private JButton conflictsBtn;
  private JPanel ganttContainer;
  private JTabbedPane tabs;
  private final InterventionView calendarView = new InterventionCalendarView();
  private final InterventionView tableView = new InterventionTableView();
  private final KanbanPanel kanbanView = new KanbanPanel();
  private JToggleButton modeToggle;
  private boolean agendaMode;
  private boolean updatingModeToggle;
  private JComboBox<String> simplePeriod;
  private JSpinner simpleRefDate;
  private boolean updatingSimpleRange;
  private List<Intervention> allInterventions = List.of();
  private List<Intervention> currentSelection = List.of();
  private boolean kanbanHasError;

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
    calendarView.setSelectionListener(this::updateSelectionLater);
    tableView.setOnOpen(this::openInterventionEditor);
    tableView.setSelectionListener(this::updateSelectionLater);

    ganttContainer = center;
    tabs = new JTabbedPane();
    tabs.addTab("Planning", IconRegistry.small("task"), center);
    tabs.addTab("Calendrier", IconRegistry.small("calendar"), calendarView.getComponent());
    tabs.addTab("Liste", IconRegistry.small("file"), tableView.getComponent());
    tabs.addTab("Pipeline", IconRegistry.small("invoice"), kanbanView);
    tabs.addChangeListener(e -> updateModeToggleState());
    add(tabs, BorderLayout.CENTER);
    kanbanView.setListener(new KanbanPanel.Listener(){
      @Override public void onOpen(Intervention intervention){
        openInterventionEditor(intervention);
      }

      @Override public void onGenerateQuote(Intervention intervention){
        generateQuoteFromKanban(intervention);
      }

      @Override public void onCreateInvoice(Intervention intervention){
        createInvoiceFromKanban(intervention);
      }

      @Override public void onStatusChanged(Intervention intervention){
        PlanningService planning = ServiceFactory.planning();
        if (planning == null){
          throw new IllegalStateException("Service planning indisponible.");
        }
        planning.saveIntervention(intervention);
      }
    });
    search.getDocument().addDocumentListener(new DocumentListener(){
      @Override public void insertUpdate(DocumentEvent e){ applySearch(); }
      @Override public void removeUpdate(DocumentEvent e){ applySearch(); }
      @Override public void changedUpdate(DocumentEvent e){ applySearch(); }
    });
    updateModeToggleState();

    bulkBar.setBorder(new EmptyBorder(4, 8, 4, 8));
    bulkBar.add(new JLabel("Sélection :"));
    bulkBar.add(selCountLabel);
    JSeparator bulkSep = new JSeparator(SwingConstants.VERTICAL);
    bulkSep.setPreferredSize(new Dimension(1, 24));
    bulkBar.add(bulkSep);
    bulkBar.add(dryRunBtn);
    bulkBar.add(bulkQuoteBtn);
    bulkBar.add(exportIcsBtn);
    Tooltips.setWithShortcut(exportMissionBtn, "Générer un ordre de mission PDF pour la sélection", "P");
    Tooltips.setWithShortcut(sendBtn, "Envoyer les ordres de mission par email aux ressources", "M");
    Tooltips.setWithShortcut(dispatcherBtn, "Ouvrir le mode Dispatcher (planification côte à côte)", null);
    Accessible.a11y(exportMissionBtn,
        "Exporter Ordre de Mission PDF",
        "Génère un ordre de mission PDF pour les interventions sélectionnées.");
    Accessible.a11y(sendBtn,
        "Envoyer aux ressources",
        "Envoie les ordres de mission aux ressources par email.");
    Accessible.a11y(dispatcherBtn,
        "Ouvrir le mode Dispatcher",
        "Affiche le planning en mode Dispatcher pour répartir les ressources.");
    bulkBar.add(exportMissionBtn);
    bulkBar.add(sendBtn);
    bulkBar.add(dispatcherBtn);
    bulkBar.add(Box.createHorizontalStrut(16));
    bulkBar.add(bulkBadges);
    bulkBadges.setText("");
    bulkBar.setVisible(false);
    dryRunBtn.setEnabled(false);
    bulkQuoteBtn.setEnabled(false);
    exportIcsBtn.setEnabled(false);
    exportMissionBtn.setEnabled(false);
    sendBtn.setEnabled(false);
    add(bulkBar, BorderLayout.SOUTH);

    bulkQuoteBtn.addActionListener(e -> generateQuotesForSelection());
    dryRunBtn.addActionListener(e -> showDryRun());
    exportIcsBtn.addActionListener(e -> exportIcs());
    exportMissionBtn.addActionListener(e -> exportMissionPdf());
    sendBtn.addActionListener(e -> sendMissionOrders());
    dispatcherBtn.addActionListener(e -> openDispatcher());
    updateSelectionUI(List.of());

    reload();

    putUndoRedoKeymap();
    installKeymap();
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
    JLabel quoteFilterLabel = new JLabel("Filtre devis:", IconRegistry.small("filter"), JLabel.LEFT);
    quoteFilterLabel.setIconTextGap(4);
    quoteFilter.setSelectedItem(QuoteFilter.TOUS);
    quoteFilter.addActionListener(e -> updateFilteredSimpleViews());
    bar.add(Box.createHorizontalStrut(12));
    bar.add(quoteFilterLabel);
    bar.add(quoteFilter);
    JLabel searchLabel = new JLabel("Rechercher :");
    bar.add(Box.createHorizontalStrut(12));
    bar.add(searchLabel);
    bar.add(search);
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

  /* ---------- Gestion de la sélection ---------- */
  private void updateSelectionLater(List<Intervention> selection){
    SwingUtilities.invokeLater(() -> updateSelectionUI(selection));
  }

  private void updateSelectionUI(List<Intervention> selection){
    List<Intervention> safe = new ArrayList<>();
    if (selection != null){
      for (Intervention intervention : selection){
        if (intervention != null){
          safe.add(intervention);
        }
      }
    }
    currentSelection = safe.isEmpty() ? List.of() : List.copyOf(safe);
    int count = currentSelection.size();
    selCountLabel.setText(count + " intervention" + (count == 1 ? "" : "s"));
    boolean active = count > 0;
    dryRunBtn.setEnabled(active);
    bulkQuoteBtn.setEnabled(active);
    exportIcsBtn.setEnabled(active);
    exportMissionBtn.setEnabled(active);
    sendBtn.setEnabled(active);
    bulkBar.setVisible(active);
    if (active){
      long quoted = currentSelection.stream().filter(Intervention::hasQuote).count();
      long pending = count - quoted;
      bulkBadges.setText(badge("À deviser", (int) pending) + "  " + badge("Devisé", (int) quoted));
    } else {
      bulkBadges.setText("");
    }
    revalidate();
    repaint();
  }

  private List<Intervention> selectedInterventions(){
    return currentSelection;
  }

  private static String badge(String label, int value){
    return "<html><span style='background:#EEF3FE;border:1px solid #90CAF9;border-radius:9px;padding:2px 6px;font-size:11px'>"
        + label + ": " + value + "</span></html>";
  }

  private List<Intervention> selectedInterventionsForQuotes(String action){
    List<Intervention> selection = selectedInterventions();
    if (selection.isEmpty()){
      Toasts.info(this, "Aucune intervention sélectionnée pour " + action);
      return List.of();
    }
    return selection;
  }

  private void showDryRun(){
    List<Intervention> selection = selectedInterventions();
    if (selection.isEmpty()){
      Toasts.info(this, "Aucune intervention sélectionnée");
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service indisponible pour prévisualiser les devis");
      return;
    }
    Map<UUID, Resource> catalog = new HashMap<>();
    try {
      List<Resource> resources = planning.listResources();
      if (resources != null){
        for (Resource resource : resources){
          if (resource != null && resource.getId() != null){
            catalog.put(resource.getId(), resource);
          }
        }
      }
    } catch (Exception ex){
      Toasts.error(this, "Impossible de récupérer les ressources");
      return;
    }

    NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    DecimalFormat qtyFormat = new DecimalFormat("0.##");
    StringBuilder details = new StringBuilder();
    int prepared = 0;
    int skipped = 0;
    int failed = 0;

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
        Quote preview = QuoteGenerator.buildQuoteFromIntervention(intervention, lines);
        if (prepared > 0){
          details.append('\n');
        }
        details.append("• ").append(displayName(intervention)).append('\n');
        DocumentTotals totals = preview.getTotals();
        details.append("   Total HT: ").append(currency.format(totals.getTotalHT()));
        details.append(" — TTC: ").append(currency.format(totals.getTotalTTC())).append('\n');
        for (DocumentLine line : preview.getLines()){
          if (line == null){
            continue;
          }
          details.append("   · ").append(safeDesignation(line)).append(" — ");
          double quantity = line.getQuantite();
          if (quantity > 0d){
            details.append(qtyFormat.format(quantity));
            String unit = line.getUnite();
            if (unit != null && !unit.isBlank()){
              details.append(' ').append(unit);
            }
            details.append(" × ").append(currency.format(line.getPrixUnitaireHT()));
          } else {
            details.append(currency.format(line.getPrixUnitaireHT()));
          }
          details.append(" = ").append(currency.format(line.lineHT())).append(" HT\n");
        }
        prepared++;
      } catch (Exception ex){
        failed++;
      }
    }

    if (prepared == 0){
      String summary = String.format("Aucun devis à prévisualiser (ignorés: %d • erreurs: %d)", skipped, failed);
      if (failed > 0){
        Toasts.error(this, summary);
      } else {
        Toasts.info(this, summary);
      }
      return;
    }

    JTextArea area = new JTextArea(details.toString().stripTrailing());
    area.setEditable(false);
    area.setLineWrap(false);
    area.setWrapStyleWord(false);
    area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, area.getFont().getSize()));
    JScrollPane scroll = new JScrollPane(area);
    scroll.setPreferredSize(new Dimension(560, Math.min(480, 200 + prepared * 120)));
    JOptionPane.showMessageDialog(this, scroll, "Prévisualisation des devis", JOptionPane.INFORMATION_MESSAGE);

    String summary = String.format("Prévisualisations: %d • ignorés: %d • erreurs: %d", prepared, skipped, failed);
    if (failed > 0){
      Toasts.error(this, summary);
    } else if (skipped > 0){
      Toasts.info(this, summary);
    } else {
      Toasts.success(this, summary);
    }
  }

  private void generateQuotesForSelection(){
    List<Intervention> selection = selectedInterventionsForQuotes("générer les devis");
    if (selection.isEmpty()){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    SalesService sales = ServiceFactory.sales();
    TimelineService timeline = ServiceFactory.timeline();
    if (planning == null || sales == null){
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
        intervention.setBillingLines(lines);
        QuoteV2 createdQuote = sales.createQuoteFromIntervention(intervention);
        if (createdQuote == null){
          throw new IllegalStateException("Réponse vide du service devis");
        }
        applyQuoteToIntervention(intervention, createdQuote, lines);
        planning.saveIntervention(intervention);
        if (timeline != null && intervention.getId() != null){
          try {
            TimelineEvent event = new TimelineEvent();
            event.setType("ACTION");
            String reference = createdQuote.getReference();
            if (reference == null || reference.isBlank()){
              reference = createdQuote.getId();
            }
            event.setMessage(reference == null || reference.isBlank()
                ? "Devis généré en masse"
                : "Devis généré en masse : " + reference);
            event.setTimestamp(Instant.now());
            event.setAuthor(System.getProperty("user.name", "user"));
            timeline.append(intervention.getId().toString(), event);
          } catch (Exception ignore) {
          }
        }
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

  private void generateQuoteFromKanban(Intervention intervention){
    if (intervention == null){
      return;
    }
    if (intervention.hasQuote()){
      Toasts.info(this, "Un devis est déjà associé à cette intervention.");
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    SalesService sales = ServiceFactory.sales();
    TimelineService timeline = ServiceFactory.timeline();
    if (planning == null || sales == null){
      Toasts.error(this, "Service indisponible pour générer le devis");
      return;
    }
    Map<UUID, Resource> catalog = new HashMap<>();
    try {
      for (Resource resource : planning.listResources()){
        if (resource != null && resource.getId() != null){
          catalog.put(resource.getId(), resource);
        }
      }
    } catch (Exception ex){
      Toasts.error(this, "Impossible de récupérer les ressources : " + ex.getMessage());
      return;
    }
    try {
      List<BillingLine> lines = ensureBillingLines(intervention, catalog);
      if (lines.isEmpty()){
        Toasts.info(this, "Aucune ligne de facturation pour générer un devis.");
        return;
      }
      intervention.setBillingLines(lines);
      QuoteV2 quote = sales.createQuoteFromIntervention(intervention);
      if (quote == null){
        throw new IllegalStateException("Réponse vide du service devis");
      }
      applyQuoteToIntervention(intervention, quote, lines);
      planning.saveIntervention(intervention);
      if (timeline != null && intervention.getId() != null){
        try {
          TimelineEvent event = new TimelineEvent();
          event.setType("ACTION");
          String reference = quote.getReference();
          if (reference == null || reference.isBlank()){
            reference = quote.getId();
          }
          event.setMessage(reference == null || reference.isBlank()
              ? "Devis généré"
              : "Devis généré : " + reference);
          event.setTimestamp(Instant.now());
          event.setAuthor(System.getProperty("user.name", "user"));
          timeline.append(intervention.getId().toString(), event);
        } catch (Exception ignore){
        }
      }
      String reference = quote.getReference();
      if (reference == null || reference.isBlank()){
        reference = quote.getId();
      }
      if (reference == null || reference.isBlank()){
        Toasts.success(this, "Devis généré");
      } else {
        Toasts.success(this, "Devis généré — " + reference);
      }
      refreshPlanning();
    } catch (Exception ex){
      Toasts.error(this, "Impossible de générer le devis : " + ex.getMessage());
    }
  }

  private void createInvoiceFromKanban(Intervention intervention){
    if (intervention == null){
      return;
    }
    if (isInvoiced(intervention)){
      Toasts.info(this, "Une facture est déjà associée à cette intervention.");
      return;
    }
    if (!intervention.hasQuote()){
      Toasts.info(this, "Générez un devis avant de créer la facture.");
      return;
    }
    UUID quoteId = intervention.getQuoteId();
    if (quoteId == null){
      Toasts.info(this, "Identifiant de devis introuvable pour cette intervention.");
      return;
    }
    var invoices = ServiceFactory.invoices();
    PlanningService planning = ServiceFactory.planning();
    if (invoices == null || planning == null){
      Toasts.error(this, "Service facturation indisponible");
      return;
    }
    try {
      Invoice invoice = invoices.createFromQuote(quoteId);
      if (invoice == null){
        Toasts.error(this, "Le service n'a retourné aucune facture.");
        return;
      }
      String number = invoice.getNumber();
      if (number != null && !number.isBlank()){
        intervention.setInvoiceNumber(number);
      }
      planning.saveIntervention(intervention);
      String message = number == null || number.isBlank()
          ? "Facture créée"
          : "Facture créée — " + number;
      Toasts.success(this, message);
      refreshPlanning();
    } catch (Exception ex){
      Toasts.error(this, "Impossible de créer la facture : " + ex.getMessage());
    }
  }

  private boolean isInvoiced(Intervention intervention){
    if (intervention == null){
      return false;
    }
    String invoiceNumber = intervention.getInvoiceNumber();
    return invoiceNumber != null && !invoiceNumber.isBlank();
  }

  private void maybeSaveLog(List<String[]> rows){
    if (rows == null || rows.size() <= 1){
      return;
    }
    int choice = JOptionPane.showConfirmDialog(
        this,
        "Enregistrer le journal d'envoi (CSV) ?",
        "Journal",
        JOptionPane.YES_NO_OPTION);
    if (choice != JOptionPane.YES_OPTION){
      return;
    }
    JFileChooser chooser = new JFileChooser();
    String defaultName = "journal-envoi-"
        + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        + ".csv";
    chooser.setSelectedFile(new File(defaultName));
    int result = chooser.showSaveDialog(this);
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File destination = chooser.getSelectedFile();
    try (PrintWriter writer = new PrintWriter(destination, StandardCharsets.UTF_8)){
      for (String[] row : rows){
        writer.println(csvRow(row));
      }
      Toasts.success(this, "Journal enregistré : " + destination.getName());
    } catch (Exception ex){
      Toasts.error(this, "Impossible d'enregistrer le journal : " + ex.getMessage());
    }
  }

  private String timestamp(){
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
  }

  private String renderSubject(List<Intervention> interventions, EmailSettings settings){
    if (settings == null){
      return "";
    }
    if (interventions == null || interventions.isEmpty()){
      return settings.getSubjectTemplate();
    }
    return fillTemplate(settings.getSubjectTemplate(), interventions.get(0));
  }

  private String renderBody(List<Intervention> interventions, EmailSettings settings){
    if (settings == null){
      return "";
    }
    if (interventions == null || interventions.isEmpty()){
      return "";
    }
    String template = settings.getBodyTemplate();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < interventions.size(); i++){
      if (i > 0){
        builder.append("\n---\n");
      }
      builder.append(fillTemplate(template, interventions.get(i)));
    }
    return builder.toString();
  }

  private String renderBodyHtml(List<Intervention> interventions, EmailSettings settings, String toEmail){
    if (settings == null){
      return "";
    }
    if (interventions == null || interventions.isEmpty()){
      return "";
    }
    String template = settings.getHtmlTemplate();
    StringBuilder builder = new StringBuilder();
    String ids = interventions.stream()
        .map(Intervention::getId)
        .filter(Objects::nonNull)
        .map(UUID::toString)
        .collect(Collectors.joining("|"));
    for (int i = 0; i < interventions.size(); i++){
      Intervention intervention = interventions.get(i);
      String filled = fillTemplateHtml(template, intervention);
      if (i == 0 && !filled.isBlank()){
        String target = nz(toEmail);
        String pixel;
        if (ids.isBlank()){
          pixel = "${pixel(to=" + target + ")}";
        } else {
          pixel = "${pixel(ids=" + ids + ",to=" + target + ")}";
        }
        if (filled.contains("${pixel}")){
          filled = filled.replace("${pixel}", pixel);
        } else {
          filled = filled + pixel;
        }
      } else {
        filled = filled.replace("${pixel}", "");
      }
      builder.append(filled);
      if (i < interventions.size() - 1){
        builder.append("<hr style=\"border:none;border-top:1px solid #ddd;margin:16px 0\"/>");
      }
    }
    return builder.toString();
  }

  private String fillTemplate(String template, Intervention intervention){
    if (template == null){
      return "";
    }
    if (intervention == null){
      return template;
    }
    TemplateValues values = templateValues(intervention);
    return applyTemplate(template, values, false);
  }

  private String fillTemplateHtml(String template, Intervention intervention){
    if (template == null){
      return "";
    }
    if (intervention == null){
      return template;
    }
    TemplateValues values = templateValues(intervention);
    return applyTemplate(template, values, true);
  }

  private TemplateValues templateValues(Intervention intervention){
    DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
    LocalDateTime start = intervention.getDateHeureDebut();
    LocalDateTime end = intervention.getDateHeureFin();
    LocalDate startDate = intervention.getDateDebut();
    LocalDate endDate = intervention.getDateFin();
    String date;
    if (start != null){
      date = dayFormat.format(start);
    } else if (startDate != null){
      date = dayFormat.format(startDate);
    } else {
      date = "—";
    }
    String timeRange;
    if (start != null && end != null){
      timeRange = timeFormat.format(start) + "–" + timeFormat.format(end);
    } else if (start != null){
      timeRange = timeFormat.format(start) + "–";
    } else if (end != null){
      timeRange = "–" + timeFormat.format(end);
    } else if (startDate != null && endDate != null){
      timeRange = dayFormat.format(startDate) + "–" + dayFormat.format(endDate);
    } else {
      timeRange = "—";
    }
    String client = trimToEmpty(intervention.getClientName());
    String clientLine = client.isBlank() ? "" : " pour " + client;
    String address = trimToEmpty(intervention.getAddress());
    if (address.isBlank()){
      address = "—";
    }
    String title = trimToEmpty(intervention.getLabel());
    if (title.isBlank()){
      title = "Intervention";
    }
    String resources = resourceList(intervention.getResources());
    return new TemplateValues(date, timeRange, client, clientLine, address, title, resources);
  }

  private String applyTemplate(String template, TemplateValues values, boolean html){
    String date = html ? escapeHtml(values.date()) : values.date();
    String timeRange = html ? escapeHtml(values.timeRange()) : values.timeRange();
    String client = html ? escapeHtml(values.client()) : values.client();
    String clientLine = html ? escapeHtml(values.clientLine()) : values.clientLine();
    String address = html ? escapeHtml(values.address()) : values.address();
    String title = html ? escapeHtml(values.title()) : values.title();
    String resources = html ? escapeHtml(values.resourceList()) : values.resourceList();
    return template
        .replace("${date}", date)
        .replace("${timeRange}", timeRange)
        .replace("${client}", client)
        .replace("${clientLine}", clientLine)
        .replace("${address}", address)
        .replace("${title}", title)
        .replace("${resourceList}", resources);
  }

  private String escapeHtml(String value){
    return StringEscapeUtils.escapeHtml4(value == null ? "" : value);
  }

  private record TemplateValues(String date, String timeRange, String client, String clientLine,
                               String address, String title, String resourceList) {
  }

  private String resourceList(List<ResourceRef> refs){
    if (refs == null || refs.isEmpty()){
      return "—";
    }
    List<String> names = new ArrayList<>();
    for (ResourceRef ref : refs){
      if (ref == null){
        continue;
      }
      String name = trimToEmpty(ref.getName());
      if (name.isEmpty()){
        if (ref.getId() != null){
          name = ref.getId().toString();
        } else {
          name = "Ressource";
        }
      }
      names.add(name);
    }
    return names.isEmpty() ? "—" : String.join(", ", names);
  }

  private String resolveResourceName(ResourceRef ref, Map<UUID, Resource> cache, PlanningService planning){
    if (ref == null){
      return "Ressource";
    }
    String name = trimToEmpty(ref.getName());
    if (!name.isEmpty()){
      return name;
    }
    UUID id = ref.getId();
    if (id == null){
      return "Ressource";
    }
    Resource resource = cache.computeIfAbsent(id, key -> {
      try {
        return planning.getResource(key);
      } catch (Exception ex){
        return null;
      }
    });
    if (resource != null && resource.getName() != null && !resource.getName().isBlank()){
      return resource.getName();
    }
    return id.toString();
  }

  private String resolveResourceEmail(ResourceRef ref, Map<UUID, Resource> cache, PlanningService planning){
    if (ref == null){
      return null;
    }
    UUID id = ref.getId();
    if (id == null){
      return null;
    }
    Resource resource = cache.computeIfAbsent(id, key -> {
      try {
        return planning.getResource(key);
      } catch (Exception ex){
        return null;
      }
    });
    if (resource == null){
      return null;
    }
    String email = resource.getEmail();
    if (email == null){
      return null;
    }
    String trimmed = email.trim();
    return trimmed.contains("@") ? trimmed : null;
  }

  private static String formatCc(String cc){
    String value = nz(cc);
    return value.isEmpty() ? "" : " (cc " + value + ")";
  }

  private static String trimToEmpty(String value){
    return value == null ? "" : value.trim();
  }

  private static String nz(String value){
    return value == null ? "" : value.trim();
  }

  private static String csvRow(String[] values){
    if (values == null || values.length == 0){
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.length; i++){
      if (i > 0){
        builder.append(',');
      }
      builder.append(csvCell(values[i]));
    }
    return builder.toString();
  }

  private static String csvCell(String value){
    String data = value == null ? "" : value;
    boolean needsQuote = data.contains(",") || data.contains("\"") || data.contains("\n") || data.contains("\r");
    String escaped = data.replace("\"", "\"\"");
    return needsQuote ? "\"" + escaped + "\"" : escaped;
  }

  private static final class Recipient {
    final String name;
    final String email;
    final List<Intervention> interventions = new ArrayList<>();

    Recipient(String name, String email){
      this.name = name;
      this.email = email;
    }
  }

  /* ---------- Exposition d'actions pour la Command Palette ---------- */
  public void actionGenerateQuotes(){
    generateQuotesForSelection();
  }

  public void actionDryRun(){
    showDryRun();
  }

  public void actionFilterToDeviser(){
    if (quoteFilter != null){
      quoteFilter.setSelectedItem(QuoteFilter.A_DEVISER);
      updateFilteredSimpleViews();
    }
  }

  public void actionFilterDejaDevise(){
    if (quoteFilter != null){
      quoteFilter.setSelectedItem(QuoteFilter.DEJA_DEVISE);
      updateFilteredSimpleViews();
    }
  }

  public void actionReload(){
    reload();

  }

  public void actionFilterCycle(){
    cycleQuoteFilter();
  }

  public int getSelectedCount(){
    return selectedInterventions().size();
  }

  public String getCurrentFilterLabel(){
    if (quoteFilter == null){
      return QuoteFilter.TOUS.toString();
    }
    Object value = quoteFilter.getSelectedItem();
    if (value instanceof QuoteFilter filter){
      return filter.toString();
    }
    return value != null ? value.toString() : QuoteFilter.TOUS.toString();
  }

  private void refreshPlanning(){
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      calendarView.setMode(isMonthSelected() ? "Mois" : "Semaine");
      allInterventions = List.of();
      kanbanHasError = true;
      kanbanView.showError("Service planning indisponible",
          "Impossible de charger les interventions du pipeline.",
          this::reload);
      updateFilteredSimpleViews();
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
      allInterventions = List.of();
      kanbanHasError = true;
      kanbanView.showError("Service planning indisponible",
          "Impossible de charger les interventions du pipeline.",
          this::reload);
      updateFilteredSimpleViews();
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

  private void applyQuoteToIntervention(Intervention intervention, QuoteV2 quote, List<BillingLine> lines){
    if (intervention == null || quote == null){
      return;
    }
    if (lines != null){
      intervention.setBillingLines(new ArrayList<>(lines));
      intervention.setQuoteDraft(QuoteGenerator.toDocumentLines(lines));
    }
    String id = quote.getId();
    if (id != null && !id.isBlank()){
      try {
        intervention.setQuoteId(UUID.fromString(id));
      } catch (IllegalArgumentException ex){
        intervention.setQuoteId(null);
      }
    } else {
      intervention.setQuoteId(null);
    }
    String reference = quote.getReference();
    if (reference != null && !reference.isBlank()){
      intervention.setQuoteReference(reference);
      intervention.setQuoteNumber(reference);
    } else {
      intervention.setQuoteReference(null);
      intervention.setQuoteNumber(null);
    }
  }

  private String displayName(Intervention intervention){
    if (intervention == null){
      return "Intervention";
    }
    String client = intervention.getClientName();
    if (client != null && !client.isBlank()){
      return client;
    }
    String label = intervention.getLabel();
    if (label != null && !label.isBlank()){
      return label;
    }
    UUID id = intervention.getId();
    return id != null ? "Intervention " + id : "Intervention";
  }

  private String safeDesignation(DocumentLine line){
    if (line == null){
      return "(ligne)";
    }
    String designation = line.getDesignation();
    if (designation == null || designation.isBlank()){
      return "(Sans libellé)";
    }
    return designation;
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
    kanbanHasError = false;
    try {
      List<Intervention> fetched = planning.listInterventions(from, to);
      if (fetched != null){
        list = fetched;
      }
    } catch (Exception ex){
      success = false;
      list = List.of();
      kanbanHasError = true;
      String message = ex.getMessage();
      kanbanView.showError("Impossible de charger le pipeline",
          message == null || message.isBlank() ? "Erreur inconnue." : message,
          this::reload);
      Toasts.error(this, "Impossible de charger les interventions");
    }
    calendarView.setMode(isMonthSelected() ? "Mois" : "Semaine");
    allInterventions = sanitizeInterventions(list);
    updateFilteredSimpleViews();
    if (success){
      Toasts.info(this, allInterventions.size() + " intervention(s) chargée(s)");
    }
  }

  private List<Intervention> sanitizeInterventions(List<Intervention> list){
    if (list == null || list.isEmpty()){
      return List.of();
    }
    List<Intervention> sanitized = new ArrayList<>();
    for (Intervention intervention : list){
      if (intervention != null){
        sanitized.add(intervention);
      }
    }
    return sanitized.isEmpty() ? List.of() : List.copyOf(sanitized);
  }

  private void updateFilteredSimpleViews(){
    List<Intervention> base = allInterventions != null ? allInterventions : List.of();
    QuoteFilter filter = (QuoteFilter) quoteFilter.getSelectedItem();
    if (filter == null){
      filter = QuoteFilter.TOUS;
    }
    List<Intervention> dataset;
    if (filter == QuoteFilter.TOUS){
      dataset = base;
      calendarView.setData(base);
      tableView.setData(base);
    } else {
      List<Intervention> filtered = new ArrayList<>();
      for (Intervention intervention : base){
        if (intervention == null){
          continue;
        }
        boolean keep;
        if (filter == QuoteFilter.A_DEVISER){
          keep = !intervention.hasQuote();
        } else {
          keep = intervention.hasQuote();
        }
        if (keep){
          filtered.add(intervention);
        }
      }
      dataset = filtered;
      calendarView.setData(filtered);
      tableView.setData(filtered);
    }
    if (!kanbanHasError){
      kanbanView.setData(dataset);
      applySearch();
    }
  }

  /* ---------- Raccourcis clavier locaux ---------- */
  private void installKeymap(){
    KeymapUtil.bind(this, "planning-new", KeyEvent.VK_N, KeymapUtil.menuMask(), this::shortcutCreateIntervention);
    KeymapUtil.bind(this, "planning-edit", KeyEvent.VK_E, 0, this::shortcutEditSelection);
    KeymapUtil.bind(this, "planning-duplicate", KeyEvent.VK_D, 0, this::shortcutDuplicateSelection);
    KeymapUtil.bind(this, "planning-generate", KeyEvent.VK_G, 0, this::actionGenerateQuotes);
    KeymapUtil.bind(this, "planning-preview", KeyEvent.VK_P, KeyEvent.SHIFT_DOWN_MASK, this::actionDryRun);
    KeymapUtil.bind(this, "planning-export", KeyEvent.VK_P, 0, this::exportMissionPdf);
    KeymapUtil.bind(this, "planning-send", KeyEvent.VK_M, 0, this::sendMissionOrders);
    KeymapUtil.bind(this, "planning-filter-adeviser", KeyEvent.VK_F, 0, () -> setFilterADeviser(true));
    KeymapUtil.bind(this, "planning-filter-cycle", KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK, this::cycleQuoteFilter);
    KeymapUtil.bind(this, "planning-focus-search", KeyEvent.VK_SLASH, 0, this::focusSearch);
    KeymapUtil.bind(this, "planning-reload", KeyEvent.VK_R, 0, this::actionReload);
  }

  private void shortcutCreateIntervention(){
    addInterventionDialog();
  }

  private void shortcutEditSelection(){
    List<Intervention> selection = selectedInterventions();
    if (selection.isEmpty()){
      Toasts.info(this, "Sélectionnez au moins une intervention.");
      return;
    }
    openInterventionEditor(selection.get(0));
  }

  private void shortcutDuplicateSelection(){
    duplicateSelected();
  }

  private void setFilterADeviser(boolean only){
    if (quoteFilter == null){
      return;
    }
    quoteFilter.setSelectedItem(only ? QuoteFilter.A_DEVISER : QuoteFilter.TOUS);
    updateFilteredSimpleViews();
  }

  private void applySearch(){
    if (kanbanView != null){
      kanbanView.setFilter(search.getText());
    }
  }

  private void focusSearch(){
    if (search != null){
      search.requestFocusInWindow();
      search.selectAll();
    } else {
      requestFocusInWindow();
    }
  }

  private void duplicateSelected(){
    duplicateSelected(1);
  }

  private void duplicateSelected(int dayShift){
    List<Intervention> selection = selectedInterventions();
    if (selection.isEmpty()){
      Toasts.info(this, "Sélectionnez au moins une intervention.");
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible.");
      return;
    }
    int created = 0;
    int failed = 0;
    for (Intervention original : selection){
      if (original == null){
        continue;
      }
      try {
        Intervention copy = duplicateFrom(original, dayShift);
        planning.saveIntervention(copy);
        created++;
      } catch (Exception ex){
        failed++;
      }
    }
    if (created > 0){
      refreshPlanning();
    }
    if (failed > 0){
      Toasts.error(this, String.format("Duplication : %d succès, %d échec(s)", created, failed));
    } else if (created > 0){
      Toasts.success(this, created == 1 ? "Intervention dupliquée" : created + " interventions dupliquées");
    } else {
      Toasts.info(this, "Aucune intervention dupliquée");
    }
  }

  private Intervention duplicateFrom(Intervention source, int dayShift){
    Intervention copy = new Intervention();
    if (source.getResources() != null){
      copy.setResources(new ArrayList<>(source.getResources()));
    }
    copy.setResourceId(source.getResourceId());
    String label = source.getLabel();
    copy.setLabel(label == null || label.isBlank() ? "Intervention (copie)" : label + " (copie)");
    copy.setColor(source.getColor());
    copy.setType(source.getType());
    copy.setAddress(source.getAddress());
    copy.setDescription(source.getDescription());
    copy.setInternalNote(source.getInternalNote());
    copy.setClosingNote(source.getClosingNote());
    if (source.getContacts() != null){
      copy.setContacts(new ArrayList<>(source.getContacts()));
    }
    if (source.getBillingLines() != null){
      copy.setBillingLines(new ArrayList<>(source.getBillingLines()));
    }
    if (source.getQuoteDraft() != null){
      copy.setQuoteDraft(new ArrayList<>(source.getQuoteDraft()));
    }
    copy.setClientId(source.getClientId());
    copy.setClientName(source.getClientName());
    copy.setStatus(source.getStatus());
    copy.setFavorite(source.isFavorite());
    copy.setLocked(source.isLocked());
    if (source.getDateHeureDebut() != null){
      copy.setDateHeureDebut(source.getDateHeureDebut().plusDays(dayShift));
    }
    if (source.getDateHeureFin() != null){
      copy.setDateHeureFin(source.getDateHeureFin().plusDays(dayShift));
    }
    if (source.getDateDebut() != null){
      copy.setDateDebut(source.getDateDebut().plusDays(dayShift));
    }
    if (source.getDateFin() != null){
      copy.setDateFin(source.getDateFin().plusDays(dayShift));
    }
    return copy;
  }

  /* ---------- Export Ordre de mission (PDF) ---------- */
  private void exportMissionPdf(){
    List<Intervention> selection = selectedInterventions();
    if (selection == null || selection.isEmpty()){
      Toasts.info(this, "Sélectionnez au moins une intervention.");
      return;
    }
    Object[] modes = {"Un seul PDF (multi-pages)", "Un .zip (1 PDF par ressource)"};
    int choice = JOptionPane.showOptionDialog(
        this,
        "Choisir le format d’export :",
        "Ordre de mission",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        modes,
        modes[0]
    );
    if (choice == JOptionPane.CLOSED_OPTION){
      return;
    }
    boolean includeCgv = false;
    if (choice == 0){
      int cgvChoice = JOptionPane.showConfirmDialog(
          this,
          "Inclure les CGV (si configurées) ?",
          "CGV",
          JOptionPane.YES_NO_OPTION
      );
      if (cgvChoice == JOptionPane.CLOSED_OPTION){
        return;
      }
      includeCgv = (cgvChoice == JOptionPane.YES_OPTION);
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(choice == 0 ? "ordre-de-mission.pdf" : "ordre-de-mission-par-ressource.zip"));
    int result = chooser.showSaveDialog(this);
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File destination = chooser.getSelectedFile();
    try {
      if (choice == 0){
        MissionOrderPdfExporter.export(destination, selection);
        if (includeCgv){
          MissionOrderPdfExporter.appendCgvIfAny(destination);
        }
      } else {
        MissionOrderPdfExporter.exportPerResourceZip(destination, selection);
        // CGV non injectées dans ce mode (un PDF par ressource) : à étendre si besoin.
      }
      Toasts.success(this, "Export généré : " + destination.getName());
    } catch (Exception ex){
      Toasts.error(this, "Échec génération PDF : " + ex.getMessage());
    }
  }

  private void sendMissionOrders(){
    List<Intervention> selection = selectedInterventions();
    if (selection == null || selection.isEmpty()){
      Toasts.info(this, "Sélectionnez au moins une intervention.");
      return;
    }
    EmailSettings settings = ServiceLocator.emailSettings();
    if (settings == null
        || settings.getSmtpHost() == null || settings.getSmtpHost().isBlank()
        || settings.getFromAddress() == null || settings.getFromAddress().isBlank()){
      JOptionPane.showMessageDialog(this,
          "Configurez d'abord le serveur SMTP dans Paramètres > Email.",
          "SMTP non configuré",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    int includeCgv = JOptionPane.showConfirmDialog(
        this,
        "Inclure les CGV (si configurées) ?",
        "CGV",
        JOptionPane.YES_NO_OPTION);
    if (includeCgv == JOptionPane.CLOSED_OPTION){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible");
      return;
    }

    Map<UUID, Resource> cache = new HashMap<>();
    Map<String, Recipient> recipients = new LinkedHashMap<>();
    int skipped = 0;

    for (Intervention intervention : selection){
      if (intervention == null){
        continue;
      }
      List<ResourceRef> resources = intervention.getResources();
      if (resources == null || resources.isEmpty()){
        skipped++;
        continue;
      }
      for (ResourceRef ref : resources){
        String email = resolveResourceEmail(ref, cache, planning);
        if (email == null){
          skipped++;
          continue;
        }
        String key = email.toLowerCase(Locale.ROOT);
        Recipient recipient = recipients.computeIfAbsent(key,
            k -> new Recipient(resolveResourceName(ref, cache, planning), email));
        recipient.interventions.add(intervention);
      }
    }

    if (recipients.isEmpty()){
      Toasts.info(this, "Aucune ressource avec adresse email valide.");
      return;
    }

    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    List<File> cleanup = new ArrayList<>();
    List<EmailPreviewDialog.EmailJob> jobs = new ArrayList<>();
    for (Recipient recipient : recipients.values()){
      try {
        File pdf = File.createTempFile("ordre-mission-", ".pdf", tmpDir);
        MissionOrderPdfExporter.export(pdf, recipient.interventions);
        if (includeCgv == JOptionPane.YES_OPTION){
          MissionOrderPdfExporter.appendCgvIfAny(pdf);
        }
        File ics = File.createTempFile("planning-", ".ics", tmpDir);
        IcsExporter.exportSingle(ics, recipient.interventions);
        cleanup.add(pdf);
        cleanup.add(ics);
        jobs.add(new EmailPreviewDialog.EmailJob(
            recipient.email,
            settings.getCcAddress(),
            renderSubject(recipient.interventions, settings),
            renderBody(recipient.interventions, settings),
            renderBodyHtml(recipient.interventions, settings, recipient.email),
            List.of(pdf, ics)
        ));
      } catch (Exception ex){
        cleanup.forEach(File::delete);
        Toasts.error(this, "Échec préparation pièces jointes : " + ex.getMessage());
        return;
      }
    }

    EmailPreviewDialog preview = new EmailPreviewDialog(SwingUtilities.getWindowAncestor(this), jobs);
    preview.setVisible(true);
    if (!preview.isApproved()){
      cleanup.forEach(File::delete);
      return;
    }

    int sent = 0;
    int failed = 0;
    List<String[]> log = new ArrayList<>();
    log.add(new String[]{"Date", "Destinataire", "Cc", "Sujet", "Fichiers", "Statut", "Message"});

    TimelineService timeline = ServiceFactory.timeline();

    for (EmailPreviewDialog.EmailJob job : jobs){
      String key = job.to() == null ? "" : job.to().toLowerCase(Locale.ROOT);
      Recipient recipient = recipients.get(key);
      String attachmentNames = job.attachments().stream()
          .map(File::getName)
          .collect(Collectors.joining("; "));
      try {
        MailSender.send(job.to(), job.cc(), job.subject(), job.body(), job.bodyHtml(), job.attachments());
        sent++;
        log.add(new String[]{timestamp(), job.to(), nz(job.cc()), job.subject(), attachmentNames, "OK", ""});
        if (timeline != null && recipient != null){
          for (Intervention intervention : recipient.interventions){
            if (intervention == null || intervention.getId() == null){
              continue;
            }
            TimelineEvent event = new TimelineEvent();
            event.setType("ACTION");
            event.setMessage("Ordre de mission envoyé à " + job.to() + formatCc(job.cc()));
            event.setTimestamp(Instant.now());
            event.setAuthor(System.getProperty("user.name", "user"));
            try {
              timeline.append(intervention.getId().toString(), event);
            } catch (Exception ignore){
            }
          }
        }
      } catch (Exception ex){
        failed++;
        log.add(new String[]{timestamp(), job.to(), nz(job.cc()), job.subject(), attachmentNames, "ERREUR",
            ex.getMessage() == null ? "" : ex.getMessage()});
      }
    }

    cleanup.forEach(File::delete);
    maybeSaveLog(log);

    String summary = String.format("Emails envoyés : %d • ignorés : %d • erreurs : %d", sent, skipped, failed);
    if (failed > 0){
      Toasts.error(this, summary);
    } else if (sent > 0){
      Toasts.success(this, summary);
    } else {
      Toasts.info(this, summary);
    }
  }

  private void exportIcs(){
    List<Intervention> selection = selectedInterventions();
    if (selection == null || selection.isEmpty()){
      Toasts.info(this, "Sélectionnez au moins une intervention à exporter.");
      return;
    }
    Object[] modes = {"Un seul fichier .ics", "Un .zip par ressource (.ics chacun)"};
    int choice = JOptionPane.showOptionDialog(
        this,
        "Choisir le format d’export :",
        "Export calendrier",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        modes,
        modes[0]
    );
    if (choice == JOptionPane.CLOSED_OPTION){
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(choice == 0 ? "planning-selection.ics" : "planning-par-ressource.zip"));
    int result = chooser.showSaveDialog(this);
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File destination = chooser.getSelectedFile();
    try {
      if (choice == 0){
        IcsExporter.exportSingle(destination, selection);
      } else {
        IcsExporter.exportPerResourceZip(destination, selection);
      }
      Toasts.success(this, "Export créé : " + destination.getName());
    } catch (Exception ex){
      Toasts.error(this, "Échec export : " + ex.getMessage());
    }
  }

  private void cycleQuoteFilter(){
    if (quoteFilter == null){
      return;
    }
    int count = quoteFilter.getItemCount();
    if (count <= 0){
      return;
    }
    int index = quoteFilter.getSelectedIndex();
    int next = (index + 1) % count;
    quoteFilter.setSelectedIndex(next);
    updateFilteredSimpleViews();
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

  private void openDispatcher(){
    List<Intervention> selection = selectedInterventions();
    if (selection == null || selection.isEmpty()){
      Toasts.info(this, "Sélectionnez une intervention à dispatcher.");
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible");
      return;
    }
    Intervention target = selection.get(0);
    DispatcherSplitDialog dialog = new DispatcherSplitDialog(
        SwingUtilities.getWindowAncestor(this),
        planning,
        ServiceFactory.clients(),
        ServiceFactory.interventionTypes(),
        ServiceFactory.templates(),
        target);
    dialog.setOnSave(updated -> {
      if (updated != null){
        planning.saveIntervention(updated);
      }
      refreshPlanning();
    });
    dialog.setVisible(true);
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
        ServiceFactory.interventionTypes(),
        ServiceFactory.templates());
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
        ServiceFactory.interventionTypes(),
        ServiceFactory.templates());
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
