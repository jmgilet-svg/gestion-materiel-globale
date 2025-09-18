package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.model.BillingLine;
import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.service.ClientService;
import com.materiel.suite.client.service.InterventionTypeService;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
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
  private final List<InterventionType> availableTypes = new ArrayList<>();

  private final JTextField titleField = new JTextField();
  private final JComboBox<InterventionType> typeCombo = new JComboBox<>();
  private final JComboBox<Client> clientCombo = new JComboBox<>();
  private final JTextField addressField = new JTextField();
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
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
  private final JButton saveButton = new JButton("Enregistrer", IconRegistry.small("success"));
  private final JButton regenerateBillingButton = new JButton("Depuis ressources", IconRegistry.small("refresh"));
  private final JButton addBillingLineButton = new JButton("Ajouter ligne", IconRegistry.small("plus"));
  private final JButton removeBillingLineButton = new JButton("Supprimer", IconRegistry.small("trash"));
  private final JButton generateQuoteButton = new JButton("Générer le devis", IconRegistry.small("file"));
  private final JButton importSignatureButton = new JButton("Importer PNG…");
  private final JButton clearSignatureButton = new JButton("Effacer");
  private final JButton fullscreenButton = new JButton("", IconRegistry.small("maximize"));
  private Rectangle previousBounds;
  private final boolean readOnly;

  private Intervention current;
  private boolean saved;
  private String signatureBase64;
  private Consumer<Intervention> onSaveCallback;

  public InterventionDialog(Window owner, PlanningService planningService, ClientService clientService, InterventionTypeService typeService){
    super(owner, "Intervention", ModalityType.APPLICATION_MODAL);
    this.planningService = planningService;
    this.clientService = clientService;
    this.typeService = typeService;
    this.resourcePicker = new ResourcePickerPanel(planningService);
    this.readOnly = !AccessControl.canEditInterventions();
    this.resourcePicker.setSelectionListener(this::onResourceSelectionChanged);
    saveButton.addActionListener(e -> onSave());
    regenerateBillingButton.addActionListener(e -> regenerateBillingFromResources());
    addBillingLineButton.addActionListener(e -> addManualBillingLine());
    removeBillingLineButton.addActionListener(e -> removeSelectedBillingLine());
    generateQuoteButton.addActionListener(e -> generateQuoteFromPrebilling());
    importSignatureButton.addActionListener(e -> importSignature());
    clearSignatureButton.addActionListener(e -> clearSignature());
    fullscreenButton.addActionListener(e -> toggleFullscreen());
    fullscreenButton.setFocusPainted(false);
    fullscreenButton.setToolTipText("Plein écran");
    reloadAvailableTypes();
    buildUI();
    setResizable(true);
    setMinimumSize(new Dimension(1180, 760));
    setLocationRelativeTo(owner);
    applyReadOnly();
  }

  private void buildUI(){
    setLayout(new BorderLayout(8, 8));
    add(buildToolbar(), BorderLayout.NORTH);
    add(buildTabs(), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);
    configureSpinners();
    configureBillingTable();
    billingModel.addTableModelListener(e -> computeTotals());
    totalHtLabel.setText("Total HT : " + currencyFormat.format(BigDecimal.ZERO));
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
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Général", IconRegistry.small("info"), buildGeneralTab());
    tabs.addTab("Intervention", IconRegistry.small("task"), buildInterventionTab());
    tabs.addTab("Facturation", IconRegistry.small("invoice"), buildFacturationTab());
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
    toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(totalHtLabel);
    panel.add(toolbar, BorderLayout.NORTH);
    panel.add(new JScrollPane(billingTable), BorderLayout.CENTER);
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
  }

  private void generateQuoteFromPrebilling(){
    billingModel.recalcAll();
    computeTotals();
    BigDecimal total = billingModel.totalHt();
    Toasts.success(this, "Devis généré (préparation) — Total HT : " + currencyFormat.format(total));
  }

  private void onResourceSelectionChanged(){
    if (readOnly){
      return;
    }
    syncAutoBillingLinesWithResources();
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
        line.setResourceId(key);
        line.setQuantity(BigDecimal.ONE);
      }
      if (resource.getName() != null && !resource.getName().isBlank()){
        if (line.getDesignation() == null || line.getDesignation().isBlank() || line.isAutoGenerated()){
          line.setDesignation(resource.getName());
        }
      }
      BigDecimal price = resource.getUnitPriceHt();
      if (price != null){
        line.setUnitPriceHt(price);
      }
      if (line.getUnit() == null || line.getUnit().isBlank()){
        line.setUnit("u");
      }
      updated.add(line);
    }
    billingModel.setLines(updated);
    computeTotals();
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

  private List<DocumentLine> toDocumentLines(List<BillingLine> lines){
    List<DocumentLine> result = new ArrayList<>();
    if (lines == null){
      return result;
    }
    for (BillingLine line : lines){
      if (line == null){
        continue;
      }
      DocumentLine doc = new DocumentLine();
      doc.setDesignation(line.getDesignation());
      BigDecimal quantity = line.getQuantity();
      doc.setQuantite(quantity != null ? quantity.doubleValue() : 0d);
      doc.setUnite(line.getUnit());
      BigDecimal price = line.getUnitPriceHt();
      doc.setPrixUnitaireHT(price != null ? price.doubleValue() : 0d);
      doc.setRemisePct(0d);
      doc.setTvaPct(0d);
      result.add(doc);
    }
    return result;
  }


  public void edit(Intervention intervention){
    this.current = intervention == null ? new Intervention() : intervention;
    this.saved = false;

    reloadAvailableTypes();
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

    resourcePicker.setSelectedResources(current.getResources());

    List<BillingLine> lines = current.getBillingLines();
    if (lines.isEmpty()){
      lines = convertDocumentLines(current.getQuoteDraft());
    }
    billingModel.setLines(lines);
    syncAutoBillingLinesWithResources();
    computeTotals();
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
    clientCombo.addActionListener(e -> updateContactsForClient());

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
  }

  private UUID selectedClientId(){
    Object selected = clientCombo.getSelectedItem();
    if (selected instanceof Client client){
      return client.getId();
    }
    return null;
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
    current.setQuoteDraft(toDocumentLines(billingLines));
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
