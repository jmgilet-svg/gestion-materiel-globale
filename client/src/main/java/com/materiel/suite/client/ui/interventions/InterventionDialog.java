package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.service.ClientService;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Fenêtre avancée pour créer ou modifier une intervention. */
public class InterventionDialog extends JDialog {
  private final PlanningService planningService;
  private final ClientService clientService;
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
  private final ResourceMultiPicker resourcePicker = new ResourceMultiPicker();
  private final ContactMultiPicker contactPicker = new ContactMultiPicker();
  private final QuoteTableModel quoteModel = new QuoteTableModel();
  private final JTable quoteTable = new JTable(quoteModel);

  private Intervention current;
  private boolean saved;

  public InterventionDialog(Window owner, PlanningService planningService, ClientService clientService, List<InterventionType> types){
    super(owner, "Intervention", ModalityType.APPLICATION_MODAL);
    this.planningService = planningService;
    this.clientService = clientService;
    if (types != null){
      availableTypes.addAll(types);
    } else {
      availableTypes.addAll(defaultTypes());
    }
    buildUI();
    setMinimumSize(new Dimension(980, 680));
    setLocationRelativeTo(owner);
  }

  private void buildUI(){
    setLayout(new BorderLayout(8, 8));
    add(buildHeader(), BorderLayout.NORTH);
    add(buildCenter(), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);
    configureSpinners();
    configureQuoteTable();
  }

  private void configureSpinners(){
    startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy HH:mm"));
    endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy HH:mm"));
  }

  private void configureQuoteTable(){
    quoteTable.setRowHeight(24);
    quoteTable.setFillsViewportHeight(true);
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

  private JComponent buildCenter(){
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    split.setResizeWeight(0.55);

    JTabbedPane leftTabs = new JTabbedPane();
    leftTabs.addTab("Ressources", IconRegistry.small("wrench"), resourcePicker);
    leftTabs.addTab("Contacts client", IconRegistry.small("user"), contactPicker);

    JPanel right = new JPanel(new BorderLayout(8, 8));
    JPanel notes = new JPanel(new GridLayout(3, 1, 6, 6));
    notes.add(panelWithLabel("Description", new JScrollPane(descriptionArea)));
    notes.add(panelWithLabel("Note interne", new JScrollPane(internalNoteArea)));
    notes.add(panelWithLabel("Note de fin", new JScrollPane(closingNoteArea)));
    right.add(notes, BorderLayout.NORTH);
    right.add(panelWithLabel("Pré-devis", new JScrollPane(quoteTable)), BorderLayout.CENTER);

    split.setLeftComponent(leftTabs);
    split.setRightComponent(right);
    return split;
  }

  private JPanel panelWithLabel(String title, JComponent component){
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(component, BorderLayout.CENTER);
    return panel;
  }

  private JComponent buildFooter(){
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton generate = new JButton("Générer depuis ressources", IconRegistry.small("file"));
    JButton save = new JButton("Enregistrer", IconRegistry.small("success"));
    JButton cancel = new JButton("Fermer");
    generate.addActionListener(e -> generateQuoteDraft());
    save.addActionListener(e -> onSave());
    cancel.addActionListener(e -> dispose());
    panel.add(generate);
    panel.add(save);
    panel.add(cancel);
    return panel;
  }

  private void onSave(){
    try {
      collect();
      saved = true;
      dispose();
      Toasts.success(this, "Intervention enregistrée");
    } catch (IllegalArgumentException ex){
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void generateQuoteDraft(){
    List<Resource> selected = resourcePicker.getSelectedResources();
    quoteModel.generateFromResources(selected);
  }

  public void edit(Intervention intervention){
    this.current = intervention == null ? new Intervention() : intervention;
    this.saved = false;

    populateTypes();
    populateClients();
    loadResources();

    titleField.setText(s(current.getLabel()));
    addressField.setText(s(current.getAddress()));
    descriptionArea.setText(s(current.getDescription()));
    internalNoteArea.setText(s(current.getInternalNote()));
    closingNoteArea.setText(s(current.getClosingNote()));

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

    quoteModel.setLines(current.getQuoteDraft());
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
        List<Client> clients = clientService.list();
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
    current.setQuoteDraft(quoteModel.getLines());
    return current;
  }

  public boolean isSaved(){
    return saved;
  }

  public Intervention getIntervention(){
    return current;
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

  private static class QuoteTableModel extends AbstractTableModel {
    private final List<DocumentLine> rows = new ArrayList<>();
    private final String[] columns = {"Désignation", "Qté", "PU HT", "Total HT"};

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return columns.length; }
    @Override public String getColumnName(int column){ return columns[column]; }

    @Override public Class<?> getColumnClass(int columnIndex){
      return switch(columnIndex){
        case 1, 2, 3 -> Double.class;
        default -> String.class;
      };
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex){
      return columnIndex == 0 || columnIndex == 1 || columnIndex == 2;
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      DocumentLine line = rows.get(rowIndex);
      return switch(columnIndex){
        case 0 -> line.getDesignation();
        case 1 -> line.getQuantite();
        case 2 -> line.getPrixUnitaireHT();
        case 3 -> line.lineHT();
        default -> "";
      };
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex){
      DocumentLine line = rows.get(rowIndex);
      try {
        switch (columnIndex){
          case 0 -> line.setDesignation(aValue != null ? aValue.toString() : "");
          case 1 -> line.setQuantite(parseDouble(aValue));
          case 2 -> line.setPrixUnitaireHT(parseDouble(aValue));
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
      } catch (NumberFormatException ignore){}
    }

    private double parseDouble(Object value){
      if (value instanceof Number number){
        return number.doubleValue();
      }
      return Double.parseDouble(value.toString());
    }

    public void setLines(List<DocumentLine> lines){
      rows.clear();
      if (lines != null){
        for (DocumentLine line : lines){
          if (line != null){
            DocumentLine copy = new DocumentLine();
            copy.setDesignation(line.getDesignation());
            copy.setQuantite(line.getQuantite());
            copy.setUnite(line.getUnite());
            copy.setPrixUnitaireHT(line.getPrixUnitaireHT());
            copy.setRemisePct(line.getRemisePct());
            copy.setTvaPct(line.getTvaPct());
            rows.add(copy);
          }
        }
      }
      fireTableDataChanged();
    }

    public List<DocumentLine> getLines(){
      List<DocumentLine> list = new ArrayList<>();
      for (DocumentLine line : rows){
        DocumentLine copy = new DocumentLine();
        copy.setDesignation(line.getDesignation());
        copy.setQuantite(line.getQuantite());
        copy.setUnite(line.getUnite());
        copy.setPrixUnitaireHT(line.getPrixUnitaireHT());
        copy.setRemisePct(line.getRemisePct());
        copy.setTvaPct(line.getTvaPct());
        list.add(copy);
      }
      return list;
    }

    public void generateFromResources(List<Resource> resources){
      rows.clear();
      if (resources != null){
        for (Resource resource : resources){
          if (resource == null){
            continue;
          }
          DocumentLine line = new DocumentLine();
          line.setDesignation(resource.getName());
          line.setQuantite(1d);
          line.setPrixUnitaireHT(0d);
          rows.add(line);
        }
      }
      fireTableDataChanged();
    }
  }
}
