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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Formulaire d'édition inline d'une ressource.
 */
public class ResourceEditorPanel extends JPanel {
  private static final String CARD_EMPTY = "empty";
  private static final String CARD_FORM = "form";

  private final PlanningService service;
  private final JTextField nameField = new JTextField(24);
  private final JTextField colorField = new JTextField(8);
  private final JTextArea notesArea = new JTextArea(5, 30);
  private final JComboBox<ResourceType> typeCombo = new JComboBox<>();
  private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
  private final JTextField tagsField = new JTextField(20);
  private final JTextArea weeklyArea = new JTextArea(4, 30);
  private final UnavailabilityTableModel tableModel = new UnavailabilityTableModel();
  private final JTable table = new JTable(tableModel);
  private final JButton addUnavailabilityBtn = new JButton("Ajouter…");
  private final JButton removeUnavailabilityBtn = new JButton("Supprimer");
  private final JButton saveBtn = new JButton("Enregistrer");
  private final JButton resetBtn = new JButton("Réinitialiser");

  private final Set<UUID> initialUnavailabilityIds = new HashSet<>();
  private Consumer<Resource> onSaved;
  private Resource current;

  public ResourceEditorPanel(PlanningService service) {
    super(new CardLayout());
    this.service = service;

    JPanel empty = new JPanel(new BorderLayout());
    JLabel emptyLabel = new JLabel("Sélectionnez une ressource ou créez-en une nouvelle.", SwingConstants.CENTER);
    empty.add(emptyLabel, BorderLayout.CENTER);

    add(empty, CARD_EMPTY);
    add(buildForm(), CARD_FORM);

    reloadTypes();
    showEmpty();
  }

  private JPanel buildForm() {
    JPanel root = new JPanel(new BorderLayout(8, 8));
    root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    notesArea.setLineWrap(true);
    notesArea.setWrapStyleWord(true);
    weeklyArea.setLineWrap(true);
    weeklyArea.setWrapStyleWord(true);
    typeCombo.setEditable(true);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4, 4, 4, 4);
    gc.anchor = GridBagConstraints.WEST;

    gc.gridx = 0;
    gc.gridy = 0;
    form.add(new JLabel("Nom"), gc);
    gc.gridx = 1;
    form.add(nameField, gc);

    gc.gridx = 0;
    gc.gridy++;
    form.add(new JLabel("Type"), gc);
    gc.gridx = 1;
    form.add(typeCombo, gc);

    gc.gridx = 0;
    gc.gridy++;
    form.add(new JLabel("Couleur (hex)"), gc);
    gc.gridx = 1;
    form.add(colorField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.NORTHWEST;
    form.add(new JLabel("Notes"), gc);
    gc.gridx = 1;
    form.add(new JScrollPane(notesArea), gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.WEST;
    form.add(new JLabel("Capacité"), gc);
    gc.gridx = 1;
    form.add(capacitySpinner, gc);

    gc.gridx = 0;
    gc.gridy++;
    form.add(new JLabel("Tags"), gc);
    gc.gridx = 1;
    form.add(tagsField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.NORTHWEST;
    form.add(new JLabel("Indisponibilités récurrentes"), gc);
    gc.gridx = 1;
    form.add(new JScrollPane(weeklyArea), gc);

    JPanel unavailabilityPanel = new JPanel(new BorderLayout(4, 4));
    unavailabilityPanel.setBorder(BorderFactory.createTitledBorder("Indisponibilités ponctuelles"));
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setFillsViewportHeight(true);
    unavailabilityPanel.add(new JScrollPane(table), BorderLayout.CENTER);

    JPanel unavailabilityActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    unavailabilityActions.add(addUnavailabilityBtn);
    unavailabilityActions.add(removeUnavailabilityBtn);
    unavailabilityPanel.add(unavailabilityActions, BorderLayout.NORTH);

    addUnavailabilityBtn.addActionListener(e -> onAddUnavailability());
    removeUnavailabilityBtn.addActionListener(e -> onRemoveUnavailability());

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    south.add(resetBtn);
    south.add(saveBtn);
    saveBtn.addActionListener(e -> onSave());
    resetBtn.addActionListener(e -> rebindValues());

    root.add(form, BorderLayout.NORTH);
    root.add(unavailabilityPanel, BorderLayout.CENTER);
    root.add(south, BorderLayout.SOUTH);
    return root;
  }

  public void setOnSaved(Consumer<Resource> onSaved) {
    this.onSaved = onSaved;
  }

  public void reloadTypes() {
    ResourceType selected = current != null ? current.getType() : (ResourceType) typeCombo.getSelectedItem();
    DefaultComboBoxModel<ResourceType> model = new DefaultComboBoxModel<>();
    if (service != null) {
      try {
        List<ResourceType> types = service.listResourceTypes();
        for (ResourceType t : types) {
          if (t != null) {
            model.addElement(t);
          }
        }
      } catch (Exception ignore) {
      }
    }
    typeCombo.setModel(model);
    if (selected != null) {
      ensureTypeInModel(selected);
      typeCombo.setSelectedItem(selected);
    } else {
      typeCombo.setSelectedItem(null);
    }
  }

  public void edit(Resource resource) {
    if (resource == null) {
      current = null;
      initialUnavailabilityIds.clear();
      tableModel.setRows(Collections.emptyList());
      showEmpty();
      return;
    }
    current = cloneResource(resource);
    showForm();
    rebindValues();
  }

  public void clear() {
    edit(null);
  }

  private void rebindValues() {
    if (current == null) {
      nameField.setText("");
      colorField.setText("");
      notesArea.setText("");
      capacitySpinner.setValue(1);
      tagsField.setText("");
      weeklyArea.setText("");
      tableModel.setRows(Collections.emptyList());
      typeCombo.setSelectedItem(null);
      return;
    }

    nameField.setText(current.getName() != null ? current.getName() : "");
    colorField.setText(current.getColor() != null ? current.getColor() : "");
    notesArea.setText(current.getNotes() != null ? current.getNotes() : "");
    Integer capacity = current.getCapacity();
    if (capacity == null || capacity < 1) {
      capacity = 1;
    }
    capacitySpinner.setValue(capacity);
    tagsField.setText(current.getTags() != null ? current.getTags() : "");
    weeklyArea.setText(current.getWeeklyUnavailability() != null ? current.getWeeklyUnavailability() : "");

    ResourceType type = current.getType();
    if (type != null) {
      ensureTypeInModel(type);
      typeCombo.setSelectedItem(type);
    } else {
      typeCombo.setSelectedItem(null);
    }

    tableModel.setRows(current.getUnavailabilities());
    initialUnavailabilityIds.clear();
    for (Unavailability u : current.getUnavailabilities()) {
      if (u.getId() != null) {
        initialUnavailabilityIds.add(u.getId());
      }
    }
  }

  private void onAddUnavailability() {
    DateTimeField startField = new DateTimeField();
    DateTimeField endField = new DateTimeField();
    JTextField reason = new JTextField(20);

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4, 4, 4, 4);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("Début"), gc);
    gc.gridx = 1;
    panel.add(startField, gc);
    gc.gridx = 0;
    gc.gridy = 1;
    panel.add(new JLabel("Fin"), gc);
    gc.gridx = 1;
    panel.add(endField, gc);
    gc.gridx = 0;
    gc.gridy = 2;
    panel.add(new JLabel("Motif"), gc);
    gc.gridx = 1;
    panel.add(reason, gc);

    int res = JOptionPane.showConfirmDialog(
        SwingUtilities.getWindowAncestor(this),
        panel,
        "Ajouter une indisponibilité",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (res == JOptionPane.OK_OPTION) {
      LocalDateTime start = startField.getDateTime();
      LocalDateTime end = endField.getDateTime();
      if (end.isBefore(start)) {
        JOptionPane.showMessageDialog(this, "La fin doit être postérieure au début.", "Validation", JOptionPane.WARNING_MESSAGE);
        return;
      }
      tableModel.add(new Unavailability(null, start, end, reason.getText().trim()));
    }
  }

  private void onRemoveUnavailability() {
    int row = table.getSelectedRow();
    if (row < 0) {
      return;
    }
    int modelRow = table.convertRowIndexToModel(row);
    tableModel.remove(modelRow);
  }

  private void onSave() {
    if (service == null) {
      JOptionPane.showMessageDialog(this, "Service indisponible", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }
    if (current == null) {
      current = new Resource();
    }
    try {
      applyAndSave();
      if (onSaved != null) {
        onSaved.accept(current);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void applyAndSave() {
    current.setName(nameField.getText().trim());
    current.setColor(colorField.getText().trim());
    current.setNotes(notesArea.getText());
    current.setType(resolveType());

    Object capVal = capacitySpinner.getValue();
    int cap = 1;
    if (capVal instanceof Number n) {
      cap = Math.max(1, n.intValue());
    } else {
      try {
        cap = Math.max(1, Integer.parseInt(String.valueOf(capVal)));
      } catch (Exception ignore) {
      }
    }
    current.setCapacity(cap);
    current.setTags(tagsField.getText().trim());
    current.setWeeklyUnavailability(weeklyArea.getText());

    List<Unavailability> desired = tableModel.getRows();
    current.setUnavailabilities(desired);

    current = service.saveResource(current);
    UUID resourceId = current.getId();
    if (resourceId != null) {
      syncUnavailabilities(desired, resourceId);
    }
    rebindValues();
  }

  private void syncUnavailabilities(List<Unavailability> desired, UUID resourceId) {
    List<Unavailability> afterSave = new ArrayList<>(current.getUnavailabilities() != null ? current.getUnavailabilities() : Collections.emptyList());
    if (!afterSave.isEmpty()) {
      desired = afterSave;
      tableModel.setRows(afterSave);
    }

    Set<UUID> currentIds = new HashSet<>();
    for (Unavailability u : desired) {
      if (u.getId() != null) {
        currentIds.add(u.getId());
      }
    }

    for (UUID id : new ArrayList<>(initialUnavailabilityIds)) {
      if (!currentIds.contains(id)) {
        service.deleteUnavailability(resourceId, id);
      }
    }

    List<Unavailability> persisted = new ArrayList<>();
    for (Unavailability u : desired) {
      if (u.getId() == null) {
        Unavailability created = service.addUnavailability(resourceId, u);
        if (created != null) {
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
    } catch (Exception ignore) {
    }
    current.setUnavailabilities(refreshed);
    tableModel.setRows(refreshed);
    initialUnavailabilityIds.clear();
    for (Unavailability u : refreshed) {
      if (u.getId() != null) {
        initialUnavailabilityIds.add(u.getId());
      }
    }
  }

  private ResourceType resolveType() {
    Object selected = typeCombo.getSelectedItem();
    if (selected instanceof ResourceType type) {
      return type;
    }
    if (selected != null) {
      String value = selected.toString().trim();
      if (!value.isEmpty()) {
        return new ResourceType(value, value);
      }
    }
    return null;
  }

  private Resource cloneResource(Resource source) {
    Resource copy = new Resource();
    copy.setId(source.getId());
    copy.setName(source.getName());
    copy.setType(source.getType());
    copy.setColor(source.getColor());
    copy.setNotes(source.getNotes());
    copy.setCapacity(source.getCapacity());
    copy.setTags(source.getTags());
    copy.setWeeklyUnavailability(source.getWeeklyUnavailability());
    copy.setUnavailabilities(new ArrayList<>(source.getUnavailabilities()));
    return copy;
  }

  private void ensureTypeInModel(ResourceType type) {
    ComboBoxModel<ResourceType> model = typeCombo.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      ResourceType t = model.getElementAt(i);
      if (Objects.equals(t, type)) {
        return;
      }
    }
    ((DefaultComboBoxModel<ResourceType>) model).addElement(type);
  }

  private void showEmpty() {
    ((CardLayout) getLayout()).show(this, CARD_EMPTY);
  }

  private void showForm() {
    ((CardLayout) getLayout()).show(this, CARD_FORM);
  }
}
