package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Sélecteur multi-ressources avec filtre par type et recherche. */
public class ResourceMultiPicker extends JPanel {
  private final JComboBox<String> typeFilter = new JComboBox<>();
  private final JTextField searchField = new JTextField();
  private final ResourceTableModel availableModel = new ResourceTableModel();
  private final JTable availableTable = new JTable(availableModel);
  private final ResourceTableModel selectedModel = new ResourceTableModel();
  private final JTable selectedTable = new JTable(selectedModel);

  private final Map<UUID, Resource> resourceIndex = new LinkedHashMap<>();
  private final List<Resource> allResources = new ArrayList<>();
  private final List<Resource> availableResources = new ArrayList<>();
  private final List<Resource> selectedResources = new ArrayList<>();

  public ResourceMultiPicker(){
    super(new BorderLayout(8, 8));
    buildNorth();
    buildTables();
    buildButtons();
    rebuildTypeFilter();
    refreshTables();
  }

  private void buildNorth(){
    JPanel north = new JPanel(new BorderLayout(6, 0));
    north.add(typeFilter, BorderLayout.WEST);
    north.add(searchField, BorderLayout.CENTER);
    add(north, BorderLayout.NORTH);
    typeFilter.addActionListener(e -> refreshTables());
    searchField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override public void update(DocumentEvent e){ refreshTables(); }
    });
  }

  private void buildTables(){
    configureTable(availableTable);
    configureTable(selectedTable);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        wrap("Disponibles", availableTable),
        wrap("Sélectionnées", selectedTable));
    split.setResizeWeight(0.5);
    add(split, BorderLayout.CENTER);
  }

  private void buildButtons(){
    JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
    JButton add = new JButton("→ Ajouter");
    JButton remove = new JButton("← Retirer");
    buttons.add(add);
    buttons.add(remove);
    add(buttons, BorderLayout.EAST);
    add.addActionListener(e -> move(availableTable, availableResources, selectedResources));
    remove.addActionListener(e -> move(selectedTable, selectedResources, availableResources));
  }

  private void configureTable(JTable table){
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setFillsViewportHeight(true);
    table.setRowHeight(24);
    table.getColumnModel().getColumn(0).setPreferredWidth(40);
    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer());
  }

  private JComponent wrap(String title, JTable table){
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(new JScrollPane(table), BorderLayout.CENTER);
    return panel;
  }

  private void move(JTable source, List<Resource> from, List<Resource> to){
    int[] rows = source.getSelectedRows();
    if (rows.length == 0){
      return;
    }
    List<Resource> moved = new ArrayList<>();
    for (int viewRow : rows){
      int modelRow = source.convertRowIndexToModel(viewRow);
      Resource r = (source.getModel() instanceof ResourceTableModel model)
          ? model.getAt(modelRow)
          : null;
      if (r != null){
        moved.add(r);
      }
    }
    if (moved.isEmpty()){
      return;
    }
    from.removeAll(moved);
    for (Resource r : moved){
      if (!containsResource(to, r.getId())){
        to.add(r);
      }
    }
    rebuildAvailableList();
    refreshTables();
  }

  private boolean containsResource(List<Resource> list, UUID id){
    if (id == null){
      return false;
    }
    return list.stream().anyMatch(r -> id.equals(r.getId()));
  }

  private void rebuildAvailableList(){
    availableResources.clear();
    for (Resource r : allResources){
      UUID id = r.getId();
      if (id != null && containsResource(selectedResources, id)){
        continue;
      }
      availableResources.add(r);
    }
  }

  private void rebuildTypeFilter(){
    Object previous = typeFilter.getSelectedItem();
    typeFilter.removeAllItems();
    typeFilter.addItem("Tous les types");
    LinkedHashSet<String> labels = new LinkedHashSet<>();
    for (Resource r : allResources){
      String label = typeLabel(r);
      if (!label.isBlank()){
        labels.add(label);
      }
    }
    for (String label : labels){
      typeFilter.addItem(label);
    }
    if (previous != null){
      typeFilter.setSelectedItem(previous);
    }
  }

  private void refreshTables(){
    String query = searchField.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    String selectedType = (String) typeFilter.getSelectedItem();
    List<Resource> filtered = availableResources.stream()
        .filter(r -> normalized.isBlank() || matches(r, normalized))
        .filter(r -> selectedType == null
            || "Tous les types".equals(selectedType)
            || typeLabel(r).equals(selectedType))
        .collect(Collectors.toList());
    availableModel.setRows(filtered);
    selectedModel.setRows(new ArrayList<>(selectedResources));
  }

  private boolean matches(Resource resource, String query){
    if (resource == null){
      return false;
    }
    String name = resource.getName();
    if (name != null && name.toLowerCase(Locale.ROOT).contains(query)){
      return true;
    }
    String type = typeLabel(resource);
    return !type.isBlank() && type.toLowerCase(Locale.ROOT).contains(query);
  }

  private static String typeLabel(Resource resource){
    ResourceType type = resource != null ? resource.getType() : null;
    if (type == null){
      return "";
    }
    if (type.getLabel() != null && !type.getLabel().isBlank()){
      return type.getLabel();
    }
    return type.getCode() != null ? type.getCode() : "";
  }

  private static String iconKey(Resource resource){
    ResourceType type = resource != null ? resource.getType() : null;
    return type != null ? type.getIcon() : null;
  }

  public void setResources(List<Resource> resources){
    resourceIndex.clear();
    allResources.clear();
    if (resources != null){
      for (Resource resource : resources){
        if (resource == null){
          continue;
        }
        if (resource.getId() != null){
          resourceIndex.put(resource.getId(), resource);
        }
        allResources.add(resource);
      }
    }
    rebuildAvailableList();
    rebuildTypeFilter();
    refreshTables();
  }

  public void setSelectedResources(List<ResourceRef> refs){
    selectedResources.clear();
    if (refs != null){
      for (ResourceRef ref : refs){
        if (ref == null){
          continue;
        }
        Resource resource = ref.getId() != null ? resourceIndex.get(ref.getId()) : null;
        if (resource == null){
          resource = new Resource(ref.getId(), ref.getName());
          if (ref.getIcon() != null && !ref.getIcon().isBlank()){
            ResourceType type = new ResourceType();
            type.setCode(ref.getIcon());
            type.setLabel(ref.getName());
            type.setIcon(ref.getIcon());
            resource.setType(type);
          }
        }
        selectedResources.add(resource);
      }
    }
    rebuildAvailableList();
    refreshTables();
  }

  public List<ResourceRef> getSelectedResourceRefs(){
    List<ResourceRef> refs = new ArrayList<>();
    for (Resource resource : selectedResources){
      UUID id = resource.getId();
      String name = resource.getName();
      String icon = iconKey(resource);
      refs.add(new ResourceRef(id, name, icon));
    }
    return refs;
  }

  public List<Resource> getSelectedResources(){
    return new ArrayList<>(selectedResources);
  }

  private static class ResourceTableModel extends AbstractTableModel {
    private final List<Resource> rows = new ArrayList<>();
    private final String[] columns = {"Icône", "Nom", "Type"};

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return columns.length; }
    @Override public String getColumnName(int column){ return columns[column]; }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      Resource resource = rows.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> iconKey(resource);
        case 1 -> resource != null ? resource.getName() : "";
        case 2 -> typeLabel(resource);
        default -> "";
      };
    }

    public Resource getAt(int index){
      return rows.get(index);
    }

    public void setRows(List<Resource> list){
      rows.clear();
      if (list != null){
        rows.addAll(list);
      }
      fireTableDataChanged();
    }
  }

  private static class IconCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setIcon(null);
      label.setText("");
      String key = value != null ? value.toString() : null;
      Icon icon = IconRegistry.small(key);
      if (icon != null){
        label.setIcon(icon);
      } else if (key != null && !key.isBlank()){
        label.setText(key);
      }
      return label;
    }
  }

  private abstract static class DocumentAdapter implements DocumentListener {
    @Override public void insertUpdate(DocumentEvent e){ update(e); }
    @Override public void removeUpdate(DocumentEvent e){ update(e); }
    @Override public void changedUpdate(DocumentEvent e){ update(e); }
    public abstract void update(DocumentEvent e);
  }
}
