package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.common.DateTimeField;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ResourceEditDialog extends JDialog {
  private final JTextField nameField = new JTextField(24);
  private final JTextField colorField = new JTextField(8);
  private final JTextArea notesArea = new JTextArea(5, 30);
  private final JComboBox<ResourceType> typeCombo = new JComboBox<>();
  private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
  private final JTextField tagsField = new JTextField(20);
  private final JTextArea weeklyArea = new JTextArea(4, 30);
  private final UnavailabilityTableModel tableModel = new UnavailabilityTableModel();
  private final JTable table = new JTable(tableModel);
  private final PlanningService service;
  private final Set<UUID> initialUnavailabilityIds = new HashSet<>();
  private Resource resource;
  private boolean saved;

  public ResourceEditDialog(Window owner, PlanningService service, Resource resource){
    super(owner, resource==null? "Nouvelle ressource" : "Modifier la ressource", ModalityType.APPLICATION_MODAL);
    this.service = service;
    this.resource = resource!=null? resource : new Resource();
    buildUI();
    loadTypes();
    bindValues();
    pack();
    setLocationRelativeTo(owner);
  }

  public boolean isSaved(){ return saved; }

  private void buildUI(){
    notesArea.setLineWrap(true);
    notesArea.setWrapStyleWord(true);
    weeklyArea.setLineWrap(true);
    weeklyArea.setWrapStyleWord(true);
    typeCombo.setEditable(true);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4,4,4,4);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0; gc.gridy = 0; form.add(new JLabel("Nom"), gc);
    gc.gridx = 1; form.add(nameField, gc);
    gc.gridx = 0; gc.gridy++; form.add(new JLabel("Type"), gc);
    gc.gridx = 1; form.add(typeCombo, gc);
    gc.gridx = 0; gc.gridy++; form.add(new JLabel("Couleur (hex)"), gc);
    gc.gridx = 1; form.add(colorField, gc);
    gc.gridx = 0; gc.gridy++; gc.anchor = GridBagConstraints.NORTHWEST; form.add(new JLabel("Notes"), gc);
    gc.gridx = 1; form.add(new JScrollPane(notesArea), gc);
    // === CRM-INJECT BEGIN: resource-editor-advanced-fields ===
    gc.gridx = 0; gc.gridy++; gc.anchor = GridBagConstraints.WEST; form.add(new JLabel("Capacité"), gc);
    gc.gridx = 1; form.add(capacitySpinner, gc);
    gc.gridx = 0; gc.gridy++; form.add(new JLabel("Tags"), gc);
    gc.gridx = 1; form.add(tagsField, gc);
    gc.gridx = 0; gc.gridy++; gc.anchor = GridBagConstraints.NORTHWEST; form.add(new JLabel("Indisponibilités récurrentes"), gc);
    gc.gridx = 1; form.add(new JScrollPane(weeklyArea), gc);
    // === CRM-INJECT END ===

    JPanel unavailabilityPanel = new JPanel(new BorderLayout(4,4));
    unavailabilityPanel.setBorder(BorderFactory.createTitledBorder("Indisponibilités ponctuelles"));
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setFillsViewportHeight(true);
    unavailabilityPanel.add(new JScrollPane(table), BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    JButton add = new JButton("Ajouter…");
    JButton remove = new JButton("Supprimer");
    actions.add(add); actions.add(remove);
    unavailabilityPanel.add(actions, BorderLayout.NORTH);
    add.addActionListener(e -> onAdd());
    remove.addActionListener(e -> onRemove());

    JButton save = new JButton("Enregistrer");
    JButton cancel = new JButton("Annuler");
    save.addActionListener(e -> onSave());
    cancel.addActionListener(e -> dispose());

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    south.add(cancel); south.add(save);

    JPanel root = new JPanel(new BorderLayout(8,8));
    root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
    root.add(form, BorderLayout.NORTH);
    root.add(unavailabilityPanel, BorderLayout.CENTER);
    root.add(south, BorderLayout.SOUTH);
    setContentPane(root);
  }

  private void loadTypes(){
    DefaultComboBoxModel<ResourceType> model = new DefaultComboBoxModel<>();
    try {
      List<ResourceType> types = service.listResourceTypes();
      for (ResourceType t : types){
        if (t!=null) model.addElement(t);
      }
    } catch(Exception ignore){}
    typeCombo.setModel(model);
  }

  private void bindValues(){
    nameField.setText(resource.getName()!=null? resource.getName():"");
    colorField.setText(resource.getColor()!=null? resource.getColor():"");
    notesArea.setText(resource.getNotes()!=null? resource.getNotes():"");
    ResourceType type = resource.getType();
    if (type!=null){
      ensureTypeInModel(type);
      typeCombo.setSelectedItem(type);
    } else {
      typeCombo.setSelectedItem(null);
    }
    // === CRM-INJECT BEGIN: resource-editor-advanced-save ===
    Integer cap = resource.getCapacity();
    if (cap==null || cap<1) cap = 1;
    capacitySpinner.setValue(cap);
    tagsField.setText(resource.getTags()!=null? resource.getTags():"");
    weeklyArea.setText(resource.getWeeklyUnavailability()!=null? resource.getWeeklyUnavailability():"");
    // === CRM-INJECT END ===

    List<Unavailability> list;
    if (resource.getId()!=null){
      try {
        list = service.listResourceUnavailabilities(resource.getId());
      } catch(Exception e){
        list = new ArrayList<>(resource.getUnavailabilities());
      }
    } else {
      list = new ArrayList<>(resource.getUnavailabilities());
    }
    tableModel.setRows(list);
    initialUnavailabilityIds.clear();
    for (Unavailability u : list){
      if (u.getId()!=null) initialUnavailabilityIds.add(u.getId());
    }
  }

  private void ensureTypeInModel(ResourceType type){
    ComboBoxModel<ResourceType> model = typeCombo.getModel();
    for (int i=0;i<model.getSize();i++){
      ResourceType t = model.getElementAt(i);
      if (t!=null && t.equals(type)) return;
    }
    ((DefaultComboBoxModel<ResourceType>)model).addElement(type);
  }

  private void onAdd(){
    DateTimeField startField = new DateTimeField();
    DateTimeField endField = new DateTimeField();
    JTextField reason = new JTextField(20);

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4,4,4,4);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0; gc.gridy = 0; panel.add(new JLabel("Début"), gc);
    gc.gridx = 1; panel.add(startField, gc);
    gc.gridx = 0; gc.gridy = 1; panel.add(new JLabel("Fin"), gc);
    gc.gridx = 1; panel.add(endField, gc);
    gc.gridx = 0; gc.gridy = 2; panel.add(new JLabel("Motif"), gc);
    gc.gridx = 1; panel.add(reason, gc);

    int res = JOptionPane.showConfirmDialog(this, panel, "Ajouter une indisponibilité", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (res==JOptionPane.OK_OPTION){
      LocalDateTime start = startField.getDateTime();
      LocalDateTime end = endField.getDateTime();
      if (end.isBefore(start)){
        JOptionPane.showMessageDialog(this, "La fin doit être postérieure au début.", "Validation", JOptionPane.WARNING_MESSAGE);
        return;
      }
      tableModel.add(new Unavailability(null, start, end, reason.getText().trim()));
    }
  }

  private void onRemove(){
    int row = table.getSelectedRow();
    if (row<0) return;
    tableModel.remove(row);
  }

  private void onSave(){
    try {
      applyAndSave();
      saved = true;
      dispose();
    } catch(Exception e){
      JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement : "+e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void applyAndSave(){
    resource.setName(nameField.getText().trim());
    resource.setColor(colorField.getText().trim());
    resource.setNotes(notesArea.getText());
    ResourceType selectedType = resolveType();
    resource.setType(selectedType);
    // === CRM-INJECT BEGIN: resource-editor-advanced-save ===
    Object capVal = capacitySpinner.getValue();
    int cap = 1;
    if (capVal instanceof Number n) cap = Math.max(1, n.intValue());
    else {
      try { cap = Math.max(1, Integer.parseInt(String.valueOf(capVal))); } catch(Exception ignore){}
    }
    resource.setCapacity(cap);
    resource.setTags(tagsField.getText().trim());
    resource.setWeeklyUnavailability(weeklyArea.getText());
    // === CRM-INJECT END ===

    List<Unavailability> desired = tableModel.getRows();
    resource.setUnavailabilities(desired);
    resource = service.saveResource(resource);

    UUID resourceId = resource.getId();
    if (resourceId!=null){
      List<Unavailability> afterSave = new ArrayList<>(resource.getUnavailabilities()!=null? resource.getUnavailabilities(): Collections.emptyList());
      if (!afterSave.isEmpty()){
        desired = afterSave;
        tableModel.setRows(afterSave);
      }
      Set<UUID> currentIds = new HashSet<>();
      for (Unavailability u : desired){
        if (u.getId()!=null) currentIds.add(u.getId());
      }
      for (UUID id : new ArrayList<>(initialUnavailabilityIds)){
        if (!currentIds.contains(id)){
          service.deleteUnavailability(resourceId, id);
        }
      }
      List<Unavailability> persisted = new ArrayList<>();
      for (Unavailability u : desired){
        if (u.getId()==null){
          Unavailability created = service.addUnavailability(resourceId, u);
          if (created!=null){
            persisted.add(created);
          } else {
            persisted.add(u);
          }
        } else {
          persisted.add(u);
        }
      }
      List<Unavailability> refreshed = persisted;
      try {
        refreshed = service.listResourceUnavailabilities(resourceId);
      } catch(Exception ignore){}
      resource.setUnavailabilities(refreshed);
      tableModel.setRows(refreshed);
      initialUnavailabilityIds.clear();
      for (Unavailability u : refreshed){
        if (u.getId()!=null) initialUnavailabilityIds.add(u.getId());
      }
    }
  }

  private ResourceType resolveType(){
    Object selected = typeCombo.getSelectedItem();
    if (selected instanceof ResourceType t) return t;
    if (selected!=null){
      String txt = selected.toString().trim();
      if (!txt.isEmpty()) return new ResourceType(txt, txt);
    }
    return null;
  }
}
