package com.materiel.suite.client.ui.planning;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
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
import java.time.temporal.WeekFields;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.materiel.suite.client.agency.AgencyContext;
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
import com.materiel.suite.client.ui.planning.render.DefaultTileRenderer;
import com.materiel.suite.client.ui.planning.render.TileRenderer;
import com.materiel.suite.client.ui.common.Badge;
import com.materiel.suite.client.ui.common.Pill;
import com.materiel.suite.client.ui.theme.ThemeManager;
import com.materiel.suite.client.ui.theme.UiTokens;
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
  private final JLabel weekBadge = new JLabel("Semaine —");
  private final JSlider zoomSlider = new JSlider(6, 24, board.getSlotWidth());
  private final JPopupMenu displayMenu = new JPopupMenu();
  private final JComboBox<Integer> slotCombo = new JComboBox<>(new Integer[]{5, 10, 15, 30, 60});
  private LocalDate pivotMonday = LocalDate.now().with(DayOfWeek.MONDAY);
  private final JComboBox<QuoteFilter> quoteFilter = new JComboBox<>(QuoteFilter.values());
  private final JTextField search = new JTextField(18);
  private final JPanel bulkBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
  private final JLabel selCountLabel = new JLabel("0 interventions");
  private final Pill contextWeekPill = new Pill("Semaine —");
  private final Badge pendingBadge = new Badge("À deviser", Badge.Tone.INFO);
  private final Badge quotedBadge = new Badge("Devisé", Badge.Tone.OK);
  private JButton conflictsBtn;
  private JPanel ganttContainer;
  private JTabbedPane tabs;
  private final InterventionView calendarView = new InterventionCalendarView();
  private final InterventionView tableView = new InterventionTableView();
  private final KanbanPanel kanbanView = new KanbanPanel();
  private final JToggleButton cardsToggle = new JToggleButton("Vue cartes");
  private final JPanel cardsContainer = new JPanel(new GridBagLayout());
  private final JScrollPane cardsScroll = new JScrollPane(cardsContainer);
  private List<Intervention> cardsData = List.of();
  private JScrollPane planningScroll;
  private JToggleButton modeToggle;
  private JToggleButton compactToggle;
  private boolean agendaMode;
  private boolean updatingModeToggle;
  private JComboBox<String> simplePeriod;
  private JSpinner simpleRefDate;
  private boolean updatingSimpleRange;
  private List<Intervention> allInterventions = List.of();
  private List<Intervention> currentSelection = List.of();
  private boolean kanbanHasError;
  private JPanel cardsPanel;
  private JPanel cardsContainer;
  private JComboBox<String> cardsStatusFilter;
  private JCheckBox cardsToQuoteFilter;
  private JComboBox<String> cardsResourceTypeFilter;
  private JComboBox<String> cardsAgencyFilter;
  private JToggleButton cardsDensityToggle;
  private List<Intervention> cardItems = List.of();
  private final Map<UUID, Resource> resourceCatalog = new HashMap<>();

  public PlanningPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);

    planningScroll = new JScrollPane(board, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    // ===== Scroll natif rétabli + réglages douceur =====
    planningScroll.setWheelScrollingEnabled(true);
    if (planningScroll.getVerticalScrollBar() != null){
      planningScroll.getVerticalScrollBar().setUnitIncrement(28);
      planningScroll.getVerticalScrollBar().setBlockIncrement(120);
    }
    // Pour s'assurer que la roue agit sur la vue scrollée
    planningScroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
    board.setAutoscrolls(true);
    DayHeader header = new DayHeader(board);
    planningScroll.setColumnHeaderView(header);
    planningScroll.getHorizontalScrollBar().addAdjustmentListener(e -> header.repaint());

    var scrollAgenda = new JScrollPane(agenda);

    JPanel center = new JPanel(new CardLayout());
    center.add(planningScroll, "gantt");
    center.add(scrollAgenda, "agenda");

    JComponent rowHeader = new JComponent(){
      @Override public Dimension getPreferredSize(){
        return new Dimension(240, board.getPreferredSize().height);
      }
      @Override protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g.create();
        try {
          // Fond + séparateur
          g2.setColor(new Color(0xF7F7F7));
          g2.fillRect(0,0,getWidth(),getHeight());
          g2.setColor(new Color(0xDDDDDD));
          g2.drawLine(getWidth()-1,0,getWidth()-1,getHeight());

          // Virtualisation : ne peindre que les ressources visibles
          Rectangle vr = board.getVisibleRect();
          java.util.List<Resource> rs = board.getResourcesList();
          if (rs == null || rs.isEmpty()) return;

          int y = 0;
          int startY = vr.y;
          int endY = vr.y + vr.height;

          // Chercher le premier index visible en accumulant les hauteurs
          int i = 0;
          for (; i < rs.size(); i++){
            int rowH = Math.max(16, board.rowHeight(rs.get(i).getId()));
            if (y + rowH >= startY){
              break;
            }
            y += rowH;
          }

          // Dessiner jusqu'à sortir de la fenêtre visible
          for (; i < rs.size() && y <= endY; i++){
            Resource r = rs.get(i);
            int rowH = Math.max(16, board.rowHeight(r.getId()));
            g2.setColor(new Color(0xF7F7F7));
            g2.fillRect(0, y, getWidth(), rowH);
            g2.setColor(Color.DARK_GRAY);
            String name = r.getName()==null? "—" : r.getName();
            FontMetrics fm = g2.getFontMetrics();
            int textY = y + Math.max(fm.getAscent()+6, rowH/2 + fm.getAscent()/2);
            g2.drawString(name, 12, textY);
            g2.setColor(new Color(0xE0E0E0));
            g2.drawLine(0, y+rowH-1, getWidth(), y+rowH-1);
            y += rowH;
          }
        } finally {
          g2.dispose();
        }
      }
    };
    planningScroll.setRowHeaderView(rowHeader);

    // Repeindre le header quand le layout du board change
    board.addPropertyChangeListener("layout", e -> {
      rowHeader.revalidate();
      rowHeader.repaint();
      pushVisibleWindowToBoard();
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
    tabs.addTab("Calendrier", IconRegistry.small("calendar"), buildCalendarTab());
    tabs.addTab("Liste", IconRegistry.small("file"), tableView.getComponent());
    tabs.addTab("Pipeline", IconRegistry.small("invoice"), kanbanView);
    cardsPanel = buildCardsTab();
    tabs.addTab("Cartes", IconRegistry.small("info"), cardsPanel);
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
    installCardDropTarget();
    reloadCardFilters();
    updateModeToggleState();

    // Injecte un renderer « propre » si le board expose l’API.
    try {
      var m = board.getClass().getMethod("setTileRenderer", TileRenderer.class);
      m.invoke(board, new DefaultTileRenderer());
    } catch (Exception ignore) {
      // API absente → ne rien faire (compatibilité ascendante).
    }

    bulkBar.setBorder(new EmptyBorder(6, 12, 6, 12));
    bulkBar.setBackground(UiTokens.bgAlt());
    JLabel selectionLabel = new JLabel("Sélection");
    Font selectionFont = selectionLabel.getFont();
    if (selectionFont != null){
      selectionLabel.setFont(selectionFont.deriveFont(Font.BOLD));
    }
    bulkBar.add(selectionLabel);
    bulkBar.add(Box.createHorizontalStrut(6));
    contextWeekPill.setText(weekBadge.getText());
    contextWeekPill.setToolTipText("Période active");
    bulkBar.add(contextWeekPill);
    bulkBar.add(Box.createHorizontalStrut(14));
    JSeparator selectionSeparator = new JSeparator(SwingConstants.VERTICAL);
    selectionSeparator.setPreferredSize(new Dimension(1, 20));
    bulkBar.add(selectionSeparator);
    bulkBar.add(Box.createHorizontalStrut(14));
    bulkBar.add(selCountLabel);
    bulkBar.add(Box.createHorizontalStrut(12));
    JSeparator actionsSeparator = new JSeparator(SwingConstants.VERTICAL);
    actionsSeparator.setPreferredSize(new Dimension(1, 24));
    bulkBar.add(actionsSeparator);
    bulkBar.add(Box.createHorizontalStrut(12));
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
    bulkBar.add(Box.createHorizontalStrut(12));
    JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    badgesPanel.setOpaque(false);
    pendingBadge.setText("À deviser: 0");
    quotedBadge.setText("Devisé: 0");
    badgesPanel.add(pendingBadge);
    badgesPanel.add(quotedBadge);
    bulkBar.add(badgesPanel);
    bulkBar.add(Box.createHorizontalStrut(12));
    JLabel shortcutsLabel = new JLabel("Raccourcis : G (Devis) · P (PDF) · M (Email) · / (Recherche)");
    Font shortcutsFont = shortcutsLabel.getFont();
    if (shortcutsFont != null){
      shortcutsLabel.setFont(shortcutsFont.deriveFont(Font.PLAIN, 11f));
    }
    shortcutsLabel.setForeground(UiTokens.textMuted());
    bulkBar.add(shortcutsLabel);
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

    installKeyAndWheelShortcuts();
    putUndoRedoKeymap();
    installKeymap();

    // NOTE: on ne branche pas le renderer expérimental pour l'instant.
    // tryAttachTileRenderer(); // ← activable plus tard si besoin

    // ===== Double-clic & clic-droit : ouvrir intervention =====
    installBoardOpenHandlers(planningScroll);

    // Zoom global Ctrl+molette (non bloquant, n’intercepte rien sans Ctrl)
    installWheelZoom(planningScroll, board);
    planningScroll.getViewport().addChangeListener(e -> pushVisibleWindowToBoard());
    pushVisibleWindowToBoard();
  }

  private JComponent buildCalendarTab(){
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(calendarView.getComponent(), BorderLayout.CENTER);

    JPanel cardsWrapper = new JPanel(new BorderLayout());
    cardsWrapper.setOpaque(false);

    JPanel toggleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    toggleBar.setOpaque(false);
    toggleBar.add(cardsToggle);
    cardsToggle.setSelected(false);
    cardsToggle.addActionListener(e -> updateCardsVisibility());

    cardsContainer.setOpaque(false);
    cardsContainer.setBorder(new EmptyBorder(8, 12, 12, 12));

    cardsScroll.setBorder(BorderFactory.createEmptyBorder());
    cardsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    cardsScroll.getVerticalScrollBar().setUnitIncrement(18);
    cardsScroll.setOpaque(false);
    cardsScroll.getViewport().setOpaque(false);
    cardsScroll.setVisible(false);

    cardsWrapper.add(toggleBar, BorderLayout.NORTH);
    cardsWrapper.add(cardsScroll, BorderLayout.CENTER);

    wrapper.add(cardsWrapper, BorderLayout.SOUTH);
    return wrapper;
  }

  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    try {
      var settings = ServiceLocator.settings().getGeneral();
      Color accent = ThemeManager.parseColorSafe(settings != null ? settings.getBrandSecondaryHex() : null, new Color(0xF4511E));
      Color background = ThemeManager.lighten(accent, 0.9f);
      if (background != null){
        bar.setBackground(background);
      }
    } catch (RuntimeException ignore){
    }
    weekBadge.setOpaque(true);
    weekBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
    weekBadge.setBackground(new Color(0xE8, 0xF0, 0xFF));
    weekBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    Font badgeFont = weekBadge.getFont();
    if (badgeFont != null){
      weekBadge.setFont(badgeFont.deriveFont(Font.BOLD));
    }
    weekBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    weekBadge.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (SwingUtilities.isLeftMouseButton(e)){
          openWeekPicker();
        }
      }
    });
    JButton prev = new JButton("◀");
    JButton next = new JButton("▶");
    JButton today = new JButton("Aujourd'hui");
    JLabel zoomL = new JLabel("Zoom (slot):");
    zoomSlider.setMinimum(6);
    zoomSlider.setMaximum(24);
    zoomSlider.setValue(board.getSlotWidth());
    JLabel granL = new JLabel("Pas:");
    JComboBox<String> gran = new JComboBox<>(new String[]{"5 min","10 min","15 min","30 min","60 min"});
    gran.setSelectedItem(board.getSlotMinutes()+" min");
    JLabel densL = new JLabel("Densité:");
    JComboBox<String> density = new JComboBox<>(new String[]{"COMPACT","NORMAL","SPACIOUS"});
    compactToggle = new JToggleButton("Compact");
    modeToggle = new JToggleButton("Agenda"); // conservé en mémoire si utilisé ailleurs
    conflictsBtn = new JButton("Conflits (0)");
    JButton toAgenda = new JButton("↔ Agenda"); // ne sera pas ajouté à la barre
    JButton addI = new JButton("+ Intervention", IconRegistry.small("task"));
    simplePeriod = new JComboBox<>(new String[]{"Semaine","Mois"}); // on garde la logique, mais on n'affiche plus ce bloc
    LocalDate initialRef = board.getStartDate();
    if (initialRef == null){
      initialRef = LocalDate.now().with(DayOfWeek.MONDAY);
      board.setStartDate(initialRef);
    }
    agenda.setStartDate(initialRef);
    simpleRefDate = new JSpinner(new SpinnerDateModel(toDate(initialRef), null, null, Calendar.DAY_OF_MONTH));
    simpleRefDate.setEditor(new JSpinner.DateEditor(simpleRefDate, "dd/MM/yyyy"));
    JButton simplePrev = new JButton("◀");       // non ajoutés à la barre
    JButton simpleToday = new JButton("Aujourd'hui");
    JButton simpleNext = new JButton("▶");

    modeToggle.addActionListener(e -> {
      if (updatingModeToggle) return;
      switchMode(modeToggle.isSelected());
    });
    conflictsBtn.addActionListener(e -> openConflictsDialog());
    density.setSelectedItem(board.getDensity().name());
    compactToggle.setSelected(board.getDensity() == UiDensity.COMPACT);
    density.addActionListener(e -> {
      board.setDensity(UiDensity.fromString(String.valueOf(density.getSelectedItem())));
      compactToggle.setSelected(board.getDensity() == UiDensity.COMPACT);
      revalidate(); repaint();
    });
    compactToggle.setToolTipText("Basculer rapidement entre COMPACT et NORMAL");
    compactToggle.addActionListener(e -> {
      boolean compact = compactToggle.isSelected();
      board.setDensity(compact ? UiDensity.COMPACT : UiDensity.NORMAL);
      String target = board.getDensity().name();
      Object current = density.getSelectedItem();
      if (!Objects.equals(current, target)){
        density.setSelectedItem(target);
      }
      revalidate(); repaint();
    });

    prev.setToolTipText("Semaine précédente");
    next.setToolTipText("Semaine suivante");
    today.setToolTipText("Revenir à la semaine courante");
    prev.addActionListener(e -> shiftWeek(-1));
    next.addActionListener(e -> shiftWeek(1));
    today.addActionListener(e -> goToday());
    zoomSlider.addChangeListener(e -> applyZoom());
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

    bar.add(prev);
    bar.add(weekBadge);
    bar.add(next);
    bar.add(today);
    // On n’ajoute PAS le toggle Agenda ici
    bar.add(Box.createHorizontalStrut(16)); bar.add(zoomL); bar.add(zoomSlider);
    bar.add(Box.createHorizontalStrut(12)); bar.add(granL); bar.add(gran);
    bar.add(Box.createHorizontalStrut(12)); bar.add(densL); bar.add(density); bar.add(compactToggle);
    bar.add(Box.createHorizontalStrut(8)); bar.add(conflictsBtn);
    // Suppression de “↔ Agenda” et du bloc “Période calendrier …”
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
    weekBadge.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (SwingUtilities.isLeftMouseButton(e)){
          openWeekPicker();
        }
      }
    });
    buildDisplayMenu();
    syncWeekBadgeFromBoard();
    return bar;
  }

  /** Ouvre l’intervention par double-clic; offre aussi un menu contextuel “Ouvrir”. */
  @SuppressWarnings("unchecked")
  private void installBoardOpenHandlers(JScrollPane scroll){
    Objects.requireNonNull(scroll, "scroll");
    boolean delegated = false;
    try {
      var method = board.getClass().getMethod("setOnOpen", java.util.function.Consumer.class);
      method.invoke(board, (java.util.function.Consumer<Intervention>) this::openInterventionEditor);
      delegated = true;
    } catch (Exception ignore){
      // fallback plan ci-dessous
    }
    if (!delegated){
      board.addMouseListener(new MouseAdapter(){
        @Override public void mouseClicked(MouseEvent e){
          if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){
            Intervention hit = tryHitTestOnBoard(e);
            if (hit != null){
              openInterventionEditor(hit);
            } else {
              List<Intervention> sel = selectedInterventions();
              if (sel != null && !sel.isEmpty()){
                openInterventionEditor(sel.get(0));
              }
            }
          }
        }
      });
    }

    JPopupMenu menu = new JPopupMenu();
    JMenuItem open = new JMenuItem("Ouvrir l’intervention");
    open.addActionListener(ev -> {
      Point p = board.getMousePosition();
      Intervention hit = null;
      if (p != null){
        MouseEvent fake = new MouseEvent(board, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, p.x, p.y, 1, false);
        hit = tryHitTestOnBoard(fake);
      }
      if (hit == null){
        List<Intervention> sel = selectedInterventions();
        if (sel != null && !sel.isEmpty()){
          hit = sel.get(0);
        }
      }
      if (hit != null){
        openInterventionEditor(hit);
      }
    });
    menu.add(open);
    board.addMouseListener(new MouseAdapter(){
      @Override public void mousePressed(MouseEvent e){ maybeShow(e); }
      @Override public void mouseReleased(MouseEvent e){ maybeShow(e); }
      private void maybeShow(MouseEvent e){
        if (e.isPopupTrigger()){
          menu.show(board, e.getX(), e.getY());
        }
      }
    });
  }

  /** Essaie d’appeler un hit-test du board si disponible (par noms usuels), sinon null. */
  private Intervention tryHitTestOnBoard(MouseEvent e){
    if (e == null){
      return null;
    }
    try {
      Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), board);
      try {
        var m = board.getClass().getMethod("findInterventionAt", Point.class);
        Object res = m.invoke(board, p);
        if (res instanceof Intervention it){
          return it;
        }
      } catch (NoSuchMethodException ignore){
      }
      try {
        var m2 = board.getClass().getMethod("getInterventionAt", int.class, int.class);
        Object res2 = m2.invoke(board, p.x, p.y);
        if (res2 instanceof Intervention it){
          return it;
        }
      } catch (NoSuchMethodException ignore){
      }
      try {
        var m3 = board.getClass().getMethod("hitTest", Point.class);
        Object hit = m3.invoke(board, p);
        if (hit != null){
          try {
            var getIt = hit.getClass().getMethod("getIntervention");
            Object it = getIt.invoke(hit);
            if (it instanceof Intervention ii){
              return ii;
            }
          } catch (Exception ignore){
          }
        }
      } catch (NoSuchMethodException ignore){
      }
    } catch (Exception ignore){
    }
    return null;
  }

  private void tryAttachTileRenderer(){
    InterventionTileRenderer renderer = new InterventionTileRenderer();
    // API moderne : setCellRenderer(BiFunction<Intervention, Integer, JComponent>)
    try{
      var m = board.getClass().getMethod("setCellRenderer", java.util.function.BiFunction.class);
      java.util.function.BiFunction<Intervention, Integer, JComponent> fn =
          (it, width) -> renderer.render(it, false, width == null ? 200 : width);
      m.invoke(board, fn);
      return;
    }catch(Exception ignore){}
    // API alternative : setRenderer(… avec getComponent(Intervention, boolean, int))
    try{
      var m = board.getClass().getMethod("setRenderer", Object.class);
      Class<?> iface = m.getParameterTypes()[0];
      Object proxy = java.lang.reflect.Proxy.newProxyInstance(
          getClass().getClassLoader(), new Class[]{iface},
          (p, mm, args) -> {
            if ("getComponent".equals(mm.getName()) && args != null && args.length >= 3){
              Intervention it = (Intervention) args[0];
              boolean sel = Boolean.TRUE.equals(args[1]);
              int w = (args[2] instanceof Integer) ? (Integer) args[2] : 200;
              return renderer.render(it, sel, w);
            }
            return null;
          });
      m.invoke(board, proxy);
    }catch(Exception ignore){}
  }

  /** Ctrl+molette non bloquant : branché sur le scroll + board, ne consomme que si Ctrl. */
  private void installWheelZoom(JScrollPane scroll, JComponent content){
    final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    MouseWheelListener listener = e -> {
      if ((e.getModifiersEx() & mask) == 0){
        return;
      }
      if (e.getWheelRotation() < 0){
        zoomInStep();
      } else {
        zoomOutStep();
      }
      e.consume();
    };
    if (scroll != null){
      JViewport viewport = scroll.getViewport();
      if (viewport != null){
        viewport.addMouseWheelListener(listener);
      }
    }
    if (content != null){
      content.addMouseWheelListener(listener);
    }
  }

  private void pushVisibleWindowToBoard(){
    if (planningScroll == null){
      return;
    }
    Rectangle viewRect = planningScroll.getViewport().getViewRect();
    if (viewRect == null){
      board.setVisibleRowWindow(-1, -1);
      return;
    }
    List<Resource> resources = board.getResourcesList();
    if (resources == null || resources.isEmpty()){
      board.setVisibleRowWindow(-1, -1);
      return;
    }
    int y = 0;
    int start = 0;
    int end = resources.size();
    boolean startFound = false;
    int viewportBottom = viewRect.y + viewRect.height;
    for (int i = 0; i < resources.size(); i++){
      Resource resource = resources.get(i);
      int rowHeight = board.rowHeight(resource.getId());
      int nextY = y + rowHeight;
      if (!startFound && nextY >= viewRect.y){
        start = i;
        startFound = true;
      }
      if (y > viewportBottom){
        end = i;
        break;
      }
      y = nextY;
    }
    if (!startFound){
      start = Math.max(0, resources.size() - 1);
    }
    start = Math.max(0, start - 2);
    end = Math.min(resources.size(), end + 2);
    if (end <= start){
      end = Math.min(resources.size(), start + 1);
    }
    board.setVisibleRowWindow(start, end);
  }

  private void zoomInStep(){
    setZoomSliderBy(1);
  }

  private void zoomOutStep(){
    setZoomSliderBy(-1);
  }

  private void setZoomSliderBy(int delta){
    if (zoomSlider == null){
      return;
    }
    int min = zoomSlider.getMinimum();
    int max = zoomSlider.getMaximum();
    int value = Math.max(min, Math.min(max, zoomSlider.getValue() + delta));
    if (value != zoomSlider.getValue()){
      zoomSlider.setValue(value);
    }
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

  // =====================  Affichage (Zoom + Pas)  =====================
  private void buildDisplayMenu(){
    displayMenu.removeAll();
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("Zoom"), gc);
    gc.gridx = 1;
    for (var listener : zoomSlider.getChangeListeners()){
      zoomSlider.removeChangeListener(listener);
    }
    zoomSlider.setMajorTickSpacing(25);
    zoomSlider.setPaintTicks(true);
    zoomSlider.addChangeListener(e -> {
      if (!zoomSlider.getValueIsAdjusting()){
        applyZoom();
      }
    });
    panel.add(zoomSlider, gc);
    gc.gridx = 0;
    gc.gridy = 1;
    panel.add(new JLabel("Pas (minutes)"), gc);
    gc.gridx = 1;
    for (var listener : slotCombo.getActionListeners()){
      slotCombo.removeActionListener(listener);
    }
    slotCombo.addActionListener(e -> applySlot());
    panel.add(slotCombo, gc);
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(panel, BorderLayout.CENTER);
    displayMenu.add(wrapper);
    syncDisplayControls();
  }

  private void syncDisplayControls(){
    int width = board.getSlotWidth();
    int sliderValue = slotWidthToSlider(width);
    if (zoomSlider.getValue() != sliderValue){
      zoomSlider.setValue(sliderValue);
    }
    Integer minutes = board.getSlotMinutes();
    if (minutes != null){
      boolean found = false;
      for (int i = 0; i < slotCombo.getItemCount(); i++){
        if (Objects.equals(slotCombo.getItemAt(i), minutes)){
          found = true;
          break;
        }
      }
      if (!found){
        slotCombo.addItem(minutes);
      }
      if (!Objects.equals(slotCombo.getSelectedItem(), minutes)){
        slotCombo.setSelectedItem(minutes);
      }
    }
  }

  private void applyZoom(){
    int width = sliderToSlotWidth(zoomSlider.getValue());
    // Préférence : API setSlotWidth (zone centrale). Fallback : setZoom si c'est ta version.
    boolean applied = false;
    try {
      var method = board.getClass().getMethod("setSlotWidth", int.class);
      method.invoke(board, width);
      applied = true;
    } catch (Exception ignore) {
      try {
        var fallback = board.getClass().getMethod("setZoom", int.class);
        fallback.invoke(board, width);
        applied = true;
      } catch (Exception ignore2) {
        // no-op
      }
    }
    // Largeur de journée côté vue agenda (si utilisée)
    agenda.setDayWidth(width * 10);
    // Forcer un relayout/repaint du board et des entêtes
    if (applied){
      board.revalidate();
      board.repaint();
    }
    pushVisibleWindowToBoard();
    revalidate();
    repaint();
  }

  private int sliderToSlotWidth(int sliderValue){
    int width = (int) Math.round(12 * sliderValue / 100.0);
    return Math.max(6, Math.min(24, width));
  }

  private int slotWidthToSlider(int width){
    int sliderValue = (int) Math.round(width / 12.0 * 100);
    return Math.max(zoomSlider.getMinimum(), Math.min(zoomSlider.getMaximum(), sliderValue));
  }

  private void applySlot(){
    Object selected = slotCombo.getSelectedItem();
    if (selected instanceof Integer minutes){
      board.setSlotMinutes(minutes);
      agenda.setSnapMinutes(minutes);
      revalidate();
      repaint();
    }
  }

  // =====================  Pickeur de semaine  =====================
  private void openWeekPicker(){
    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Aller à la semaine", Dialog.ModalityType.APPLICATION_MODAL);
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    int currentYear = pivotMonday != null ? pivotMonday.get(WeekFields.ISO.weekBasedYear()) : LocalDate.now().get(WeekFields.ISO.weekBasedYear());
    int currentWeek = pivotMonday != null ? pivotMonday.get(WeekFields.ISO.weekOfWeekBasedYear()) : LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("Année"), gc);
    gc.gridx = 1;
    JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(currentYear, 2000, 2100, 1));
    panel.add(yearSpinner, gc);
    gc.gridx = 0;
    gc.gridy = 1;
    panel.add(new JLabel("Semaine (ISO)"), gc);
    gc.gridx = 1;
    JSpinner weekSpinner = new JSpinner(new SpinnerNumberModel(currentWeek, 1, 53, 1));
    panel.add(weekSpinner, gc);
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton ok = new JButton("Aller");
    JButton cancel = new JButton("Annuler");
    buttons.add(ok);
    buttons.add(cancel);
    gc.gridx = 0;
    gc.gridy = 2;
    gc.gridwidth = 2;
    gc.fill = GridBagConstraints.NONE;
    panel.add(buttons, gc);
    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.getRootPane().setDefaultButton(ok);
    ok.addActionListener(e -> {
      int targetYear = (int) yearSpinner.getValue();
      int targetWeek = (int) weekSpinner.getValue();
      applyPivotMonday(isoWeekMonday(targetYear, targetWeek));
      dialog.dispose();
    });
    cancel.addActionListener(e -> dialog.dispose());
    dialog.setVisible(true);
  }

  private static LocalDate isoWeekMonday(int year, int week){
    WeekFields wf = WeekFields.ISO;
    LocalDate january4 = LocalDate.of(year, 1, 4);
    LocalDate firstWeekMonday = january4.with(wf.dayOfWeek(), 1);
    int normalizedWeek = Math.max(1, Math.min(week, 53));
    return firstWeekMonday.plusWeeks(normalizedWeek - 1L);
  }

  private void goToday(){
    applyPivotMonday(LocalDate.now());
  }

  private void shiftWeek(int direction){
    LocalDate start = board.getStartDate();
    if (start == null){
      start = LocalDate.now().with(DayOfWeek.MONDAY);
    }
    applyPivotMonday(start.plusWeeks(direction));
  }

  private void applyPivotMonday(LocalDate reference){
    LocalDate monday = reference != null
        ? reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        : LocalDate.now().with(DayOfWeek.MONDAY);
    pivotMonday = monday;
    board.setStartDate(monday);
    agenda.setStartDate(board.getStartDate());
    updateWeekBadge();
    updateSimpleReference(monday);
  }

  private void updateWeekBadge(){
    LocalDate monday = pivotMonday != null ? pivotMonday : LocalDate.now().with(DayOfWeek.MONDAY);
    int isoWeek = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
    int year = monday.get(WeekFields.ISO.weekBasedYear());
    String label = String.format("Semaine %d · %d-W%02d", isoWeek, year, isoWeek);
    weekBadge.setText(label);
    LocalDate sunday = monday.plusDays(6);
    weekBadge.setToolTipText("Du " + SIMPLE_DAY_FORMAT.format(monday) + " au " + SIMPLE_DAY_FORMAT.format(sunday));
    contextWeekPill.setText(label);
    contextWeekPill.setToolTipText(weekBadge.getToolTipText());
  }

  private void syncWeekBadgeFromBoard(){
    LocalDate start = board.getStartDate();
    if (start != null){
      pivotMonday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    } else {
      pivotMonday = LocalDate.now().with(DayOfWeek.MONDAY);
    }
    updateWeekBadge();
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
      pendingBadge.setText("À deviser: " + pending);
      quotedBadge.setText("Devisé: " + quoted);
    } else {
      pendingBadge.setText("À deviser: 0");
      quotedBadge.setText("Devisé: 0");
    }
    revalidate();
    repaint();
  }

  private List<Intervention> selectedInterventions(){
    return currentSelection;
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

  private static String firstNonBlank(String... values){
    if (values == null){
      return "";
    }
    for (String value : values){
      if (value != null && !value.isBlank()){
        return value;
      }
    }
    return "";
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
      syncWeekBadgeFromBoard();
      return;
    }
    board.reload();
    agenda.reload();
    pushVisibleWindowToBoard();
    refreshSimpleViews(planning);
    syncWeekBadgeFromBoard();
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
        list = filterByAgency(fetched);
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
    reloadCardFilters();
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
    updateCardDataset(dataset);
    if (!kanbanHasError){
      kanbanView.setData(dataset);
      applySearch();
    }
    cardsData = dataset == null ? List.of() : List.copyOf(dataset);
    renderCards(cardsData);
  }

  private void updateCardsVisibility(){
    boolean show = cardsToggle.isSelected();
    cardsScroll.setVisible(show);
    cardsScroll.revalidate();
    cardsScroll.repaint();
    if (show){
      renderCards(cardsData);
    }
  }

  private void renderCards(List<Intervention> list){
    cardsContainer.removeAll();
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.insets = new Insets(0, 0, 8, 0);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1.0;

    if (list == null || list.isEmpty()){
      JLabel empty = new JLabel("Aucune intervention", JLabel.CENTER);
      empty.setForeground(UiTokens.textMuted());
      JPanel wrapper = new JPanel(new BorderLayout());
      wrapper.setOpaque(false);
      wrapper.add(empty, BorderLayout.CENTER);
      cardsContainer.add(wrapper, gc);
      gc.gridy++;
    } else {
      for (Intervention intervention : list){
        if (intervention == null){
          continue;
        }
        InterventionTilePanel tile = new InterventionTilePanel(intervention, new InterventionTilePanel.Listener(){
          @Override public void onOpen(Intervention value){
            showInterventionSummary(value);
          }

          @Override public void onEdit(Intervention value){
            openInterventionEditor(value);
          }

          @Override public void onMarkDone(Intervention value){
            markInterventionDone(value);
          }
        });
        tile.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsContainer.add(tile, gc);
        gc.gridy++;
      }
    }

    GridBagConstraints filler = new GridBagConstraints();
    filler.gridx = 0;
    filler.gridy = gc.gridy;
    filler.weighty = 1.0;
    filler.fill = GridBagConstraints.VERTICAL;
    cardsContainer.add(Box.createVerticalGlue(), filler);

    cardsContainer.revalidate();
    cardsContainer.repaint();
  }

  private void showInterventionSummary(Intervention intervention){
    if (intervention == null){
      return;
    }
    String title = intervention.getLabel();
    if (title == null || title.isBlank()){
      title = "Intervention";
    }
    String client = intervention.getClientName();
    if (client == null || client.isBlank()){
      client = "Client inconnu";
    }
    String status = intervention.getStatus();
    if (status == null || status.isBlank()){
      status = "Planifiée";
    }
    String address = intervention.getAddress();
    if (address == null || address.isBlank()){
      address = intervention.getSiteLabel();
    }
    if (address == null || address.isBlank()){
      address = "—";
    }
    String typeLabel = "—";
    var type = intervention.getType();
    if (type != null){
      String label = type.getLabel();
      if (label != null && !label.isBlank()){
        typeLabel = label;
      } else {
        String code = type.getCode();
        if (code != null && !code.isBlank()){
          typeLabel = code;
        }
      }
    }
    String range = formatCardRange(intervention);
    String resources = formatResourcesPlain(intervention.getResources());

    String html = "<html><b>" + StringEscapeUtils.escapeHtml4(title) + "</b><br/>"
        + "<span style='color:#555555'>" + StringEscapeUtils.escapeHtml4(client) + "</span><br/>"
        + StringEscapeUtils.escapeHtml4(range) + "<br/>"
        + "Type : " + StringEscapeUtils.escapeHtml4(typeLabel) + "<br/>"
        + "Statut : " + StringEscapeUtils.escapeHtml4(status) + "<br/>"
        + "Adresse : " + StringEscapeUtils.escapeHtml4(address) + "<br/>"
        + "Ressources : " + StringEscapeUtils.escapeHtml4(resources)
        + "</html>";
    JOptionPane.showMessageDialog(this, html, "Intervention", JOptionPane.INFORMATION_MESSAGE);
  }

  private String formatCardRange(Intervention intervention){
    if (intervention == null){
      return "—";
    }
    LocalDateTime start = intervention.getDateHeureDebut();
    if (start == null){
      start = intervention.getStartDateTime();
    }
    LocalDateTime end = intervention.getDateHeureFin();
    if (end == null){
      end = intervention.getEndDateTime();
    }
    if (start == null){
      return "—";
    }
    if (end == null){
      return SIMPLE_DAY_TIME_FORMAT.format(start);
    }
    return SIMPLE_DAY_TIME_FORMAT.format(start) + " → " + SIMPLE_DAY_TIME_FORMAT.format(end);
  }

  private String formatResourcesPlain(List<ResourceRef> refs){
    if (refs == null || refs.isEmpty()){
      return "Aucune";
    }
    List<String> names = new ArrayList<>();
    for (ResourceRef ref : refs){
      if (ref == null){
        continue;
      }
      String name = ref.getName();
      if (name == null || name.isBlank()){
        names.add("Ressource");
      } else {
        names.add(name);
      }
    }
    return names.isEmpty() ? "Aucune" : String.join(", ", names);
  }

  private void markInterventionDone(Intervention intervention){
    if (intervention == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible");
      return;
    }
    String previous = intervention.getStatus();
    try {
      intervention.setStatus("Terminé");
      planning.saveIntervention(intervention);
      Toasts.success(this, "Intervention marquée comme terminée");
      refreshPlanning();
    } catch (Exception ex){
      intervention.setStatus(previous);
      String message = ex.getMessage();
      if (message == null || message.isBlank()){
        Toasts.error(this, "Impossible de marquer l'intervention comme terminée");
      } else {
        Toasts.error(this, message);
      }
    }
  }

  private JPanel buildCardsTab(){
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    filterBar.setOpaque(false);
    cardsStatusFilter = new JComboBox<>(new String[]{"Tous", "Planifié", "En cours", "Terminé", "Annulé", "Brouillon"});
    cardsToQuoteFilter = new JCheckBox("À deviser");
    cardsResourceTypeFilter = new JComboBox<>(new String[]{"Tous"});
    cardsAgencyFilter = new JComboBox<>(new String[]{"Toutes"});
    cardsDensityToggle = new JToggleButton("Mode compact");
    cardsDensityToggle.setToolTipText("Réduire l'espacement des cartes");

    filterBar.add(new JLabel("Statut"));
    filterBar.add(cardsStatusFilter);
    filterBar.add(cardsToQuoteFilter);
    filterBar.add(new JLabel("Type res."));
    filterBar.add(cardsResourceTypeFilter);
    filterBar.add(new JLabel("Agence"));
    filterBar.add(cardsAgencyFilter);
    filterBar.add(cardsDensityToggle);

    cardsContainer = new JPanel();
    cardsContainer.setOpaque(false);
    cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
    cardsContainer.setBorder(new EmptyBorder(12, 12, 12, 12));

    JScrollPane scroll = new JScrollPane(cardsContainer,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(24);
    scroll.getViewport().setBackground(Color.WHITE);
    scroll.setBorder(null);

    panel.add(filterBar, BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);

    cardsStatusFilter.addActionListener(e -> applyCardFilters());
    cardsToQuoteFilter.addActionListener(e -> applyCardFilters());
    cardsResourceTypeFilter.addActionListener(e -> applyCardFilters());
    cardsAgencyFilter.addActionListener(e -> applyCardFilters());
    cardsDensityToggle.addActionListener(e -> applyCardFilters());

    return panel;
  }

  private void updateCardDataset(List<Intervention> dataset){
    cardItems = dataset == null ? List.of() : List.copyOf(dataset);
    applyCardFilters();
  }

  private void applyCardFilters(){
    if (cardsContainer == null){
      return;
    }
    List<Intervention> base = cardItems != null ? cardItems : List.of();
    String status = cardsStatusFilter != null ? String.valueOf(cardsStatusFilter.getSelectedItem()) : "Tous";
    boolean onlyToQuote = cardsToQuoteFilter != null && cardsToQuoteFilter.isSelected();
    String resourceType = cardsResourceTypeFilter != null ? String.valueOf(cardsResourceTypeFilter.getSelectedItem()) : "Tous";
    String agency = cardsAgencyFilter != null ? String.valueOf(cardsAgencyFilter.getSelectedItem()) : "Toutes";
    boolean compact = cardsDensityToggle != null && cardsDensityToggle.isSelected();

    List<Intervention> filtered = new ArrayList<>();
    for (Intervention intervention : base){
      if (intervention == null){
        continue;
      }
      if (!"Tous".equals(status) && !matchesStatus(intervention, status)){
        continue;
      }
      if (onlyToQuote && intervention.hasQuote()){
        continue;
      }
      if (!"Tous".equals(resourceType) && !resourceType.isBlank() && !hasResourceType(intervention, resourceType)){
        continue;
      }
      if (!"Toutes".equals(agency) && !matchesAgency(intervention, agency)){
        continue;
      }
      filtered.add(intervention);
    }
    renderCards(filtered, compact);
  }

  private boolean matchesStatus(Intervention intervention, String filter){
    String status = intervention.getStatus();
    if (status == null){
      return "Planifié".equals(filter) || "Tous".equals(filter);
    }
    String lower = status.toLowerCase(Locale.ROOT);
    return switch (filter) {
      case "Planifié" -> lower.contains("plan");
      case "En cours" -> lower.contains("cours") || lower.contains("progress");
      case "Terminé" -> lower.contains("term") || lower.contains("done");
      case "Annulé" -> lower.contains("annul") || lower.contains("cancel");
      case "Brouillon" -> lower.contains("brou") || lower.contains("draft");
      default -> true;
    };
  }

  private boolean matchesAgency(Intervention intervention, String filter){
    if (filter == null || filter.isBlank()){
      return true;
    }
    String agencyName = intervention.getAgency();
    if (agencyName != null && agencyName.equalsIgnoreCase(filter)){
      return true;
    }
    String agencyId = intervention.getAgencyId();
    return agencyId != null && agencyId.equalsIgnoreCase(filter);
  }

  private boolean hasResourceType(Intervention intervention, String filter){
    if (intervention == null || filter == null || filter.isBlank()){
      return false;
    }
    String expected = filter.trim().toLowerCase(Locale.ROOT);
    List<ResourceRef> refs = intervention.getResources();
    if (refs == null || refs.isEmpty()){
      return false;
    }
    for (ResourceRef ref : refs){
      if (ref == null){
        continue;
      }
      Resource resource = ref.getId() != null ? resourceCatalog.get(ref.getId()) : null;
      if (resource != null){
        String label = resource.getTypeLabel();
        if (label != null && label.trim().equalsIgnoreCase(filter)){
          return true;
        }
        String code = resource.getTypeCode();
        if (code != null && code.trim().equalsIgnoreCase(expected)){
          return true;
        }
      }
      String icon = ref.getIcon();
      if (icon != null && icon.trim().equalsIgnoreCase(expected)){
        return true;
      }
    }
    return false;
  }

  private void renderCards(List<Intervention> items, boolean compact){
    cardsContainer.removeAll();
    if (items == null || items.isEmpty()){
      JLabel empty = new JLabel("Aucune intervention ne correspond aux filtres.");
      empty.setForeground(UiTokens.textMuted());
      empty.setBorder(new EmptyBorder(20, 12, 20, 12));
      cardsContainer.add(empty);
    } else {
      boolean first = true;
      for (Intervention intervention : items){
        if (!first){
          cardsContainer.add(Box.createVerticalStrut(compact ? 6 : 12));
        }
        first = false;
        Intervention current = intervention;
        InterventionTilePanel tile = new InterventionTilePanel(current, new InterventionTilePanel.Listener(){
          @Override public void onOpen(Intervention it){ openInterventionEditor(it); }
          @Override public void onEdit(Intervention it){ openInterventionEditor(it); }
          @Override public void onMarkDone(Intervention it){ markInterventionDone(it); }
          @Override public void onTimeAdjust(Intervention it, boolean start, int minutesDelta){
            adjustInterventionTime(it, start, minutesDelta);
          }
        });
        tile.setCompact(compact);
        tile.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, tile.getPreferredSize().height));
        tile.setTransferHandler(new TransferHandler(){
          @Override public int getSourceActions(JComponent c){
            return MOVE;
          }
          @Override protected Transferable createTransferable(JComponent c){
            if (current == null || current.getId() == null){
              return null;
            }
            return new StringSelection(current.getId().toString());
          }
        });
        tile.addMouseListener(new MouseAdapter(){
          @Override public void mousePressed(MouseEvent e){
            JComponent src = (JComponent) e.getSource();
            TransferHandler handler = src.getTransferHandler();
            if (handler != null){
              handler.exportAsDrag(src, e, TransferHandler.MOVE);
            }
          }
        });
        cardsContainer.add(tile);
      }
    }
    cardsContainer.revalidate();
    cardsContainer.repaint();
  }

  private void reloadCardFilters(){
    if (cardsResourceTypeFilter != null){
      String selection = String.valueOf(cardsResourceTypeFilter.getSelectedItem());
      List<String> options = new ArrayList<>();
      options.add("Tous");
      try {
        var gateway = ServiceLocator.resources();
        if (gateway != null){
          for (var type : gateway.listTypes()){
            if (type == null){
              continue;
            }
            String label = firstNonBlank(type.getName(), type.getLabel(), type.getCode());
            if (!label.isBlank() && !options.contains(label)){
              options.add(label);
            }
          }
        }
      } catch (Exception ignore){
      }
      cardsResourceTypeFilter.setModel(new DefaultComboBoxModel<>(options.toArray(new String[0])));
      if (selection != null && options.contains(selection)){
        cardsResourceTypeFilter.setSelectedItem(selection);
      }
    }
    if (cardsAgencyFilter != null){
      String selection = String.valueOf(cardsAgencyFilter.getSelectedItem());
      Set<String> agencies = new LinkedHashSet<>();
      agencies.add("Toutes");
      if (allInterventions != null){
        for (Intervention intervention : allInterventions){
          if (intervention == null){
            continue;
          }
          String label = firstNonBlank(intervention.getAgency(), intervention.getAgencyId());
          if (!label.isBlank()){
            agencies.add(label);
          }
        }
      }
      cardsAgencyFilter.setModel(new DefaultComboBoxModel<>(agencies.toArray(new String[0])));
      if (selection != null && agencies.contains(selection)){
        cardsAgencyFilter.setSelectedItem(selection);
      }
    }
    loadResourceCatalog();
    applyCardFilters();
  }

  private void loadResourceCatalog(){
    resourceCatalog.clear();
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      return;
    }
    try {
      List<Resource> resources = planning.listResources();
      if (resources != null){
        for (Resource resource : resources){
          if (resource != null && resource.getId() != null){
            resourceCatalog.put(resource.getId(), resource);
          }
        }
      }
    } catch (Exception ignore){
    }
  }

  private void installCardDropTarget(){
    try {
      new DropTarget(board, new DropTargetAdapter(){
        @Override public void drop(DropTargetDropEvent dtde){
          if (!dtde.isDataFlavorSupported(DataFlavor.stringFlavor)){
            dtde.rejectDrop();
            return;
          }
          try {
            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            Transferable transferable = dtde.getTransferable();
            String id = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            Point location = dtde.getLocation();
            LocalDateTime when = boardTimeAt(location);
            if (id != null && when != null){
              moveInterventionStart(id, when);
              dtde.dropComplete(true);
            } else {
              dtde.dropComplete(false);
            }
          } catch (Exception ex){
            dtde.dropComplete(false);
          }
        }
      });
    } catch (Exception ignore){
    }
  }

  private LocalDateTime boardTimeAt(Point point){
    if (point == null){
      return null;
    }
    int slotWidth = Math.max(1, board.getSlotWidth());
    int slotMinutes = Math.max(1, board.getSlotMinutes());
    int slotsPerDay = Math.max(1, board.getSlotsPerDay());
    int x = Math.max(0, point.x);
    int slot = x / slotWidth;
    int dayIndex = slot / slotsPerDay;
    int slotInDay = slot % slotsPerDay;
    LocalDate startDate = board.getStartDate();
    if (startDate == null){
      startDate = LocalDate.now().with(DayOfWeek.MONDAY);
    }
    LocalDate day = startDate.plusDays(dayIndex);
    int minutes = slotInDay * slotMinutes;
    return day.atStartOfDay().plusMinutes(minutes);
  }

  private void moveInterventionStart(String id, LocalDateTime newStart){
    if (id == null || id.isBlank() || newStart == null){
      return;
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
    } catch (IllegalArgumentException ex){
      return;
    }
    Intervention target = null;
    for (Intervention intervention : allInterventions){
      if (intervention != null && uuid.equals(intervention.getId())){
        target = intervention;
        break;
      }
    }
    if (target == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible.");
      return;
    }
    try {
      LocalDateTime oldStart = target.getDateHeureDebut();
      LocalDateTime oldEnd = target.getDateHeureFin();
      long duration = 60;
      if (oldStart != null && oldEnd != null){
        duration = Duration.between(oldStart, oldEnd).toMinutes();
        if (duration <= 0){
          duration = 15;
        }
      }
      LocalDateTime newEnd = newStart.plusMinutes(duration);
      target.setDateHeureDebut(newStart);
      target.setDateHeureFin(newEnd);
      planning.saveIntervention(target);
      Toasts.success(this, "Intervention déplacée à " + newStart.format(SIMPLE_DAY_TIME_FORMAT));
      refreshPlanning();
    } catch (Exception ex){
      Toasts.error(this, "Déplacement impossible : " + ex.getMessage());
    }
  }

  private void adjustInterventionTime(Intervention intervention, boolean start, int minutesDelta){
    if (intervention == null || minutesDelta == 0){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible.");
      return;
    }
    try {
      if (start){
        LocalDateTime base = intervention.getDateHeureDebut();
        if (base == null){
          base = intervention.getStartDateTime();
        }
        LocalDateTime updated = base.plusMinutes(minutesDelta);
        intervention.setDateHeureDebut(updated);
        LocalDateTime end = intervention.getDateHeureFin();
        if (end != null && !end.isAfter(updated)){
          intervention.setDateHeureFin(updated.plusMinutes(15));
        }
      } else {
        LocalDateTime base = intervention.getDateHeureFin();
        if (base == null){
          base = intervention.getEndDateTime();
        }
        LocalDateTime updated = base.plusMinutes(minutesDelta);
        LocalDateTime startDt = intervention.getDateHeureDebut();
        if (startDt != null && !updated.isAfter(startDt)){
          updated = startDt.plusMinutes(15);
        }
        intervention.setDateHeureFin(updated);
      }
      planning.saveIntervention(intervention);
      String delta = minutesDelta > 0 ? "+" + minutesDelta : String.valueOf(minutesDelta);
      Toasts.success(this, (start ? "Début" : "Fin") + " ajusté(e) de " + delta + " min");
      refreshPlanning();
    } catch (Exception ex){
      Toasts.error(this, "Ajustement impossible : " + ex.getMessage());
    }
  }

  private void markInterventionDone(Intervention intervention){
    if (intervention == null){
      return;
    }
    PlanningService planning = ServiceFactory.planning();
    if (planning == null){
      Toasts.error(this, "Service planning indisponible.");
      return;
    }
    try {
      intervention.setStatus("DONE");
      planning.saveIntervention(intervention);
      Toasts.success(this, "Intervention marquée comme terminée");
      refreshPlanning();
    } catch (Exception ex){
      Toasts.error(this, "Impossible de mettre à jour l'intervention : " + ex.getMessage());
    }
  }

  private void installKeyAndWheelShortcuts(){
    int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap actionMap = getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask), "planning-zoom-in");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, mask), "planning-zoom-in");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask), "planning-zoom-in");
    actionMap.put("planning-zoom-in", new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        zoomInStep();
      }
    });
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask), "planning-zoom-out");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask), "planning-zoom-out");
    actionMap.put("planning-zoom-out", new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        zoomOutStep();
      }
    });
    // Important : ne JAMAIS consommer la molette ici → le scroll reste natif
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, mask), "planning-go-week");
    actionMap.put("planning-go-week", new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        openWeekPicker();
      }
    });
    // pas de listener bloquant ici; voir installWheelZoom()
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
    maximizeDialog(dialog);
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
    maximizeDialog(dialog);
    dialog.setVisible(true);
  }

  private void maximizeDialog(InterventionDialog dialog){
    if (dialog == null){
      return;
    }
    Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    if (bounds != null){
      dialog.setBounds(bounds);
    }
    dialog.setResizable(true);
  }

  private void putUndoRedoKeymap(){
    int WHEN = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
    getInputMap(WHEN).put(KeyStroke.getKeyStroke("control Z"), "undo");
    getInputMap(WHEN).put(KeyStroke.getKeyStroke("control Y"), "redo");
    getActionMap().put("undo", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ CommandBus.get().undo(); refreshPlanning(); }});
    getActionMap().put("redo", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ CommandBus.get().redo(); refreshPlanning(); }});
  }
}
