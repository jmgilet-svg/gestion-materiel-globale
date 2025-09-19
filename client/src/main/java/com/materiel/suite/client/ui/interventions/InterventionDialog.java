package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionTemplate;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.QuoteV2;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.ClientService;
import com.materiel.suite.client.service.InterventionTypeService;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.SalesService;
import com.materiel.suite.client.service.TemplateService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

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
  private final JLabel quoteStatusLabel = new JLabel("Aucun devis généré");
  private final StepBar workflowStepBar = new StepBar();
  private final JTabbedPane tabs = new JTabbedPane();
  private final JLabel quoteSummaryLabel = new JLabel("Aucun devis généré");
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
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
  private Rectangle previousBounds;
  private final boolean readOnly;

  private Intervention current;
  private boolean saved;
  private String signatureBase64;
  private Consumer<Intervention> onSaveCallback;

  public InterventionDialog(Window owner,
                            PlanningService planningService,
                            ClientService clientService,
                            InterventionTypeService typeService,
                            TemplateService templateService){
    super(owner, "Intervention", ModalityType.APPLICATION_MODAL);
    this.planningService = planningService;
    this.clientService = clientService;
    this.typeService = typeService;
    this.templateService = templateService;
    this.resourcePicker = new ResourcePickerPanel(planningService);
    this.readOnly = !AccessControl.canEditInterventions();
    this.resourcePicker.setSelectionListener(this::onResourceSelectionChanged);
    this.contactPicker.setSelectionListener(this::refreshWorkflowState);
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
    fullscreenButton.addActionListener(e -> toggleFullscreen());
    fullscreenButton.setFocusPainted(false);
    fullscreenButton.setToolTipText("Plein écran");
    openQuoteButton.setEnabled(false);
    reloadAvailableTypes();
    buildUI();
    loadTemplates();
    installWorkflowHooks();
    refreshWorkflowState();
    setResizable(true);
    setMinimumSize(new Dimension(1180, 760));
    setLocationRelativeTo(owner);
    applyReadOnly();
  }

  private void buildUI(){
    setLayout(new BorderLayout(8, 8));
    add(buildNorthPanel(), BorderLayout.NORTH);
    add(buildTabs(), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);
    configureSpinners();
    configureBillingTable();
    billingModel.addTableModelListener(e -> {
      computeTotals();
      refreshWorkflowState();
    });
    totalHtLabel.setText("Total HT : " + currencyFormat.format(BigDecimal.ZERO));
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
    // Mapping étapes → onglets (ordre figé) : Intervention(1), Devis(3), Facturation(2)
    workflowStepBar.setOnNavigate(this::navigateToStep);
    ChangeListener spinnerListener = e -> {
      updateResourcePickerWindow();
      refreshWorkflowState();
    };
    startSpinner.addChangeListener(spinnerListener);
    endSpinner.addChangeListener(spinnerListener);
    titleField.getDocument().addDocumentListener(documentListener(this::refreshWorkflowState));
  }

  private void navigateToStep(int index){
    int target = switch (index){
      case 0 -> 1; // Intervention
      case 1 -> 3; // Devis
      case 2 -> 2; // Facturation
      default -> 1;
    };
    if (target < 0 || target >= tabs.getTabCount()){
      return;
    }
    tabs.setSelectedIndex(target);
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
    if (current != null){
      current.setGeneralDone(generalReady);
      current.setDetailsDone(detailsReady);
      current.setBillingReady(billingReady);
      // Étape courante (prochaine à faire) — ordre figé : Intervention → Devis → Facturation
      String stage = !detailsReady ? "INTERVENTION" : (!quoted ? "DEVIS" : "FACTURATION");
      current.setWorkflowStage(stage);
    }
    // Peindre la barre (activeIndex selon l'onglet : Intervention(1)→0, Devis(3)→1, Facturation(2)→2)
    int tabIdx = tabs.getSelectedIndex();
    int active = switch (tabIdx){
      case 1 -> 0;
      case 3 -> 1;
      case 2 -> 2;
      default -> 0;
    };
    workflowStepBar.setState(active, detailsReady, quoted, billingReady);
    quoteSummaryLabel.setText(quoteStatusLabel.getText());
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
    saveButton.setEnabled(false);
    billingTable.setEnabled(false);
    billingTable.setRowSelectionAllowed(false);
  }

  private void configureSpinners(){
    startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy HH:mm"));
    endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy HH:mm"));
    signatureAtSpinner.setEditor(new JSpinner.DateEditor(signatureAtSpinner, "dd/MM/yyyy HH:mm"));
  }

  private void configureBillingTable(){
    billingTable.setRowHeight(24);
    billingTable.setFillsViewportHeight(true);
    billingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    billingTable.setAutoCreateRowSorter(true);
    billingTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    billingTable.setDefaultRenderer(BigDecimal.class, rightRenderer);
    if (billingTable.getColumnModel().getColumnCount() > 0){
      billingTable.getColumnModel().getColumn(0).setMaxWidth(70);
      if (billingTable.getColumnModel().getColumnCount() > 2){
        billingTable.getColumnModel().getColumn(2).setPreferredWidth(80);
      }
      if (billingTable.getColumnModel().getColumnCount() > 3){
        billingTable.getColumnModel().getColumn(3).setPreferredWidth(80);
      }
      if (billingTable.getColumnModel().getColumnCount() > 4){
        billingTable.getColumnModel().getColumn(4).setPreferredWidth(100);
      }
      if (billingTable.getColumnModel().getColumnCount() > 5){
        billingTable.getColumnModel().getColumn(5).setPreferredWidth(120);
      }
    }
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

    gc.gridx = 0; gc.gridy = y; panel.add(new JLabel("Début"), gc);
    gc.gridx = 1; panel.add(startSpinner, gc);
    gc.gridx = 2; panel.add(new JLabel("Fin"), gc);
    gc.gridx = 3; panel.add(endSpinner, gc);
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
    tabs.addChangeListener(e -> refreshWorkflowState());
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
    toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(totalHtLabel);
    panel.add(toolbar, BorderLayout.NORTH);
    panel.add(new JScrollPane(billingTable), BorderLayout.CENTER);
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
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton cancel = new JButton("Fermer");
    cancel.addActionListener(e -> dispose());
    panel.add(saveButton);
    panel.add(cancel);
    return panel;
  }

  private void importSignature(){
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(this);
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File file = chooser.getSelectedFile();
    try {
      byte[] data = Files.readAllBytes(file.toPath());
      signatureBase64 = Base64.getEncoder().encodeToString(data);
      signatureAtSpinner.setValue(new Date());
      Toasts.success(this, "Signature importée");
    } catch (IOException ex){
      signatureBase64 = null;
      Toasts.error(this, "Impossible de lire le fichier sélectionné");
    }
    updateSignaturePreview();
  }

  private void clearSignature(){
    signatureBase64 = null;
    updateSignaturePreview();
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
      saved = true;
      dispose();
      Toasts.success(this, "Intervention enregistrée");
    } catch (IllegalArgumentException ex){
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    } catch (RuntimeException ex){
      String message = ex.getMessage();
      if (message == null || message.isBlank()){
        message = "Impossible d'enregistrer l'intervention.";
      }
      JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void regenerateBillingFromResources(){
    syncAutoBillingLinesWithResources();
    Toasts.info(this, "Lignes générées depuis les ressources");
    refreshWorkflowState();
  }

  private void applySelectedTemplate(){
    if (readOnly){
      return;
    }
    InterventionTemplate template = (InterventionTemplate) templateCombo.getSelectedItem();
    if (template == null){
      Toasts.info(this, "Aucun modèle sélectionné");
      return;
    }
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
    Toasts.success(this, name == null || name.isBlank() ? "Modèle appliqué" : "Modèle appliqué : " + name);
  }

  private void addManualBillingLine(){
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

  private void removeSelectedBillingLine(){
    int viewRow = billingTable.getSelectedRow();
    if (viewRow < 0){
      return;
    }
    int modelRow = billingTable.convertRowIndexToModel(viewRow);
    List<BillingLine> lines = billingModel.getLines();
    if (modelRow < 0 || modelRow >= lines.size()){
      return;
    }
    lines.remove(modelRow);
    billingModel.setLines(lines);
    computeTotals();
    refreshWorkflowState();
  }

  private void generateQuoteFromPrebilling(){
    billingModel.recalcAll();
    computeTotals();
    List<BillingLine> lines = billingModel.getLines();
    if (lines.isEmpty()){
      Toasts.info(this, "Aucune ligne de pré-devis à convertir");
      return;
    }
    if (current == null){
      Toasts.error(this, "Intervention introuvable");
      return;
    }
    if (current.hasQuote()){
      int confirm = JOptionPane.showConfirmDialog(this,
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
      Toasts.error(this, "Service devis indisponible");
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
      Toasts.success(this, ref == null || ref.isBlank() ? "Devis créé" : "Devis créé — " + ref);
      if (onSaveCallback != null){
        onSaveCallback.accept(current);
      } else if (planningService != null){
        planningService.saveIntervention(current);
      }
      refreshWorkflowState();
    } catch (Exception ex){
      Toasts.error(this, "Échec de génération du devis : " + ex.getMessage());
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
      Toasts.info(this, "Aucun devis lié à cette intervention.");
      return;
    }
    SalesService sales = ServiceLocator.sales();
    if (sales == null){
      Toasts.error(this, "Service devis indisponible");
      return;
    }
    UUID quoteId = current.getQuoteId();
    if (quoteId == null){
      Toasts.error(this, "Identifiant de devis indisponible.");
      return;
    }
    try {
      QuoteV2 quote = sales.getQuote(quoteId.toString());
      if (quote == null){
        Toasts.error(this, "Devis introuvable côté service.");
        return;
      }
      new QuotePreviewDialog(this, quote).setVisible(true);
    } catch (Exception ex){
      Toasts.error(this, "Impossible d'ouvrir le devis : " + ex.getMessage());
    }
  }

  private void onResourceSelectionChanged(){
    if (readOnly){
      return;
    }
    syncAutoBillingLinesWithResources();
    refreshWorkflowState();
  }

  private void syncAutoBillingLinesWithResources(){
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

    updateResourcePickerWindow();
    resourcePicker.setSelectedResources(current.getResources());

    List<BillingLine> lines = current.getBillingLines();
    if (lines.isEmpty()){
      lines = convertDocumentLines(current.getQuoteDraft());
    }
    billingModel.setLines(lines);
    syncAutoBillingLinesWithResources();
    computeTotals();
    updateQuoteBadge();
    refreshWorkflowState();
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
    BigDecimal total = billingModel.totalHt();
    totalHtLabel.setText("Total HT : " + currencyFormat.format(total));
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
    refreshWorkflowState();
    return current;
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
