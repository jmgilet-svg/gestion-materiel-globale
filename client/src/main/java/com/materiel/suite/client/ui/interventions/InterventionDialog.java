package com.materiel.suite.client.ui.interventions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.NumberFormatter;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.events.AppEventBus;
import com.materiel.suite.client.events.SettingsEvents;
import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionTemplate;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.TimelineEvent;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.ClientService;
import com.materiel.suite.client.service.InterventionTypeService;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.SalesService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplateService;
import com.materiel.suite.client.settings.GeneralSettings;
import com.materiel.suite.client.ui.common.Accessible;
import com.materiel.suite.client.ui.common.KeymapUtil;
import com.materiel.suite.client.ui.common.OverridableCellRenderers;
import com.materiel.suite.client.ui.common.ResourceChipsPanel;
import com.materiel.suite.client.ui.common.TableUtils;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.util.Money;

/** Fenêtre avancée pour créer ou modifier une intervention. */
public class InterventionDialog extends JDialog {
  private final PlanningService planningService;
  private final ClientService clientService;
  private final InterventionTypeService typeService;
  private final TemplateService templateService;
  private final List<InterventionType> availableTypes = new ArrayList<>();
  private final List<InterventionTemplate> availableTemplates = new ArrayList<>();

  private final JTextField titleField = new JTextField();
  private final JComboBox<InterventionType> typeCombo = new JComboBox<>();
  private final JComboBox<Client> clientCombo = new JComboBox<>();
  private final JTextField addressField = new JTextField();
  private final JComboBox<InterventionTemplate> templateCombo = new JComboBox<>();
  private final JSpinner startSpinner = new JSpinner(new SpinnerDateModel());
  private final JSpinner endSpinner = new JSpinner(new SpinnerDateModel());
  private final JTextField durationField = new JTextField(6);
  private final JTextArea descriptionArea = new JTextArea(4, 30);
  private final JTextArea internalNoteArea = new JTextArea(3, 30);
  private final JTextArea closingNoteArea = new JTextArea(3, 30);
  private final JTextField signatureByField = new JTextField(18);
  private final JSpinner signatureAtSpinner = new JSpinner(new SpinnerDateModel());
  private final JLabel signaturePreview = new JLabel();
  private final ResourcePickerPanel resourcePicker;
  private final ContactPickerPanel contactPicker = new ContactPickerPanel();
  private final BillingTableModel billingModel = new BillingTableModel();
  private final JTable billingTable = new JTable(billingModel);
  private final JLabel totalHtLabel = new JLabel();
  private final JLabel totalTvaLabel = new JLabel();
  private final JLabel totalTtcLabel = new JLabel();
  private final JLabel quoteStatusLabel = new JLabel("Aucun devis généré");
  private final StepBar workflowStepBar = new StepBar();
  private final JTabbedPane tabs = new JTabbedPane();
  private enum WorkflowStage { INTERVENTION, DEVIS, FACTURATION }
  private WorkflowStage currentStage = WorkflowStage.INTERVENTION;
  private final JButton prevStageButton = new JButton("← Précédent");
  private final JButton nextStageButton = new JButton("Suivant →");
  private final JButton stageGenerateQuoteButton = new JButton("Générer le devis");
  private final JButton stageGenerateInvoiceButton = new JButton("Générer la facture");
  private boolean ignoreTabChange;
  private int lastAccessibleTabIndex;
  private final DefaultListModel<TimelineEvent> historyModel = new DefaultListModel<>();
  private final JList<TimelineEvent> historyList = new JList<>(historyModel);
  private final JTextArea historyInput = new JTextArea(3, 20);
  private final JButton historySend = new JButton("Ajouter", IconRegistry.small("plus"));
  private final JLabel quoteSummaryLabel = new JLabel("Aucun devis généré");
  private final JButton saveButton = new JButton("Enregistrer", IconRegistry.small("success"));
  private final JButton applyTemplateButton = new JButton("Appliquer", IconRegistry.small("file-plus"));
  private final JButton regenerateBillingButton = new JButton("Depuis ressources", IconRegistry.small("refresh"));
  private final JButton addBillingLineButton = new JButton("Ajouter ligne", IconRegistry.small("plus"));
  private final JButton removeBillingLineButton = new JButton("Supprimer", IconRegistry.small("trash"));
  private final JButton generateQuoteButton = new JButton("Générer le devis", IconRegistry.small("file"));
  private final JButton openQuoteButton = new JButton("Ouvrir le devis");
  private final JButton importSignatureButton = new JButton("Importer PNG…");
  private final JButton clearSignatureButton = new JButton("Effacer");
  private final JButton fullscreenButton = new JButton("", IconRegistry.small("maximize"));
  private final ResourceChipsPanel billingResourceChips = new ResourceChipsPanel();
  private final JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
  private Rectangle previousBounds;
  private final boolean readOnly;
  private volatile boolean dirty;
  private volatile long lastEditTs;
  private final Timer autosaveTimer = new Timer(30_000, e -> tryAutosave());
  private final JLabel autosaveLabel = new JLabel(" ");
  private final Deque<List<BillingLine>> undoStack = new ArrayDeque<>();
  private final Deque<List<BillingLine>> redoStack = new ArrayDeque<>();
  private static final int MAX_HISTORY = 20;
  private List<BillingLine> lastLinesSnapshot = new ArrayList<>();
  private boolean skipNextTableSnapshot;
  private boolean suppressDirtyEvents;
  private boolean suppressDurationSync;
  private AutoCloseable settingsSubscription;

  private Intervention current;
  private boolean saved;
  private String signatureBase64;
  private Consumer<Intervention> onSaveCallback;
  private Component parentComponent;
  private Runnable closeHandler;
  private final AtomicLong conflictRefreshSeq = new AtomicLong();

  public InterventionDialog(Window owner,
                            PlanningService planningService,
                            ClientService clientService,
                            InterventionTypeService typeService,
                            TemplateService templateService){
    super(owner, "Intervention", ModalityType.APPLICATION_MODAL);
    setContentPane(contentPanel);
    this.planningService = planningService;
    this.clientService = clientService;
    this.typeService = typeService;
    this.templateService = templateService;
    this.resourcePicker = new ResourcePickerPanel(planningService);
    this.readOnly = !AccessControl.canEditInterventions();
    this.parentComponent = this;
    this.closeHandler = this::dispose;
    this.resourcePicker.setSelectionListener(this::onResourceSelectionChanged);
    this.contactPicker.setSelectionListener(() -> {
      refreshWorkflowState();
      markEdited();
    });
    templateCombo.setRenderer(new DefaultListCellRenderer(){
      @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof InterventionTemplate template){
          label.setText(template.getName());
        } else {
          label.setText("(Aucun)");
        }
        return label;
      }
    });
    saveButton.addActionListener(e -> onSave());
    applyTemplateButton.addActionListener(e -> applySelectedTemplate());
    regenerateBillingButton.addActionListener(e -> regenerateBillingFromResources());
    addBillingLineButton.addActionListener(e -> addManualBillingLine());
    removeBillingLineButton.addActionListener(e -> removeSelectedBillingLine());
    generateQuoteButton.addActionListener(e -> generateQuoteFromPrebilling());
    openQuoteButton.addActionListener(e -> openQuotePreview());
    importSignatureButton.addActionListener(e -> importSignature());
    clearSignatureButton.addActionListener(e -> clearSignature());
    prevStageButton.setFocusable(false);
    nextStageButton.setFocusable(false);
    prevStageButton.addActionListener(e -> goToPreviousStage());
    nextStageButton.addActionListener(e -> goToNextStage());
    stageGenerateQuoteButton.addActionListener(e -> actionGenerateQuote());
    stageGenerateInvoiceButton.addActionListener(e -> actionGenerateInvoice());
    stageGenerateQuoteButton.setVisible(false);
    stageGenerateInvoiceButton.setVisible(false);
    fullscreenButton.addActionListener(e -> toggleFullscreen());
    fullscreenButton.setFocusPainted(false);
    fullscreenButton.setToolTipText("Basculer plein écran (Alt+Entrée)");
    Accessible.a11y(fullscreenButton,
        "Basculer plein écran",
        "Affiche ou quitte le mode plein écran pour la fiche intervention.");
    billingResourceChips.setListener(this::onBillingResourceChip);
    billingResourceChips.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    openQuoteButton.setEnabled(false);
    reloadAvailableTypes();
    buildUI();
    loadTemplates();
    installWorkflowHooks();
    buildShortcuts();
    wireDirtyTracking();
    startAutosave();
    refreshWorkflowState();
    setResizable(true);
    setMinimumSize(new Dimension(1180, 760));
    setLocationRelativeTo(owner);
    applyReadOnly();
    billingResourceChips.setEnabled(!readOnly);
  }

  private void buildUI(){
    contentPanel.removeAll();
    contentPanel.setLayout(new BorderLayout(8, 8));
    contentPanel.add(buildNorthPanel(), BorderLayout.NORTH);
    contentPanel.add(buildTabs(), BorderLayout.CENTER);
    contentPanel.add(buildFooter(), BorderLayout.SOUTH);
    configureSpinners();
    configureBillingTable();
    billingModel.addTableModelListener(e -> {
      handleBillingModelMutation();
      computeTotals();
      refreshWorkflowState();
      refreshConflictsAsync();
    });
    computeTotals();
  }

  private void loadTemplates(){
    availableTemplates.clear();
    if (templateService != null){
      try {
        availableTemplates.addAll(templateService.list());
      } catch (Exception ignore){}
    }
    DefaultComboBoxModel<InterventionTemplate> model = new DefaultComboBoxModel<>();
    model.addElement(null);
    for (InterventionTemplate template : availableTemplates){
      model.addElement(template);
    }
    templateCombo.setModel(model);
    applyTemplateButton.setEnabled(!readOnly && model.getSize() > 1);
  }

  private void installWorkflowHooks(){
    // Navigation pilotée par goToStage pour aligner l'étape active sur les onglets disponibles.
    workflowStepBar.setOnNavigate(this::navigateToStep);
    ChangeListener spinnerListener = e -> {
      if (!suppressDurationSync){
        if (e.getSource() == startSpinner){
          recomputeEndFromDuration();
        } else if (e.getSource() == endSpinner){
          recomputeDurationFromEnd();
        }
      }
      updateResourcePickerWindow();
      refreshWorkflowState();
      markEdited();
      refreshConflictsAsync();
    };
    startSpinner.addChangeListener(spinnerListener);
    endSpinner.addChangeListener(spinnerListener);
    titleField.getDocument().addDocumentListener(documentListener(this::refreshWorkflowState));
  }

  private void buildShortcuts(){
    installShortcutsOn(getRootPane());
  }

  public void installShortcutsOn(JComponent target){
    if (target == null){
      return;
    }
    KeymapUtil.bindGlobal(target, "intervention-gen-quote", KeymapUtil.ctrlG(), this::generateQuoteFromPrebilling);
    KeymapUtil.bindGlobal(target, "intervention-regenerate", KeymapUtil.ctrlR(), this::regenerateBillingFromResources);
    KeymapUtil.bindGlobal(target, "intervention-step-1", KeymapUtil.ctrlDigit(1), () -> goToStage(WorkflowStage.INTERVENTION));
    KeymapUtil.bindGlobal(target, "intervention-step-2", KeymapUtil.ctrlDigit(2), () -> goToStage(WorkflowStage.DEVIS));
    KeymapUtil.bindGlobal(target, "intervention-step-3", KeymapUtil.ctrlDigit(3), () -> goToStage(WorkflowStage.FACTURATION));
    KeymapUtil.bindGlobal(target, "intervention-prev-stage", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeymapUtil.menuMask()), this::goToPreviousStage);
    KeymapUtil.bindGlobal(target, "intervention-next-stage", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeymapUtil.menuMask()), this::goToNextStage);
    KeymapUtil.bindGlobal(target, "intervention-close", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::closeDialog);
    InputMap map = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actions = target.getActionMap();
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeymapUtil.menuMask()), "intervention-undo");
    actions.put("intervention-undo", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        undoLines();
      }
    });
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeymapUtil.menuMask()), "intervention-redo");
    actions.put("intervention-redo", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        redoLines();
      }
    });
  }

  private void wireDirtyTracking(){
    if (readOnly){
      autosaveLabel.setText("Lecture seule");
      return;
    }
    titleField.getDocument().addDocumentListener(documentListener(this::markEdited));
    addressField.getDocument().addDocumentListener(documentListener(this::markEdited));
    descriptionArea.getDocument().addDocumentListener(documentListener(this::markEdited));
    internalNoteArea.getDocument().addDocumentListener(documentListener(this::markEdited));
    closingNoteArea.getDocument().addDocumentListener(documentListener(this::markEdited));
    signatureByField.getDocument().addDocumentListener(documentListener(this::markEdited));
    ChangeListener listener = e -> markEdited();
    startSpinner.addChangeListener(listener);
    endSpinner.addChangeListener(listener);
    signatureAtSpinner.addChangeListener(listener);
    lastLinesSnapshot = deepCopyLines(billingModel.getLines());
  }

  private void startAutosave(){
    if (readOnly){
      autosaveLabel.setText("Lecture seule");
      return;
    }
    int delay = 30_000;
    try {
      GeneralSettings settings = ServiceLocator.settings().getGeneral();
      if (settings != null){
        int seconds = Math.max(5, settings.getAutosaveIntervalSeconds());
        delay = seconds * 1000;
      }
    } catch (RuntimeException ignore){
    }
    autosaveTimer.setDelay(delay);
    autosaveTimer.setInitialDelay(delay);
    autosaveTimer.setRepeats(true);
    autosaveTimer.start();
    settingsSubscription = AppEventBus.get().subscribe(SettingsEvents.GeneralSaved.class, event -> {
      if (event == null){
        return;
      }
      int seconds = Math.max(5, event.autosaveIntervalSeconds);
      int newDelay = seconds * 1000;
      autosaveTimer.setDelay(newDelay);
      autosaveTimer.setInitialDelay(newDelay);
      if (autosaveTimer.isRunning()){
        autosaveTimer.restart();
      }
      SwingUtilities.invokeLater(() ->
          autosaveLabel.setText("Autosauvegarde : " + seconds + "s appliquée ("
              + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")")
      );
    });
    addWindowListener(new WindowAdapter(){
      @Override public void windowClosing(WindowEvent e){
        if (!dirty){
          setDefaultCloseOperation(DISPOSE_ON_CLOSE);
          return;
        }
        int choice = JOptionPane.showConfirmDialog(
            dialogParent(),
            "Des modifications n'ont pas été enregistrées. Enregistrer avant de fermer ?",
            "Fermer la fiche",
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (choice == JOptionPane.CANCEL_OPTION){
          setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
          return;
        }
        if (choice == JOptionPane.YES_OPTION){
          doSaveNow(true);
          if (dirty){
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            return;
          }
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      }

      @Override public void windowClosed(WindowEvent e){
        autosaveTimer.stop();
        if (settingsSubscription != null){
          try {
            settingsSubscription.close();
          } catch (Exception ignore){
          }
          settingsSubscription = null;
        }
      }
    });
  }

  private void tryAutosave(){
    if (readOnly || !dirty){
      return;
    }
    long since = System.currentTimeMillis() - lastEditTs;
    if (since < 10_000L){
      return;
    }
    doSaveNow(false);
  }

  private void doSaveNow(boolean toast){
    if (readOnly){
      return;
    }
    try {
      collect();
      boolean persisted = false;
      if (onSaveCallback != null){
        onSaveCallback.accept(current);
        persisted = true;
      } else if (planningService != null){
        planningService.saveIntervention(current);
        persisted = true;
      }
      if (toast && persisted){
        toastSuccess("Modifications enregistrées");
      }
      markSaved(toast && persisted ? "Modifications enregistrées" : "Brouillon enregistré");
    } catch (IllegalArgumentException ex){
      autosaveLabel.setText("⚠︎ " + ex.getMessage());
    } catch (RuntimeException ex){
      String msg = ex.getMessage();
      if (msg == null || msg.isBlank()){
        msg = ex.getClass().getSimpleName();
      }
      autosaveLabel.setText("⚠︎ Échec enregistrement : " + msg);
    }
  }

  private void markEdited(){
    if (readOnly || suppressDirtyEvents){
      return;
    }
    dirty = true;
    lastEditTs = System.currentTimeMillis();
    autosaveLabel.setText("Modifications non enregistrées…");
  }

  private void markSaved(String message){
    lastLinesSnapshot = deepCopyLines(billingModel.getLines());
    dirty = false;
    lastEditTs = 0L;
    if (readOnly){
      autosaveLabel.setText("Lecture seule");
      return;
    }
    if (message == null || message.isBlank()){
      autosaveLabel.setText(" ");
    } else {
      autosaveLabel.setText(message + " (" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")");
    }
  }

  private void snapshotLinesForUndo(){
    if (readOnly || suppressDirtyEvents){
      return;
    }
    pushUndoSnapshot(billingModel.getLines());
    skipNextTableSnapshot = true;
  }

  private void handleBillingModelMutation(){
    if (suppressDirtyEvents || readOnly){
      lastLinesSnapshot = deepCopyLines(billingModel.getLines());
      return;
    }
    if (!skipNextTableSnapshot){
      pushUndoSnapshot(lastLinesSnapshot);
    } else {
      skipNextTableSnapshot = false;
    }
    lastLinesSnapshot = deepCopyLines(billingModel.getLines());
    markEdited();
  }

  private void pushUndoSnapshot(List<BillingLine> snapshot){
    if (snapshot == null){
      snapshot = List.of();
    }
    undoStack.push(deepCopyLines(snapshot));
    if (undoStack.size() > MAX_HISTORY){
      undoStack.removeLast();
    }
    redoStack.clear();
  }

  private void undoLines(){
    if (readOnly || undoStack.isEmpty()){
      return;
    }
    redoStack.push(deepCopyLines(billingModel.getLines()));
    if (redoStack.size() > MAX_HISTORY){
      redoStack.removeLast();
    }
    List<BillingLine> previous = undoStack.pop();
    suppressDirtyEvents = true;
    billingModel.setLines(deepCopyLines(previous));
    suppressDirtyEvents = false;
    lastLinesSnapshot = deepCopyLines(billingModel.getLines());
    computeTotals();
    refreshWorkflowState();
    markEdited();
  }

  private void redoLines(){
    if (readOnly || redoStack.isEmpty()){
      return;
    }
    undoStack.push(deepCopyLines(billingModel.getLines()));
    if (undoStack.size() > MAX_HISTORY){
      undoStack.removeLast();
    }
    List<BillingLine> next = redoStack.pop();
    suppressDirtyEvents = true;
    billingModel.setLines(deepCopyLines(next));
    suppressDirtyEvents = false;
    lastLinesSnapshot = deepCopyLines(billingModel.getLines());
    computeTotals();
    refreshWorkflowState();
    markEdited();
  }

  private List<BillingLine> deepCopyLines(List<BillingLine> source){
    List<BillingLine> copy = new ArrayList<>();
    if (source == null){
      return copy;
    }
    for (BillingLine line : source){
      if (line == null){
        continue;
      }
      BillingLine clone = new BillingLine();
      clone.setId(line.getId());
      clone.setAutoGenerated(line.isAutoGenerated());
      clone.setResourceId(line.getResourceId());
      clone.setDesignation(line.getDesignation());
      clone.setUnit(line.getUnit());
      clone.setQuantity(line.getQuantity());
      clone.setUnitPriceHt(line.getUnitPriceHt());
      clone.setTotalHt(line.getTotalHt());
      copy.add(clone);
    }
    return copy;
  }

  private void handleTabSelectionChange(){
    if (tabs == null || ignoreTabChange){
      return;
    }
    int selected = tabs.getSelectedIndex();
    WorkflowStage stage = stageForTabIndex(selected);
    if (stage != null && !canAccessStage(stage)){
      showStageGuardMessage(stage);
      ignoreTabChange = true;
      try {
        if (lastAccessibleTabIndex >= 0 && lastAccessibleTabIndex < tabs.getTabCount()){
          tabs.setSelectedIndex(lastAccessibleTabIndex);
        }
      } finally {
        ignoreTabChange = false;
      }
      refreshWorkflowState();
      return;
    }
    if (stage != null){
      currentStage = stage;
    }
    lastAccessibleTabIndex = Math.max(0, Math.min(selected, tabs.getTabCount() - 1));
    refreshWorkflowState();
  }

  private WorkflowStage stageForTabIndex(int index){
    if (tabs == null || index < 0 || index >= tabs.getTabCount()){
      return null;
    }
    String title = tabs.getTitleAt(index);
    if (title == null){
      return null;
    }
    String normalized = title.toLowerCase(Locale.ROOT);
    if (normalized.contains("factur")){
      return WorkflowStage.DEVIS;
    }
    if (normalized.contains("devis")){
      return WorkflowStage.FACTURATION;
    }
    if (normalized.contains("intervention") || normalized.contains("général") || normalized.contains("general")){
      return WorkflowStage.INTERVENTION;
    }
    return null;
  }

  private int findTabIndexForStage(WorkflowStage stage){
    if (tabs == null || stage == null){
      return -1;
    }
    int fallbackIntervention = -1;
    for (int i = 0; i < tabs.getTabCount(); i++){
      String title = tabs.getTitleAt(i);
      if (title == null){
        continue;
      }
      String normalized = title.toLowerCase(Locale.ROOT);
      switch (stage){
        case INTERVENTION -> {
          if (normalized.contains("général") || normalized.contains("general")){
            return i;
          }
          if (fallbackIntervention < 0 && normalized.contains("intervention")){
            fallbackIntervention = i;
          }
        }
        case DEVIS -> {
          if (normalized.contains("factur")){
            return i;
          }
        }
        case FACTURATION -> {
          if (normalized.contains("devis")){
            return i;
          }
        }
      }
    }
    return stage == WorkflowStage.INTERVENTION ? fallbackIntervention : -1;
  }

  private void navigateToStep(int index){
    WorkflowStage target = switch (index){
      case 0 -> WorkflowStage.INTERVENTION;
      case 1 -> WorkflowStage.DEVIS;
      case 2 -> WorkflowStage.FACTURATION;
      default -> WorkflowStage.INTERVENTION;
    };
    goToStage(target);
  }

  private boolean goToStage(WorkflowStage target){
    if (target == null || tabs == null){
      return false;
    }
    if (!canAccessStage(target)){
      showStageGuardMessage(target);
      return false;
    }
    int index = findTabIndexForStage(target);
    if (index < 0){
      return false;
    }
    if (tabs.getSelectedIndex() != index){
      ignoreTabChange = true;
      try {
        tabs.setSelectedIndex(index);
      } finally {
        ignoreTabChange = false;
      }
    }
    currentStage = target;
    lastAccessibleTabIndex = index;
    refreshWorkflowState();
    return true;
  }

  private void goToPreviousStage(){
    switch (currentStage){
      case FACTURATION -> goToStage(WorkflowStage.DEVIS);
      case DEVIS -> goToStage(WorkflowStage.INTERVENTION);
      default -> {
      }
    }
  }

  private void goToNextStage(){
    switch (currentStage){
      case INTERVENTION -> goToStage(WorkflowStage.DEVIS);
      case DEVIS -> goToStage(WorkflowStage.FACTURATION);
      default -> {
      }
    }
  }

  private boolean canAccessStage(WorkflowStage stage){
    if (stage == null){
      return false;
    }
    return switch (stage){
      case INTERVENTION -> true;
      case DEVIS -> isInterventionStageReady();
      case FACTURATION -> hasLinkedQuote();
    };
  }

  private void showStageGuardMessage(WorkflowStage stage){
    if (stage == null){
      return;
    }
    switch (stage){
      case DEVIS -> toastInfo("Compléter l'intervention avant d'accéder au devis.");
      case FACTURATION -> toastInfo("Générer un devis avant de passer en facturation.");
      default -> {
      }
    }
  }

  private boolean isInterventionStageReady(){
    if (!isGeneralSectionComplete()){
      return false;
    }
    Date start = spinnerDate(startSpinner);
    Date end = spinnerDate(endSpinner);
    return start != null && end != null && !end.before(start);
  }

  private boolean hasLinkedQuote(){
    return current != null && current.hasQuote();
  }

  private void syncNavigationButtons(){
    prevStageButton.setEnabled(currentStage != WorkflowStage.INTERVENTION);
    boolean canAdvance = switch (currentStage){
      case INTERVENTION -> isInterventionStageReady();
      case DEVIS -> hasLinkedQuote();
      case FACTURATION -> false;
    };
    nextStageButton.setEnabled(canAdvance);
  }

  private DocumentListener documentListener(Runnable action){
    return new DocumentListener(){
      @Override public void insertUpdate(DocumentEvent e){ action.run(); }
      @Override public void removeUpdate(DocumentEvent e){ action.run(); }
      @Override public void changedUpdate(DocumentEvent e){ action.run(); }
    };
  }

  private void refreshWorkflowState(){
    boolean generalReady = isGeneralSectionComplete();
    boolean detailsReady = hasDetailsInformation();
    boolean billingReady = isBillingSectionReady();
    boolean quoted = current != null && current.hasQuote();
    boolean canGenerateQuote = isInterventionStageReady();
    if (current != null){
      current.setGeneralDone(generalReady);
      current.setDetailsDone(detailsReady);
      current.setBillingReady(billingReady);
      // Étape courante (prochaine à faire) — ordre figé : Intervention → Devis → Facturation
      String stage = !detailsReady ? "INTERVENTION" : (!quoted ? "DEVIS" : "FACTURATION");
      current.setWorkflowStage(stage);
    }
    int active = switch (currentStage){
      case INTERVENTION -> 0;
      case DEVIS -> 1;
      case FACTURATION -> 2;
    };
    workflowStepBar.setState(active, detailsReady, quoted, billingReady);
    quoteSummaryLabel.setText(quoteStatusLabel.getText());
    boolean showQuoteAction = currentStage == WorkflowStage.INTERVENTION && !readOnly;
    stageGenerateQuoteButton.setVisible(showQuoteAction);
    stageGenerateQuoteButton.setEnabled(showQuoteAction && canGenerateQuote);
    boolean showInvoiceAction = currentStage == WorkflowStage.DEVIS && !readOnly;
    stageGenerateInvoiceButton.setVisible(showInvoiceAction);
    stageGenerateInvoiceButton.setEnabled(showInvoiceAction && quoted);
    syncNavigationButtons();
  }

  private boolean isGeneralSectionComplete(){
    return notBlank(titleField.getText())
        && clientCombo.getSelectedItem() instanceof Client
        && hasSpinnerDate(startSpinner)
        && hasSpinnerDate(endSpinner);
  }

  private boolean hasDetailsInformation(){
    return !resourcePicker.getSelectedResourceRefs().isEmpty()
        || !contactPicker.getSelectedContacts().isEmpty();
  }

  private boolean isBillingSectionReady(){
    return billingModel.getLines().stream()
        .anyMatch(line -> line != null && line.getTotalHt() != null && line.getTotalHt().signum() > 0);
  }

  private boolean hasSpinnerDate(JSpinner spinner){
    Object value = spinner.getValue();
    return value instanceof Date || value instanceof LocalDateTime;
  }

  private static boolean notBlank(String value){
    return value != null && !value.isBlank();
  }

  private JComponent buildNorthPanel(){
    JPanel north = new JPanel(new BorderLayout());
    north.add(buildToolbar(), BorderLayout.NORTH);
    north.add(workflowStepBar, BorderLayout.SOUTH);
    return north;
  }

  private JComponent buildToolbar(){
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    fullscreenButton.setText("");
    toolbar.add(fullscreenButton);
    toolbar.add(Box.createHorizontalGlue());
    autosaveLabel.setForeground(new Color(0x607D8B));
    toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(autosaveLabel);
    return toolbar;
  }

  private void applyReadOnly(){
    if (!readOnly){
      return;
    }
    titleField.setEditable(false);
    typeCombo.setEnabled(false);
    clientCombo.setEnabled(false);
    addressField.setEditable(false);
    templateCombo.setEnabled(false);
    applyTemplateButton.setEnabled(false);
    startSpinner.setEnabled(false);
    endSpinner.setEnabled(false);
    durationField.setEditable(false);
    durationField.setEnabled(false);
    descriptionArea.setEditable(false);
    internalNoteArea.setEditable(false);
    closingNoteArea.setEditable(false);
    resourcePicker.setReadOnly(true);
    contactPicker.setReadOnly(true);
    signatureByField.setEditable(false);
    signatureAtSpinner.setEnabled(false);
    importSignatureButton.setEnabled(false);
    clearSignatureButton.setEnabled(false);
    regenerateBillingButton.setEnabled(false);
    addBillingLineButton.setEnabled(false);
    removeBillingLineButton.setEnabled(false);
    generateQuoteButton.setEnabled(false);
    openQuoteButton.setEnabled(false);
    stageGenerateQuoteButton.setEnabled(false);
    stageGenerateInvoiceButton.setEnabled(false);
    stageGenerateQuoteButton.setVisible(false);
    stageGenerateInvoiceButton.setVisible(false);
    saveButton.setEnabled(false);
    billingTable.setEnabled(false);
    billingTable.setRowSelectionAllowed(false);
    historyInput.setEditable(false);
    historySend.setEnabled(false);
    billingResourceChips.setEnabled(false);
  }

  private void configureSpinners(){
    startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy HH:mm"));
    endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy HH:mm"));
    signatureAtSpinner.setEditor(new JSpinner.DateEditor(signatureAtSpinner, "dd/MM/yyyy HH:mm"));
    durationField.setText("1:00");
    durationField.getDocument().addDocumentListener(new DocumentListener(){
      @Override public void insertUpdate(DocumentEvent e){ onDurationFieldEdited(); }
      @Override public void removeUpdate(DocumentEvent e){ onDurationFieldEdited(); }
      @Override public void changedUpdate(DocumentEvent e){ onDurationFieldEdited(); }
    });
    JFormattedTextField endField = ((JSpinner.DefaultEditor) endSpinner.getEditor()).getTextField();
    endField.getDocument().addDocumentListener(new DocumentListener(){
      @Override public void insertUpdate(DocumentEvent e){ onEndSpinnerEdited(); }
      @Override public void removeUpdate(DocumentEvent e){ onEndSpinnerEdited(); }
      @Override public void changedUpdate(DocumentEvent e){ onEndSpinnerEdited(); }
    });
  }

  private void configureBillingTable(){
    billingTable.setRowHeight(26);
    billingTable.setShowHorizontalLines(true);
    billingTable.setGridColor(new Color(0xEEEEEE));
    billingTable.setFillsViewportHeight(true);
    billingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    billingTable.setAutoCreateRowSorter(true);
    billingTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    billingTable.putClientProperty("conflicts.map", Map.of());
    OverridableCellRenderers.ManualOverrideHighlightRenderer renderer =
        new OverridableCellRenderers.ManualOverrideHighlightRenderer();
    billingTable.setDefaultRenderer(BigDecimal.class, renderer);
    installNumericEditors();
    int conflictIndex = findColumnIndexIgnoreCase("Conflit");
    if (conflictIndex >= 0){
      billingTable.getColumnModel().getColumn(conflictIndex).setCellRenderer(new ConflictRenderer());
      if (conflictIndex != 0){
        billingTable.getColumnModel().moveColumn(conflictIndex, 0);
      }
      configureColumnWidth("Conflit", 70, 90);
    }
    configureColumnWidth("Auto", 70, 90);
    configureColumnWidth("Qté", 80, null);
    configureColumnWidth("Quantité", 80, null);
    configureColumnWidth("Unité", 80, null);
    configureColumnWidth("PU HT", 110, null);
    configureColumnWidth("Total HT", 140, null);
    installBillingShortcuts();
    TableUtils.persistColumnWidths(billingTable, "intervention.billing");
  }

  private void installNumericEditors(){
    configureNumericEditor("Qté");
    configureNumericEditor("Quantité");
    configureNumericEditor("PU HT");
    configureNumericEditor("PU");
  }

  private void configureNumericEditor(String columnName){
    int index = findColumnIndexIgnoreCase(columnName);
    if (index < 0){
      return;
    }
    JFormattedTextField field = createDecimalField();
    DefaultCellEditor editor = new DefaultCellEditor(field);
    editor.setClickCountToStart(1);
    billingTable.getColumnModel().getColumn(index).setCellEditor(editor);
  }

  private void configureColumnWidth(String columnName, int preferred, Integer max){
    int index = findColumnIndexIgnoreCase(columnName);
    if (index < 0){
      return;
    }
    var column = billingTable.getColumnModel().getColumn(index);
    column.setPreferredWidth(preferred);
    if (max != null){
      column.setMaxWidth(max);
    }
  }

  private void onDurationFieldEdited(){
    if (suppressDurationSync){
      return;
    }
    markEdited();
    recomputeEndFromDuration();
  }

  private void onEndSpinnerEdited(){
    if (suppressDurationSync){
      return;
    }
    recomputeDurationFromEnd();
    refreshConflictsAsync();
  }

  private void recomputeEndFromDuration(){
    int[] parsed = parseDuration(durationField.getText());
    if (parsed == null){
      return;
    }
    LocalDateTime start = toLocalDateTime(startSpinner.getValue());
    suppressDurationSync = true;
    try {
      LocalDateTime end = start.plusHours(parsed[0]).plusMinutes(parsed[1]);
      endSpinner.setValue(toDate(end));
    } finally {
      suppressDurationSync = false;
    }
    updateResourcePickerWindow();
    refreshWorkflowState();
    refreshConflictsAsync();
  }

  private void recomputeDurationFromEnd(){
    LocalDateTime start = toLocalDateTime(startSpinner.getValue());
    LocalDateTime end = toLocalDateTime(endSpinner.getValue());
    if (!end.isAfter(start)){
      return;
    }
    long minutes = ChronoUnit.MINUTES.between(start, end);
    long hours = minutes / 60;
    long mins = minutes % 60;
    suppressDurationSync = true;
    try {
      durationField.setText(hours + ":" + (mins < 10 ? "0" + mins : String.valueOf(mins)));
    } finally {
      suppressDurationSync = false;
    }
  }

  private int[] parseDuration(String text){
    if (text == null){
      return null;
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()){
      return null;
    }
    String normalized = trimmed.replace(',', '.');
    try {
      if (normalized.matches("\\d+[:hH]\\d{1,2}")){
        String[] parts = normalized.toLowerCase(Locale.ROOT).replace('h', ':').split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (minutes >= 60){
          hours += minutes / 60;
          minutes = minutes % 60;
        }
        return new int[]{Math.max(0, hours), Math.max(0, minutes)};
      }
      double value = Double.parseDouble(normalized);
      if (value < 0d){
        value = 0d;
      }
      int hours = (int) Math.floor(value);
      int minutes = (int) Math.round((value - hours) * 60d);
      if (minutes == 60){
        hours += 1;
        minutes = 0;
      }
      return new int[]{hours, Math.max(0, minutes)};
    } catch (NumberFormatException ex){
      return null;
    }
  }

  private void refreshConflictsAsync(){
    if (billingTable == null){
      return;
    }
    Set<String> resourceIds = collectBillingResourceIds();
    if (resourceIds.isEmpty()){
      conflictRefreshSeq.incrementAndGet();
      billingTable.putClientProperty("conflicts.map", Map.of());
      billingTable.repaint();
      return;
    }
    if (planningService == null){
      conflictRefreshSeq.incrementAndGet();
      billingTable.putClientProperty("conflicts.map", Map.of());
      billingTable.repaint();
      return;
    }
    LocalDateTime start = toLocalDateTime(startSpinner.getValue());
    LocalDateTime end = toLocalDateTime(endSpinner.getValue());
    if (!end.isAfter(start)){
      conflictRefreshSeq.incrementAndGet();
      billingTable.putClientProperty("conflicts.map", Map.of());
      billingTable.repaint();
      return;
    }
    LocalDate from = start.toLocalDate().minusDays(1);
    LocalDate to = end.toLocalDate().plusDays(1);
    long token = conflictRefreshSeq.incrementAndGet();
    CompletableFuture
        .supplyAsync(() -> {
          try {
            List<Intervention> list = planningService.listInterventions(from, to);
            return list != null ? list : List.<Intervention>of();
          } catch (Exception ex){
            return List.<Intervention>of();
          }
        })
        .thenAccept(interventions -> SwingUtilities.invokeLater(() -> {
          if (token != conflictRefreshSeq.get()){
            return;
          }
          Map<String, String> conflicts = detectConflicts(interventions, start, end, resourceIds);
          billingTable.putClientProperty("conflicts.map", conflicts);
          billingTable.repaint();
        }));
  }

  private Set<String> collectBillingResourceIds(){
    Set<String> ids = new HashSet<>();
    for (BillingLine line : billingModel.getLines()){
      if (line == null){
        continue;
      }
      String rid = line.getResourceId();
      if (rid != null && !rid.isBlank()){
        ids.add(rid);
      }
    }
    return ids;
  }

  private Map<String, String> detectConflicts(List<Intervention> interventions,
                                              LocalDateTime start,
                                              LocalDateTime end,
                                              Set<String> resourceIds){
    Map<String, String> map = new HashMap<>();
    if (interventions == null || interventions.isEmpty()){
      return map;
    }
    String currentId = current != null && current.getId() != null ? current.getId().toString() : null;
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    for (Intervention other : interventions){
      if (other == null){
        continue;
      }
      if (!AgencyContext.matchesCurrentAgency(other)){
        continue;
      }
      if (other.getId() != null && Objects.equals(currentId, other.getId().toString())){
        continue;
      }
      LocalDateTime otherStart = other.getDateHeureDebut();
      LocalDateTime otherEnd = other.getDateHeureFin();
      if (otherStart == null || otherEnd == null){
        continue;
      }
      boolean overlap = otherStart.isBefore(end) && otherEnd.isAfter(start);
      if (!overlap){
        continue;
      }
      for (ResourceRef ref : other.getResources()){
        if (ref == null || ref.getId() == null){
          continue;
        }
        String rid = ref.getId().toString();
        if (!resourceIds.contains(rid)){
          continue;
        }
        if (map.containsKey(rid)){
          continue;
        }
        String clientName = other.getClientName();
        if (clientName == null || clientName.isBlank()){
          clientName = other.getLabel();
        }
        String tooltip = "Chevauche " + fmt.format(otherStart) + "–" + fmt.format(otherEnd);
        if (clientName != null && !clientName.isBlank()){
          tooltip += " / " + clientName;
        }
        map.put(rid, tooltip);
      }
    }
    return map;
  }

  private class ConflictRenderer extends DefaultTableCellRenderer {
    ConflictRenderer(){
      setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setText("");
      setToolTipText(null);
      setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
      if (table == null){
        return component;
      }
      int modelRow = table.convertRowIndexToModel(row);
      if (modelRow < 0 || modelRow >= billingModel.getRowCount()){
        return component;
      }
      BillingLine line = billingModel.lineAt(modelRow);
      String rid = line != null ? line.getResourceId() : null;
      @SuppressWarnings("unchecked")
      Map<String, String> conflicts = (Map<String, String>) table.getClientProperty("conflicts.map");
      if (rid != null && conflicts != null && conflicts.containsKey(rid)){
        setText("⚠");
        setForeground(isSelected ? table.getSelectionForeground() : new Color(0xB71C1C));
        setToolTipText(conflicts.get(rid));
      }
      return component;
    }
  }

  private JFormattedTextField createDecimalField(){
    NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
    format.setGroupingUsed(false);
    NumberFormatter formatter = new NumberFormatter(format);
    formatter.setAllowsInvalid(false);
    formatter.setCommitsOnValidEdit(true);
    formatter.setMinimum(0d);
    JFormattedTextField field = new JFormattedTextField(formatter);
    field.setColumns(8);
    field.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
    return field;
  }

  private int findColumnIndexIgnoreCase(String name){
    if (name == null || name.isBlank()){
      return -1;
    }
    var columnModel = billingTable.getColumnModel();
    for (int i = 0; i < columnModel.getColumnCount(); i++){
      Object header = columnModel.getColumn(i).getHeaderValue();
      if (header != null && header.toString().equalsIgnoreCase(name)){
        return i;
      }
    }
    return -1;
  }

  private void installBillingShortcuts(){
    InputMap map = billingTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap actions = billingTable.getActionMap();
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "billing-delete");
    actions.put("billing-delete", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        removeSelectedBillingLine();
      }
    });
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeymapUtil.menuMask()), "billing-duplicate");
    actions.put("billing-duplicate", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        duplicateSelectedBillingLine();
      }
    });
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeymapUtil.menuMask()), "billing-move-up");
    actions.put("billing-move-up", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        moveSelectedBillingLine(-1);
      }
    });
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeymapUtil.menuMask()), "billing-move-down");
    actions.put("billing-move-down", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        moveSelectedBillingLine(1);
      }
    });
  }

  private JComponent buildHeader(){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 6, 6, 6);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 0;
    int y = 0;

    gc.gridx = 0; gc.gridy = y; panel.add(new JLabel("Modèle"), gc);
    gc.gridx = 1; gc.weightx = 1; gc.gridwidth = 2; panel.add(templateCombo, gc);
    gc.gridx = 3; gc.weightx = 0; gc.gridwidth = 1; panel.add(applyTemplateButton, gc);
    y++;

    gc.gridx = 0; gc.gridy = y; panel.add(new JLabel("Titre"), gc);
    gc.gridx = 1; gc.weightx = 1; panel.add(titleField, gc); gc.weightx = 0;
    gc.gridx = 2; panel.add(new JLabel("Type"), gc);
    gc.gridx = 3; panel.add(typeCombo, gc);
    y++;

    gc.gridx = 0; gc.gridy = y; panel.add(new JLabel("Client"), gc);
    gc.gridx = 1; gc.weightx = 1; panel.add(clientCombo, gc); gc.weightx = 0;
    gc.gridx = 2; panel.add(new JLabel("Adresse / chantier"), gc);
    gc.gridx = 3; panel.add(addressField, gc);
    y++;

    JPanel datesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    datesRow.add(new JLabel("Début"));
    datesRow.add(startSpinner);
    datesRow.add(new JLabel("Durée (H:MM)"));
    durationField.setColumns(6);
    datesRow.add(durationField);
    datesRow.add(new JLabel("Fin"));
    datesRow.add(endSpinner);
    gc.gridx = 0; gc.gridy = y; gc.gridwidth = 4; panel.add(datesRow, gc);
    gc.gridwidth = 1;
    y++;

    return panel;
  }

  private JComponent buildTabs(){
    tabs.removeAll();
    for (ChangeListener listener : tabs.getChangeListeners()){
      tabs.removeChangeListener(listener);
    }
    tabs.addTab("Général", IconRegistry.small("info"), buildGeneralTab());
    tabs.addTab("Intervention", IconRegistry.small("task"), buildInterventionTab());
    tabs.addTab("Devis", IconRegistry.small("file"), buildQuoteTab());
    tabs.addTab("Facturation", IconRegistry.small("invoice"), buildFacturationTab());
    tabs.addTab("Historique", IconRegistry.small("info"), buildHistoryTab());
    tabs.addChangeListener(e -> handleTabSelectionChange());
    lastAccessibleTabIndex = Math.max(0, tabs.getSelectedIndex());
    WorkflowStage initialStage = stageForTabIndex(lastAccessibleTabIndex);
    if (initialStage != null){
      currentStage = initialStage;
    } else {
      currentStage = WorkflowStage.INTERVENTION;
    }
    return tabs;
  }

  private JComponent buildGeneralTab(){
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.add(buildHeader(), BorderLayout.NORTH);
    panel.add(panelWithLabel("Description", new JScrollPane(descriptionArea)), BorderLayout.CENTER);
    return panel;
  }

  private JComponent buildInterventionTab(){
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JTabbedPane leftTabs = new JTabbedPane();
    leftTabs.addTab("Ressources", IconRegistry.small("wrench"), resourcePicker);
    leftTabs.addTab("Contacts client", IconRegistry.small("user"), contactPicker);

    JPanel right = new JPanel(new GridLayout(3, 1, 6, 6));
    right.add(panelWithLabel("Note interne", new JScrollPane(internalNoteArea)));
    right.add(panelWithLabel("Note de fin", new JScrollPane(closingNoteArea)));
    right.add(buildSignaturePanel());

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabs, right);
    split.setResizeWeight(0.55);
    panel.add(split, BorderLayout.CENTER);
    return panel;
  }

  private JComponent buildFacturationTab(){
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(regenerateBillingButton);
    toolbar.add(addBillingLineButton);
    toolbar.add(removeBillingLineButton);
    toolbar.addSeparator();
    toolbar.add(generateQuoteButton);
    toolbar.add(openQuoteButton);
    toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(quoteStatusLabel);
    JPanel header = new JPanel(new BorderLayout(0, 4));
    header.add(toolbar, BorderLayout.NORTH);
    header.add(billingResourceChips, BorderLayout.CENTER);
    panel.add(header, BorderLayout.NORTH);
    panel.add(new JScrollPane(billingTable), BorderLayout.CENTER);
    JPanel totals = new JPanel(new GridLayout(1, 0, 12, 0));
    totals.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    totalHtLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    totalTvaLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    totalTtcLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    totals.add(totalHtLabel);
    totals.add(totalTvaLabel);
    totals.add(totalTtcLabel);
    panel.add(totals, BorderLayout.SOUTH);
    return panel;
  }

  private JComponent buildQuoteTab(){
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JPanel info = new JPanel();
    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
    info.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    JLabel title = new JLabel("Statut du devis lié :");
    title.setAlignmentX(Component.LEFT_ALIGNMENT);
    quoteSummaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    info.add(title);
    info.add(Box.createVerticalStrut(6));
    info.add(quoteSummaryLabel);
    info.add(Box.createVerticalStrut(12));
    JLabel helper = new JLabel("Fonctionnalités de suivi du devis à venir.");
    helper.setAlignmentX(Component.LEFT_ALIGNMENT);
    info.add(helper);
    panel.add(info, BorderLayout.NORTH);
    return panel;
  }

  private JComponent buildHistoryTab(){
    historyList.setCellRenderer(new HistoryRenderer());
    historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    historyList.setFixedCellHeight(-1);
    historyInput.setLineWrap(true);
    historyInput.setWrapStyleWord(true);
    if (historySend.getActionListeners().length == 0){
      historySend.addActionListener(e -> sendHistoryComment());
    }
    JPanel composer = new JPanel(new BorderLayout(6, 6));
    composer.add(new JScrollPane(historyInput), BorderLayout.CENTER);
    composer.add(historySend, BorderLayout.EAST);
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.add(new JScrollPane(historyList), BorderLayout.CENTER);
    panel.add(composer, BorderLayout.SOUTH);
    return panel;
  }

  private void loadHistory(){
    historyModel.clear();
    if (current == null || current.getId() == null){
      return;
    }
    var timeline = ServiceLocator.timeline();
    if (timeline == null){
      return;
    }
    UUID interventionId = current.getId();
    CompletableFuture
        .supplyAsync(() -> {
          try {
            List<TimelineEvent> events = timeline.list(interventionId.toString());
            return events != null ? events : List.<TimelineEvent>of();
          } catch (Exception ex){
            return List.<TimelineEvent>of();
          }
        })
        .thenAccept(events -> SwingUtilities.invokeLater(() -> {
          historyModel.clear();
          for (TimelineEvent event : events){
            historyModel.addElement(event);
          }
        }));
  }

  private void sendHistoryComment(){
    String text = historyInput.getText();
    if (text == null){
      return;
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()){
      return;
    }
    historyInput.setText("");
    logEventAsync("COMMENT", trimmed);
  }

  private void logEventAsync(String type, String message){
    if (current == null || current.getId() == null){
      return;
    }
    var timeline = ServiceLocator.timeline();
    if (timeline == null){
      return;
    }
    String trimmed = message == null ? "" : message.trim();
    if (trimmed.isEmpty()){
      return;
    }
    UUID interventionId = current.getId();
    CompletableFuture
        .supplyAsync(() -> {
          try {
            TimelineEvent event = new TimelineEvent();
            event.setType(type);
            event.setMessage(trimmed);
            event.setTimestamp(Instant.now());
            event.setAuthor(System.getProperty("user.name", "user"));
            return timeline.append(interventionId.toString(), event);
          } catch (Exception ex){
            return null;
          }
        })
        .thenAccept(saved -> {
          if (saved != null){
            SwingUtilities.invokeLater(() -> historyModel.addElement(saved));
          }
        });
  }

  private static class HistoryRenderer extends JPanel implements ListCellRenderer<TimelineEvent> {
    private static final DateTimeFormatter HISTORY_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private final JLabel header = new JLabel();
    private final JLabel body = new JLabel();

    HistoryRenderer(){
      super(new BorderLayout(6, 2));
      setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
      header.setFont(header.getFont().deriveFont(Font.BOLD));
      body.setVerticalAlignment(SwingConstants.TOP);
      header.setOpaque(false);
      body.setOpaque(false);
      add(header, BorderLayout.NORTH);
      add(body, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TimelineEvent> list,
                                                  TimelineEvent value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus){
      String type = value != null && value.getType() != null ? value.getType() : "INFO";
      Instant ts = value != null ? value.getTimestamp() : null;
      String when = ts == null ? "" : HISTORY_FORMAT.format(ts);
      String author = value != null && value.getAuthor() != null ? value.getAuthor() : "";
      String headerText = "[" + type + "]";
      if (!when.isBlank()){
        headerText += " " + when;
      }
      if (!author.isBlank()){
        headerText += " — " + author;
      }
      header.setText(headerText);
      String message = value != null ? value.getMessage() : "";
      body.setText("<html>" + escapeHtml(message) + "</html>");
      Color background = isSelected ? new Color(0xDCEAFB) : Color.WHITE;
      setBackground(background);
      setOpaque(true);
      return this;
    }

    private String escapeHtml(String text){
      if (text == null || text.isEmpty()){
        return "";
      }
      return text.replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\n", "<br>");
    }
  }

  private JPanel panelWithLabel(String title, JComponent component){
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(component, BorderLayout.CENTER);
    return panel;
  }

  private JComponent buildSignaturePanel(){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4, 4, 4, 4);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.NONE;
    gc.weightx = 0;
    int y = 0;

    gc.gridx = 0; gc.gridy = y; panel.add(new JLabel(IconRegistry.small("signature")), gc);
    gc.gridx = 1; panel.add(new JLabel("Signé par"), gc);
    gc.gridx = 2; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(signatureByField, gc); gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
    gc.gridx = 3; panel.add(new JLabel("Le"), gc);
    gc.gridx = 4; panel.add(signatureAtSpinner, gc);
    y++;

    signaturePreview.setPreferredSize(new Dimension(200, 80));
    signaturePreview.setBorder(BorderFactory.createLineBorder(new Color(0xDDDDDD)));
    signaturePreview.setHorizontalAlignment(SwingConstants.CENTER);
    signaturePreview.setOpaque(true);
    signaturePreview.setBackground(Color.WHITE);
    signaturePreview.setText("Aucune signature");

    gc.gridx = 2; gc.gridy = y; panel.add(importSignatureButton, gc);
    gc.gridx = 3; panel.add(clearSignatureButton, gc);
    gc.gridx = 4; gc.fill = GridBagConstraints.BOTH; gc.weightx = 1;
    panel.add(signaturePreview, gc);

    return panel;
  }

  private JComponent buildFooter(){
    JPanel panel = new JPanel(new BorderLayout());
    JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
    navigation.add(prevStageButton);
    navigation.add(nextStageButton);
    panel.add(navigation, BorderLayout.WEST);
    JPanel contextual = new JPanel(new FlowLayout(FlowLayout.CENTER));
    contextual.add(stageGenerateQuoteButton);
    contextual.add(stageGenerateInvoiceButton);
    panel.add(contextual, BorderLayout.CENTER);
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton cancel = new JButton("Fermer");
    cancel.addActionListener(e -> closeDialog());
    actions.add(saveButton);
    actions.add(cancel);
    panel.add(actions, BorderLayout.EAST);
    return panel;
  }

  private void importSignature(){
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(dialogParent());
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File file = chooser.getSelectedFile();
    try {
      byte[] data = Files.readAllBytes(file.toPath());
      signatureBase64 = Base64.getEncoder().encodeToString(data);
      signatureAtSpinner.setValue(new Date());
      toastSuccess("Signature importée");
    } catch (IOException ex){
      signatureBase64 = null;
      toastError("Impossible de lire le fichier sélectionné");
    }
    updateSignaturePreview();
    markEdited();
  }

  private void clearSignature(){
    signatureBase64 = null;
    updateSignaturePreview();
    markEdited();
  }

  private void updateSignaturePreview(){
    if (signatureBase64 != null && !signatureBase64.isBlank()){
      try {
        byte[] bytes = Base64.getDecoder().decode(signatureBase64);
        ImageIcon icon = createSignatureIcon(bytes);
        signaturePreview.setIcon(icon);
        signaturePreview.setText(icon == null ? "Aperçu indisponible" : "");
      } catch (IllegalArgumentException ex){
        signaturePreview.setIcon(null);
        signaturePreview.setText("Aperçu indisponible");
      }
    } else {
      signaturePreview.setIcon(null);
      signaturePreview.setText("Aucune signature");
    }
  }

  private ImageIcon createSignatureIcon(byte[] bytes){
    if (bytes == null || bytes.length == 0){
      return null;
    }
    ImageIcon icon = new ImageIcon(bytes);
    int w = icon.getIconWidth();
    int h = icon.getIconHeight();
    if (w <= 0 || h <= 0){
      return null;
    }
    int boxW = 200;
    int boxH = 80;
    double scale = Math.min((double) boxW / w, (double) boxH / h);
    if (scale > 1d){
      scale = 1d;
    }
    int newW = Math.max(1, (int) Math.round(w * scale));
    int newH = Math.max(1, (int) Math.round(h * scale));
    Image scaled = icon.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
    return new ImageIcon(scaled);
  }

  private void onSave(){
    try {
      collect();
      if (onSaveCallback != null){
        onSaveCallback.accept(current);
      }
      markSaved("Modifications enregistrées");
      saved = true;
      closeDialog();
      toastSuccess("Intervention enregistrée");
    } catch (IllegalArgumentException ex){
      JOptionPane.showMessageDialog(dialogParent(), ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    } catch (RuntimeException ex){
      String message = ex.getMessage();
      if (message == null || message.isBlank()){
        message = "Impossible d'enregistrer l'intervention.";
      }
      JOptionPane.showMessageDialog(dialogParent(), message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void regenerateBillingFromResources(){
    syncAutoBillingLinesWithResources();
    toastInfo("Lignes générées depuis les ressources");
    refreshWorkflowState();
  }

  private void applySelectedTemplate(){
    if (readOnly){
      return;
    }
    InterventionTemplate template = (InterventionTemplate) templateCombo.getSelectedItem();
    if (template == null){
      toastInfo("Aucun modèle sélectionné");
      return;
    }
    snapshotLinesForUndo();
    String typeId = template.getDefaultTypeId();
    if (typeId != null && !typeId.isBlank()){
      for (int i = 0; i < typeCombo.getItemCount(); i++){
        InterventionType type = typeCombo.getItemAt(i);
        if (type != null && type.getCode() != null && typeId.equalsIgnoreCase(type.getCode())){
          typeCombo.setSelectedIndex(i);
          break;
        }
      }
    }
    Date start = spinnerDate(startSpinner);
    Integer duration = template.getDefaultDurationMinutes();
    if (start != null && duration != null && duration > 0){
      long millis = start.getTime() + duration.longValue() * 60_000L;
      endSpinner.setValue(new Date(millis));
    }
    List<BillingLine> lines = billingModel.getLines();
    List<InterventionTemplate.TemplateLine> defaults = template.getDefaultLines();
    if (defaults != null && !defaults.isEmpty()){
      Date begin = spinnerDate(startSpinner);
      Date finish = spinnerDate(endSpinner);
      for (InterventionTemplate.TemplateLine source : defaults){
        if (source == null){
          continue;
        }
        BillingLine line = new BillingLine();
        line.setId(UUID.randomUUID().toString());
        line.setAutoGenerated(false);
        String designation = source.getDesignation();
        line.setDesignation(designation != null && !designation.isBlank() ? designation : "Ligne modèle");
        String unit = source.getUnit();
        if (unit == null || unit.isBlank()){
          unit = "u";
        }
        line.setUnit(unit);
        BigDecimal quantity = source.getQuantity();
        if (quantity == null){
          if ("h".equalsIgnoreCase(unit)){
            quantity = PreDevisUtil.computeRoundedHours(begin, finish);
          } else {
            quantity = BigDecimal.ONE;
          }
        }
        line.setQuantity(quantity);
        BigDecimal price = source.getUnitPriceHt();
        if (price == null){
          price = BigDecimal.ZERO;
        }
        line.setUnitPriceHt(price);
        line.setTotalHt(price.multiply(quantity));
        lines.add(line);
      }
      billingModel.setLines(lines);
    }
    syncAutoBillingLinesWithResources();
    String name = template.getName();
    toastSuccess(name == null || name.isBlank() ? "Modèle appliqué" : "Modèle appliqué : " + name);
    markEdited();
  }

  private void addManualBillingLine(){
    snapshotLinesForUndo();
    BillingLine line = new BillingLine();
    line.setId(UUID.randomUUID().toString());
    line.setDesignation("Ligne manuelle");
    line.setAutoGenerated(false);
    List<BillingLine> lines = billingModel.getLines();
    lines.add(line);
    billingModel.setLines(lines);
    int row = billingModel.getRowCount() - 1;
    if (row >= 0){
      billingTable.setRowSelectionInterval(row, row);
      billingTable.editCellAt(row, 1);
      billingTable.requestFocusInWindow();
    }
    computeTotals();
    refreshWorkflowState();
  }

  private void onBillingResourceChip(Resource resource){
    if (resource == null || readOnly){
      return;
    }
    snapshotLinesForUndo();
    List<BillingLine> generated = PreDevisUtil.fromResourcesWithWindow(List.of(resource),
        spinnerDate(startSpinner),
        spinnerDate(endSpinner));
    BillingLine line = generated.isEmpty() ? null : generated.get(0);
    if (line == null){
      line = new BillingLine();
      line.setId(UUID.randomUUID().toString());
      line.setDesignation(resource != null && resource.getName() != null && !resource.getName().isBlank()
          ? resource.getName()
          : "Ressource");
      line.setUnit("u");
      line.setQuantity(BigDecimal.ONE);
      line.setUnitPriceHt(BigDecimal.ZERO);
      line.setTotalHt(BigDecimal.ZERO);
    }
    line.setAutoGenerated(false);
    if (resource != null && resource.getId() != null){
      line.setResourceId(resource.getId().toString());
    }
    List<BillingLine> lines = billingModel.getLines();
    lines.add(line);
    billingModel.setLines(lines);
    computeTotals();
    refreshWorkflowState();
    markEdited();
  }

  private void removeSelectedBillingLine(){
    if (readOnly){
      return;
    }
    int viewRow = billingTable.getSelectedRow();
    if (viewRow < 0){
      return;
    }
    int modelRow = billingTable.convertRowIndexToModel(viewRow);
    List<BillingLine> lines = billingModel.getLines();
    if (modelRow < 0 || modelRow >= lines.size()){
      return;
    }
    snapshotLinesForUndo();
    BillingLine removed = lines.remove(modelRow);
    billingModel.setLines(lines);
    computeTotals();
    refreshWorkflowState();
    List<BillingLine> removedCopy = removed == null ? List.of() : deepCopyLines(List.of(removed));
    if (!removedCopy.isEmpty()){
      BillingLine toRestore = removedCopy.get(0);
      Toasts.showWithAction(dialogParent(), "Ligne supprimée", "Annuler", () -> SwingUtilities.invokeLater(() -> {
        if (readOnly){
          return;
        }
        snapshotLinesForUndo();
        List<BillingLine> current = billingModel.getLines();
        int idx = Math.max(0, Math.min(modelRow, current.size()));
        current.add(idx, toRestore);
        billingModel.setLines(current);
        computeTotals();
        refreshWorkflowState();
        int viewIndex = billingTable.convertRowIndexToView(idx);
        if (viewIndex >= 0){
          billingTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
          billingTable.scrollRectToVisible(billingTable.getCellRect(viewIndex, 0, true));
        }
      }));
    }
  }

  private void duplicateSelectedBillingLine(){
    if (readOnly){
      return;
    }
    int viewRow = billingTable.getSelectedRow();
    if (viewRow < 0){
      return;
    }
    int modelRow = billingTable.convertRowIndexToModel(viewRow);
    List<BillingLine> lines = billingModel.getLines();
    if (modelRow < 0 || modelRow >= lines.size()){
      return;
    }
    snapshotLinesForUndo();
    List<BillingLine> copies = deepCopyLines(List.of(lines.get(modelRow)));
    if (copies.isEmpty()){
      return;
    }
    BillingLine duplicate = copies.get(0);
    duplicate.setId(UUID.randomUUID().toString());
    duplicate.setAutoGenerated(false);
    lines.add(modelRow + 1, duplicate);
    billingModel.setLines(lines);
    computeTotals();
    refreshWorkflowState();
    int viewTarget = billingTable.convertRowIndexToView(modelRow + 1);
    if (viewTarget >= 0){
      billingTable.getSelectionModel().setSelectionInterval(viewTarget, viewTarget);
      billingTable.scrollRectToVisible(billingTable.getCellRect(viewTarget, 0, true));
    }
  }

  private void moveSelectedBillingLine(int delta){
    if (readOnly){
      return;
    }
    int viewRow = billingTable.getSelectedRow();
    if (viewRow < 0){
      return;
    }
    int modelRow = billingTable.convertRowIndexToModel(viewRow);
    List<BillingLine> lines = billingModel.getLines();
    if (modelRow < 0 || modelRow >= lines.size()){
      return;
    }
    int target = Math.max(0, Math.min(lines.size() - 1, modelRow + delta));
    if (target == modelRow){
      return;
    }
    snapshotLinesForUndo();
    BillingLine moved = lines.remove(modelRow);
    lines.add(target, moved);
    billingModel.setLines(lines);
    computeTotals();
    refreshWorkflowState();
    int viewTarget = billingTable.convertRowIndexToView(target);
    if (viewTarget >= 0){
      billingTable.getSelectionModel().setSelectionInterval(viewTarget, viewTarget);
      billingTable.scrollRectToVisible(billingTable.getCellRect(viewTarget, 0, true));
    }
  }

  private void actionGenerateQuote(){
    if (readOnly){
      return;
    }
    if (!isInterventionStageReady()){
      toastInfo("Compléter l'intervention avant de générer un devis.");
      return;
    }
    try {
      collect();
    } catch (IllegalArgumentException ex){
      toastInfo(ex.getMessage());
      return;
    } catch (RuntimeException ex){
      String message = ex.getMessage();
      if (message == null || message.isBlank()){
        message = "Impossible de préparer le devis.";
      }
      toastError(message);
      return;
    }
    generateQuoteFromPrebilling();
    if (current != null && current.hasQuote()){
      goToStage(WorkflowStage.DEVIS);
    } else {
      refreshWorkflowState();
    }
  }

  private void actionGenerateInvoice(){
    if (readOnly){
      return;
    }
    if (current == null || !current.hasQuote()){
      toastInfo("Aucun devis associé.");
      return;
    }
    try {
      var invoiceService = ServiceFactory.invoices();
      if (invoiceService == null){
        toastError("Service facturation indisponible.");
        return;
      }
      UUID quoteId = current.getQuoteId();
      if (quoteId == null){
        toastInfo("Identifiant de devis manquant.");
        return;
      }
      Invoice invoice = invoiceService.createFromQuote(quoteId);
      if (invoice == null){
        toastError("Le service n'a retourné aucune facture.");
        return;
      }
      if (invoice.getNumber() != null && !invoice.getNumber().isBlank()){
        current.setInvoiceNumber(invoice.getNumber());
      }
      if (onSaveCallback != null){
        onSaveCallback.accept(current);
      } else if (planningService != null){
        planningService.saveIntervention(current);
      }
      toastSuccess("Facture générée.");
      markSaved("Facture générée");
      String ref = invoice.getNumber();
      logEventAsync("ACTION", ref == null || ref.isBlank()
          ? "Facture générée"
          : "Facture générée : " + ref);
      goToStage(WorkflowStage.FACTURATION);
    } catch (Exception ex){
      String message = ex.getMessage();
      if (message == null || message.isBlank()){
        toastError("Échec de génération de la facture.");
        logEventAsync("SYSTEM", "Erreur génération facture");
      } else {
        toastError("Échec de génération de la facture : " + message);
        logEventAsync("SYSTEM", "Erreur génération facture : " + message);
      }
    }
  }

  private void generateQuoteFromPrebilling(){
    billingModel.recalcAll();
    computeTotals();
    List<BillingLine> lines = billingModel.getLines();
    if (lines.isEmpty()){
      toastInfo("Aucune ligne de pré-devis à convertir");
      return;
    }
    if (current == null){
      toastError("Intervention introuvable");
      return;
    }
    if (current.hasQuote()){
      int confirm = JOptionPane.showConfirmDialog(dialogParent(),
          "Un devis est déjà lié à cette intervention. Générer un nouveau devis ?",
          "Devis existant",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.WARNING_MESSAGE);
      if (confirm != JOptionPane.OK_OPTION){
        return;
      }
    }
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      toastError("Service devis indisponible");
      return;
    }
    try {
      current.setBillingLines(lines);
      QuoteV2 quote = sales.createQuoteFromIntervention(current);
      if (quote == null){
        throw new IllegalStateException("Réponse vide du service devis");
      }
      applyQuoteToIntervention(current, quote, lines);
      updateQuoteBadge();
      String ref = quote.getReference();
      if (ref == null || ref.isBlank()){
        ref = quote.getId() != null ? quote.getId() : "";
      }
      toastSuccess(ref == null || ref.isBlank() ? "Devis créé" : "Devis créé — " + ref);
      if (onSaveCallback != null){
        onSaveCallback.accept(current);
      } else if (planningService != null){
        planningService.saveIntervention(current);
      }
      refreshWorkflowState();
      markSaved("Devis enregistré");
      String finalRef = ref;
      logEventAsync("ACTION", finalRef == null || finalRef.isBlank()
          ? "Devis généré"
          : "Devis généré : " + finalRef);
    } catch (Exception ex){
      toastError("Échec de génération du devis : " + ex.getMessage());
      logEventAsync("SYSTEM", "Erreur génération devis : " + ex.getMessage());
    }
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

  private void openQuotePreview(){
    if (current == null || !current.hasQuote()){
      toastInfo("Aucun devis lié à cette intervention.");
      return;
    }
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      toastError("Service devis indisponible");
      return;
    }
    UUID quoteId = current.getQuoteId();
    if (quoteId == null){
      toastError("Identifiant de devis indisponible.");
      return;
    }
    try {
      QuoteV2 quote = sales.getQuote(quoteId.toString());
      if (quote == null){
        toastError("Devis introuvable côté service.");
        return;
      }
      new QuotePreviewDialog(dialogWindow(), quote).setVisible(true);
    } catch (Exception ex){
      toastError("Impossible d'ouvrir le devis : " + ex.getMessage());
    }
  }

  private void onResourceSelectionChanged(){
    if (readOnly){
      return;
    }
    syncAutoBillingLinesWithResources();
    refreshWorkflowState();
    markEdited();
    List<Resource> selected = resourcePicker.getSelectedResources();
    int count = selected == null ? 0 : selected.size();
    logEventAsync("ACTION", "Sélection de ressources mise à jour (" + count + ")");
    refreshConflictsAsync();
  }

  private void syncAutoBillingLinesWithResources(){
    snapshotLinesForUndo();
    List<BillingLine> existing = billingModel.getLines();
    List<BillingLine> manual = new ArrayList<>();
    LinkedHashMap<String, BillingLine> autoByKey = new LinkedHashMap<>();
    for (BillingLine line : existing){
      if (line == null){
        continue;
      }
      if (line.isAutoGenerated() && line.getResourceId() != null && !line.getResourceId().isBlank()){
        autoByKey.put(line.getResourceId(), line);
      } else {
        manual.add(line);
      }
    }
    List<BillingLine> updated = new ArrayList<>(manual);
    List<Resource> selectedResources = resourcePicker.getSelectedResources();
    Date start = spinnerDate(startSpinner);
    Date end = spinnerDate(endSpinner);
    for (Resource resource : selectedResources){
      if (resource == null){
        continue;
      }
      String key = resourceKey(resource);
      BillingLine line = key != null ? autoByKey.remove(key) : null;
      if (line == null){
        line = new BillingLine();
        line.setId(UUID.randomUUID().toString());
        line.setAutoGenerated(true);
      }
      if (resource.getId() != null){
        line.setResourceId(resource.getId().toString());
      } else if (key != null){
        line.setResourceId(key);
      }
      String name = resource.getName();
      if (name != null && !name.isBlank()){
        line.setDesignation(name);
      } else if (line.getDesignation() == null || line.getDesignation().isBlank()){
        line.setDesignation("Ressource");
      }
      PreDevisUtil.Inferred inferred = PreDevisUtil.inferUnitAndQty(resource, start, end);
      line.setUnit(inferred.unit());
      line.setQuantity(inferred.quantity());
      BigDecimal price = resource.getUnitPriceHt();
      if (price != null){
        line.setUnitPriceHt(price);
      } else if (line.getUnitPriceHt() == null){
        line.setUnitPriceHt(BigDecimal.ZERO);
      }
      BigDecimal unitPrice = line.getUnitPriceHt() != null ? line.getUnitPriceHt() : BigDecimal.ZERO;
      BigDecimal quantity = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ONE;
      line.setTotalHt(unitPrice.multiply(quantity));
      updated.add(line);
    }
    billingModel.setLines(updated);
    computeTotals();
    refreshWorkflowState();
  }

  private String resourceKey(Resource resource){
    if (resource == null){
      return null;
    }
    UUID id = resource.getId();
    if (id != null){
      return id.toString();
    }
    String name = resource.getName();
    if (name != null && !name.isBlank()){
      return "name:" + name.trim().toLowerCase(Locale.ROOT);
    }
    return null;
  }

  private void toggleFullscreen(){
    if (previousBounds == null){
      previousBounds = getBounds();
      Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
      setBounds(screen);
      fullscreenButton.setIcon(IconRegistry.small("minimize"));
      fullscreenButton.setToolTipText("Quitter le plein écran");
    } else {
      setBounds(previousBounds);
      previousBounds = null;
      fullscreenButton.setIcon(IconRegistry.small("maximize"));
      fullscreenButton.setToolTipText("Plein écran");
    }
    revalidate();
    repaint();
  }

  public void setParentComponent(Component parent){
    this.parentComponent = parent != null ? parent : this;
  }

  public void setCloseHandler(Runnable handler){
    this.closeHandler = handler != null ? handler : this::dispose;
  }

  public void setFullscreenButtonVisible(boolean visible){
    fullscreenButton.setVisible(visible);
  }

  public JComponent detachContentPanel(){
    Container parent = contentPanel.getParent();
    if (parent != null){
      parent.remove(contentPanel);
      parent.revalidate();
      parent.repaint();
    }
    return contentPanel;
  }

  private Component dialogParent(){
    return parentComponent != null ? parentComponent : this;
  }

  private Window dialogWindow(){
    Component parent = dialogParent();
    if (parent instanceof Window window){
      return window;
    }
    return SwingUtilities.getWindowAncestor(parent);
  }

  private void closeDialog(){
    if (closeHandler != null){
      closeHandler.run();
    } else {
      dispose();
    }
  }

  private void toastSuccess(String message){
    Toasts.success(dialogParent(), message);
  }

  private void toastInfo(String message){
    Toasts.info(dialogParent(), message);
  }

  private void toastError(String message){
    Toasts.error(dialogParent(), message);
  }
  private List<BillingLine> convertDocumentLines(List<DocumentLine> lines){
    List<BillingLine> result = new ArrayList<>();
    if (lines == null){
      return result;
    }
    for (DocumentLine line : lines){
      if (line == null){
        continue;
      }
      BillingLine billingLine = new BillingLine();
      billingLine.setId(UUID.randomUUID().toString());
      billingLine.setDesignation(line.getDesignation());
      billingLine.setQuantity(BigDecimal.valueOf(line.getQuantite()));
      String unit = line.getUnite();
      billingLine.setUnit(unit != null && !unit.isBlank() ? unit : "u");
      billingLine.setUnitPriceHt(BigDecimal.valueOf(line.getPrixUnitaireHT()));
      billingLine.setTotalHt(BigDecimal.valueOf(line.lineHT()));
      billingLine.setAutoGenerated(false);
      result.add(billingLine);
    }
    return result;
  }


  public void edit(Intervention intervention){
    suppressDirtyEvents = true;
    try {
      this.current = intervention == null ? new Intervention() : intervention;
      this.saved = false;

      resourcePicker.setContext(this.current);

      reloadAvailableTypes();
      loadTemplates();
      populateTypes();
      populateClients();
      loadResources();

      titleField.setText(s(current.getLabel()));
      addressField.setText(s(current.getAddress()));
      descriptionArea.setText(s(current.getDescription()));
      internalNoteArea.setText(s(current.getInternalNote()));
      closingNoteArea.setText(s(current.getClosingNote()));
      signatureByField.setText(s(current.getSignatureBy()));
      if (current.getSignatureAt() != null){
        signatureAtSpinner.setValue(toDate(current.getSignatureAt()));
      } else {
        signatureAtSpinner.setValue(new Date());
      }
      signatureBase64 = current.getSignaturePngBase64();
      updateSignaturePreview();

      LocalDateTime start = current.getDateHeureDebut();
      LocalDateTime end = current.getDateHeureFin();
      if (start == null){
        start = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
      }
      if (end == null){
        end = start.plusHours(4);
      }
      startSpinner.setValue(toDate(start));
      endSpinner.setValue(toDate(end));
      recomputeDurationFromEnd();

      updateResourcePickerWindow();
      resourcePicker.setSelectedResources(current.getResources());

      List<BillingLine> lines = current.getBillingLines();
      if (lines.isEmpty()){
        lines = convertDocumentLines(current.getQuoteDraft());
      }
      billingModel.setLines(lines);
      syncAutoBillingLinesWithResources();
      computeTotals();
      refreshConflictsAsync();
      updateQuoteBadge();
      refreshWorkflowState();
    } finally {
      suppressDirtyEvents = false;
    }
    undoStack.clear();
    redoStack.clear();
    skipNextTableSnapshot = false;
    lastLinesSnapshot = deepCopyLines(billingModel.getLines());
    markSaved("Brouillon chargé");
    loadHistory();
  }

  private void populateTypes(){
    List<InterventionType> types = new ArrayList<>(availableTypes);
    InterventionType currentType = current != null ? current.getType() : null;
    if (currentType != null && types.stream().noneMatch(t -> Objects.equals(t.getCode(), currentType.getCode()))){
      types.add(currentType);
    }
    DefaultComboBoxModel<InterventionType> model = new DefaultComboBoxModel<>();
    model.addElement(null);
    for (InterventionType type : types){
      model.addElement(type);
    }
    typeCombo.setModel(model);
    typeCombo.setRenderer(new DefaultListCellRenderer(){
      @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof InterventionType type){
          label.setText(type.toString());
          label.setIcon(IconRegistry.small(type.getIconKey()));
        } else {
          label.setText("(Aucun)");
          label.setIcon(null);
        }
        return label;
      }
    });
    for (ActionListener listener : typeCombo.getActionListeners()){
      typeCombo.removeActionListener(listener);
    }
    typeCombo.addActionListener(e -> markEdited());
    if (currentType != null){
      typeCombo.setSelectedItem(currentType);
    } else {
      typeCombo.setSelectedIndex(0);
    }
  }

  private void populateClients(){
    DefaultComboBoxModel<Client> model = new DefaultComboBoxModel<>();
    model.addElement(null);
    if (clientService != null){
      try {
        List<Client> clients = clientService.listClients();
        for (Client client : clients){
          model.addElement(client);
        }
      } catch (Exception ignored){}
    }
    clientCombo.setModel(model);
    clientCombo.setRenderer(new DefaultListCellRenderer(){
      @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Client client){
          label.setText(client.getName());
        } else {
          label.setText("(Aucun)");
        }
        return label;
      }
    });
    for (ActionListener listener : clientCombo.getActionListeners()){
      clientCombo.removeActionListener(listener);
    }
    clientCombo.addActionListener(e -> {
      updateContactsForClient();
      refreshWorkflowState();
      markEdited();
    });

    UUID clientId = current != null ? current.getClientId() : null;
    if (clientId != null){
      for (int i = 0; i < model.getSize(); i++){
        Client client = model.getElementAt(i);
        if (client != null && clientId.equals(client.getId())){
          clientCombo.setSelectedIndex(i);
          break;
        }
      }
    } else {
      clientCombo.setSelectedIndex(0);
    }
    updateContactsForClient();
    refreshWorkflowState();
  }

  private void updateContactsForClient(){
    UUID clientId = selectedClientId();
    List<Contact> baseSelection = new ArrayList<>(contactPicker.getSelectedContacts());
    if (baseSelection.isEmpty() && current != null && current.getContacts() != null){
      baseSelection.addAll(current.getContacts());
    }
    List<Contact> contacts = List.of();
    if (clientId != null && clientService != null){
      try {
        contacts = clientService.listContacts(clientId);
      } catch (Exception ignored){}
    }
    contactPicker.setContacts(contacts);
    if (clientId != null){
      List<Contact> filtered = new ArrayList<>();
      for (Contact contact : baseSelection){
        if (clientId.equals(contact.getClientId())){
          filtered.add(contact);
        }
      }
      contactPicker.setSelectedContacts(filtered);
    } else {
      contactPicker.setSelectedContacts(List.of());
    }
    refreshWorkflowState();
  }

  private UUID selectedClientId(){
    Object selected = clientCombo.getSelectedItem();
    if (selected instanceof Client client){
      return client.getId();
    }
    return null;
  }

  private void updateResourcePickerWindow(){
    if (resourcePicker == null){
      return;
    }
    LocalDateTime start = toLocalDateTime(startSpinner.getValue());
    LocalDateTime end = toLocalDateTime(endSpinner.getValue());
    resourcePicker.setPlannedWindow(start, end);
  }

  private void loadResources(){
    if (planningService == null){
      resourcePicker.setResources(List.of());
      return;
    }
    List<Resource> resources = planningService.listResources();
    resourcePicker.setResources(resources);
  }

  private void computeTotals(){
    BigDecimal totalHt = Money.round(billingModel.totalHt());
    BigDecimal totalTva = Money.round(totalHt.multiply(Money.vatRate()));
    BigDecimal totalTtc = Money.round(totalHt.add(totalTva));
    NumberFormat format = Money.currencyFormat();
    totalHtLabel.setText("Total HT : " + format.format(totalHt));
    totalTvaLabel.setText("TVA : " + format.format(totalTva));
    totalTtcLabel.setText("Total TTC : " + format.format(totalTtc));
  }

  private void updateQuoteBadge(){
    String text = "Aucun devis généré";
    if (current != null){
      String ref = current.getQuoteReference();
      if (ref == null || ref.isBlank()){
        ref = current.getQuoteNumber();
      }
      if (current.hasQuote() || (ref != null && !ref.isBlank())){
        if (ref == null || ref.isBlank()){
          text = "Devis lié";
        } else {
          text = "Devis : " + ref;
        }
      }
    }
    quoteStatusLabel.setText(text);
    quoteSummaryLabel.setText(text);
    boolean enable = current != null && current.hasQuote();
    openQuoteButton.setEnabled(enable);
    refreshWorkflowState();
  }

  public Intervention collect(){
    if (current == null){
      current = new Intervention();
    }
    String title = titleField.getText() != null ? titleField.getText().trim() : "";
    if (title.isEmpty()){
      throw new IllegalArgumentException("Le titre est obligatoire");
    }
    LocalDateTime start = toLocalDateTime(startSpinner.getValue());
    LocalDateTime end = toLocalDateTime(endSpinner.getValue());
    if (end.isBefore(start)){
      throw new IllegalArgumentException("La fin doit être postérieure au début");
    }
    current.setLabel(title);
    current.setType((InterventionType) typeCombo.getSelectedItem());
    current.setAddress(s(addressField.getText()));
    current.setDescription(descriptionArea.getText());
    current.setInternalNote(internalNoteArea.getText());
    current.setClosingNote(closingNoteArea.getText());
    current.setDateHeureDebut(start);
    current.setDateHeureFin(end);

    List<ResourceRef> refs = resourcePicker.getSelectedResourceRefs();
    current.setResources(refs);

    Client client = (Client) clientCombo.getSelectedItem();
    if (client != null){
      current.setClientId(client.getId());
      current.setClientName(client.getName());
    } else {
      current.setClientId(null);
      current.setClientName(null);
    }

    current.setContacts(contactPicker.getSelectedContacts());
    List<BillingLine> billingLines = billingModel.getLines();
    current.setBillingLines(billingLines);
    current.setQuoteDraft(QuoteGenerator.toDocumentLines(billingLines));
    String signer = signatureByField.getText();
    if (signer != null){
      signer = signer.trim();
    }
    current.setSignatureBy(signer == null || signer.isBlank() ? null : signer);
    current.setSignaturePngBase64(signatureBase64 != null && !signatureBase64.isBlank() ? signatureBase64 : null);
    LocalDateTime sigAt = null;
    Object rawSignatureAt = signatureAtSpinner.getValue();
    if (rawSignatureAt instanceof Date date){
      sigAt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    } else if (rawSignatureAt instanceof LocalDateTime ldt){
      sigAt = ldt;
    }
    if (current.getSignaturePngBase64() != null || (current.getSignatureBy() != null && !current.getSignatureBy().isBlank())){
      current.setSignatureAt(sigAt);
    } else {
      current.setSignatureAt(null);
    }
    applyCurrentAgency(current);
    refreshWorkflowState();
    return current;
  }

  private void applyCurrentAgency(Intervention intervention){
    if (intervention == null){
      return;
    }
    String id = AgencyContext.agencyId();
    String label = AgencyContext.agencyLabel();
    if (label != null && !label.isBlank()){
      intervention.setAgency(label);
    } else if (id != null && !id.isBlank()){
      intervention.setAgency(id);
    }
    if (id != null && !id.isBlank()){
      intervention.setAgencyId(id);
      invokeAgencySetter(intervention, "setAgencyCode", id);
    }
    if (label != null && !label.isBlank()){
      invokeAgencySetter(intervention, "setAgencyName", label);
    }
  }

  private void invokeAgencySetter(Intervention intervention, String method, String value){
    try {
      intervention.getClass().getMethod(method, String.class).invoke(intervention, value);
    } catch (Exception ignore){
    }
  }

  public boolean isSaved(){
    return saved;
  }

  public Intervention getIntervention(){
    return current;
  }

  public void setOnSave(Consumer<Intervention> onSave){
    this.onSaveCallback = onSave;
  }

  private LocalDateTime toLocalDateTime(Object value){
    if (value instanceof Date date){
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    if (value instanceof LocalDateTime ldt){
      return ldt;
    }
    return LocalDateTime.now();
  }

  private Date spinnerDate(JSpinner spinner){
    if (spinner == null){
      return null;
    }
    Object value = spinner.getValue();
    if (value instanceof Date date){
      return date;
    }
    if (value instanceof LocalDateTime ldt){
      return toDate(ldt);
    }
    return null;
  }

  private Date toDate(LocalDateTime value){
    Instant instant = value.atZone(ZoneId.systemDefault()).toInstant();
    return Date.from(instant);
  }

  private String s(String value){
    return value == null ? "" : value;
  }

  private List<InterventionType> defaultTypes(){
    List<InterventionType> types = new ArrayList<>();
    types.add(new InterventionType("LIFT", "Levage", "crane"));
    types.add(new InterventionType("TRANSPORT", "Transport", "truck"));
    types.add(new InterventionType("MANUT", "Manutention", "forklift"));
    return types;
  }

  private void reloadAvailableTypes(){
    availableTypes.clear();
    List<InterventionType> types = null;
    if (typeService != null){
      try {
        types = typeService.list();
      } catch (Exception ignore){}
    }
    if (types == null || types.isEmpty()){
      availableTypes.addAll(defaultTypes());
    } else {
      availableTypes.addAll(types);
    }
  }

}
