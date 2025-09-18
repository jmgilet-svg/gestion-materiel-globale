package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.ui.resources.ResourcePriceEditorDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Sélecteur ergonomique de ressources avec cases à cocher, filtre par type et recherche textuelle. */
public class ResourcePickerPanel extends JPanel {
  private static final String ALL_TYPES = "Tous les types";

  private final PlanningService planningService;
  private final JComboBox<String> typeFilter = new JComboBox<>();
  private final JTextField searchField = new JTextField();
  private final JButton selectAllButton = new JButton("Tout");
  private final JButton clearAllButton = new JButton("Aucun");
  private final JButton editPriceButton = new JButton("Tarif…");
  private final ResourceTableModel model = new ResourceTableModel();
  private final JTable table = new JTable(model);
  private final NumberFormat priceFormat = NumberFormat.getNumberInstance(Locale.FRANCE);

  private final List<Resource> allResources = new ArrayList<>();
  private final Map<UUID, Resource> resourceIndex = new LinkedHashMap<>();
  private final LinkedHashMap<String, ResourceRef> selectedRefs = new LinkedHashMap<>();
  private boolean readOnly;
  private Runnable selectionListener;

  public ResourcePickerPanel(){
    this(ServiceFactory.planning());
  }

  public ResourcePickerPanel(PlanningService planningService){
    super(new BorderLayout(8, 8));
    this.planningService = planningService;
    priceFormat.setMaximumFractionDigits(2);
    priceFormat.setMinimumFractionDigits(0);
    buildNorth();
    buildTable();
    installListeners();
    rebuildTypeFilter();
    applyFilter();
  }

  private void buildNorth(){
    JPanel north = new JPanel(new BorderLayout(6, 0));

    JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    filters.add(typeFilter);
    filters.add(new JLabel(IconRegistry.small("search")));
    searchField.setColumns(18);
    filters.add(searchField);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    actions.add(editPriceButton);
    actions.add(selectAllButton);
    actions.add(clearAllButton);

    north.add(filters, BorderLayout.CENTER);
    north.add(actions, BorderLayout.EAST);
    add(north, BorderLayout.NORTH);
  }

  private void buildTable(){
    table.setFillsViewportHeight(true);
    table.setRowHeight(24);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getColumnModel().getColumn(0).setMaxWidth(50);
    table.getColumnModel().getColumn(1).setMaxWidth(36);
    table.getColumnModel().getColumn(1).setCellRenderer(new IconRenderer());
    if (table.getColumnModel().getColumnCount() > 4){
      table.getColumnModel().getColumn(4).setPreferredWidth(90);
      table.getColumnModel().getColumn(4).setCellRenderer(new PriceRenderer());
    }
    add(new JScrollPane(table), BorderLayout.CENTER);
  }

  private void installListeners(){
    typeFilter.addActionListener(e -> applyFilter());
    searchField.getDocument().addDocumentListener(new DocumentAdapter(){
      @Override public void update(DocumentEvent e){ applyFilter(); }
    });
    selectAllButton.addActionListener(e -> selectFiltered(true));
    clearAllButton.addActionListener(e -> selectFiltered(false));
    editPriceButton.addActionListener(e -> {
      if (readOnly){
        Toasts.info(ResourcePickerPanel.this, "Lecture seule");
        return;
      }
      if (!AccessControl.canEditResources()){
        Toasts.error(ResourcePickerPanel.this, "Droit requis : édition Ressources");
        return;
      }
      openPriceDialog();
    });
    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()){
        updateEditPriceButtonState();
      }
    });
    updateEditPriceButtonState();
  }

  private void selectFiltered(boolean select){
    if (readOnly){
      return;
    }
    List<Resource> visible = model.rows();
    if (visible.isEmpty()){
      return;
    }
    for (Resource resource : visible){
      if (resource == null){
        continue;
      }
      String key = keyOf(resource);
      if (select){
        ResourceRef ref = toRef(resource);
        if (ref != null){
          selectedRefs.put(key, ref);
        }
      } else {
        selectedRefs.remove(key);
      }
    }
    model.refreshAll();
    notifySelectionChanged();
  }

  public void setResources(List<Resource> resources){
    resourceIndex.clear();
    allResources.clear();
    if (resources != null){
      for (Resource resource : resources){
        if (resource == null){
          continue;
        }
        allResources.add(resource);
        if (resource.getId() != null){
          resourceIndex.put(resource.getId(), resource);
        }
      }
    }
    ensureSelectedResourcesPresent();
    rebuildTypeFilter();
    applyFilter();
  }

  public void setSelectedResources(List<ResourceRef> refs){
    selectedRefs.clear();
    if (refs != null){
      for (ResourceRef ref : refs){
        if (ref == null){
          continue;
        }
        ResourceRef copy = new ResourceRef(ref.getId(), ref.getName(), ref.getIcon());
        selectedRefs.put(keyOf(copy), copy);
      }
    }
    ensureSelectedResourcesPresent();
    rebuildTypeFilter();
    applyFilter();
  }

  public void setReadOnly(boolean readOnly){
    this.readOnly = readOnly;
    typeFilter.setEnabled(!readOnly);
    searchField.setEditable(!readOnly);
    searchField.setEnabled(!readOnly);
    selectAllButton.setEnabled(!readOnly);
    clearAllButton.setEnabled(!readOnly);
    table.setEnabled(!readOnly);
    table.setRowSelectionAllowed(!readOnly);
    if (readOnly){
      table.clearSelection();
    }
    updateEditPriceButtonState();
  }

  public void setSelectionListener(Runnable listener){
    this.selectionListener = listener;
  }

  private void notifySelectionChanged(){
    if (selectionListener != null){
      selectionListener.run();
    }
  }

  public List<Resource> getSelectedResources(){
    List<Resource> list = new ArrayList<>();
    for (ResourceRef ref : selectedRefs.values()){
      if (ref == null){
        continue;
      }
      Resource resource = ref.getId() != null ? resourceIndex.get(ref.getId()) : findByName(ref.getName());
      if (resource != null){
        list.add(resource);
      } else {
        list.add(createPlaceholder(ref));
      }
    }
    return list;
  }

  public List<ResourceRef> getSelectedResourceRefs(){
    return new ArrayList<>(selectedRefs.values());
  }

  private void ensureSelectedResourcesPresent(){
    for (ResourceRef ref : selectedRefs.values()){
      if (ref == null){
        continue;
      }
      Resource existing = ref.getId() != null ? resourceIndex.get(ref.getId()) : findByName(ref.getName());
      if (existing == null){
        Resource placeholder = createPlaceholder(ref);
        allResources.add(placeholder);
        if (placeholder.getId() != null){
          resourceIndex.put(placeholder.getId(), placeholder);
        }
      }
    }
  }

  private Resource createPlaceholder(ResourceRef ref){
    Resource resource = new Resource(ref.getId(), ref.getName());
    if (ref.getIcon() != null && !ref.getIcon().isBlank()){
      ResourceType type = new ResourceType();
      type.setIcon(ref.getIcon());
      type.setLabel(ref.getName());
      resource.setType(type);
    }
    return resource;
  }

  private Resource findByName(String name){
    if (name == null){
      return null;
    }
    for (Resource resource : allResources){
      if (resource != null && resource.getId() == null && Objects.equals(name, resource.getName())){
        return resource;
      }
    }
    return null;
  }

  private void rebuildTypeFilter(){
    Object previous = typeFilter.getSelectedItem();
    typeFilter.removeAllItems();
    typeFilter.addItem(ALL_TYPES);
    LinkedHashSet<String> labels = new LinkedHashSet<>();
    for (Resource resource : allResources){
      String label = typeLabel(resource);
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

  private void applyFilter(){
    String query = searchField.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    String selectedType = (String) typeFilter.getSelectedItem();
    List<Resource> filtered = new ArrayList<>();
    for (Resource resource : allResources){
      if (resource == null){
        continue;
      }
      if (selectedType != null && !ALL_TYPES.equals(selectedType)){
        if (!typeLabel(resource).equals(selectedType)){
          continue;
        }
      }
      if (!normalized.isBlank() && !matches(resource, normalized)){
        continue;
      }
      filtered.add(resource);
    }
    model.setRows(filtered);
    updateEditPriceButtonState();
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

  private String keyOf(Resource resource){
    if (resource == null){
      return "";
    }
    UUID id = resource.getId();
    if (id != null){
      return id.toString();
    }
    String name = resource.getName();
    return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  private String keyOf(ResourceRef ref){
    if (ref == null){
      return "";
    }
    UUID id = ref.getId();
    if (id != null){
      return id.toString();
    }
    String name = ref.getName();
    return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  private boolean isSelected(Resource resource){
    return selectedRefs.containsKey(keyOf(resource));
  }

  private ResourceRef toRef(Resource resource){
    if (resource == null){
      return null;
    }
    return new ResourceRef(resource.getId(), resource.getName(), iconKey(resource));
  }

  private Resource selectedResource(){
    int viewRow = table.getSelectedRow();
    if (viewRow < 0){
      return null;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    return model.getAt(modelRow);
  }

  private void openPriceDialog(){
    Resource resource = selectedResource();
    if (resource == null){
      return;
    }
    Window owner = SwingUtilities.getWindowAncestor(this);
    ResourcePriceEditorDialog dialog = new ResourcePriceEditorDialog(owner, resource);
    dialog.setVisible(true);
    if (resource.getId() != null){
      resourceIndex.put(resource.getId(), resource);
    }
    model.refreshAll();
    updateEditPriceButtonState();
  }

  private void updateEditPriceButtonState(){
    boolean hasSelection = table.getSelectedRow() >= 0;
    boolean canEdit = !readOnly && AccessControl.canEditResources() && ServiceLocator.resources().isAvailable();
    editPriceButton.setEnabled(hasSelection && canEdit);
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

  private class ResourceTableModel extends AbstractTableModel {
    private final List<Resource> rows = new ArrayList<>();
    private final String[] columns = {"", "Icône", "Nom", "Type", "PU HT"};

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return columns.length; }
    @Override public String getColumnName(int column){ return columns[column]; }

    @Override public Class<?> getColumnClass(int columnIndex){
      return switch (columnIndex){
        case 0 -> Boolean.class;
        default -> Object.class;
      };
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex){
      return !readOnly && columnIndex == 0;
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      Resource resource = rows.get(rowIndex);
      return switch (columnIndex){
        case 0 -> isSelected(resource);
        case 1 -> iconKey(resource);
        case 2 -> resource != null ? resource.getName() : "";
        case 3 -> typeLabel(resource);
        case 4 -> resource != null ? resource.getUnitPriceHt() : null;
        default -> "";
      };
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex){
      if (columnIndex != 0){
        return;
      }
      Resource resource = rows.get(rowIndex);
      boolean selected = Boolean.TRUE.equals(aValue);
      String key = keyOf(resource);
      if (selected){
        ResourceRef ref = toRef(resource);
        if (ref != null){
          selectedRefs.put(key, ref);
        }
      } else {
        selectedRefs.remove(key);
      }
      fireTableRowsUpdated(rowIndex, rowIndex);
      notifySelectionChanged();
    }

    void setRows(List<Resource> list){
      rows.clear();
      if (list != null){
        rows.addAll(list);
      }
      fireTableDataChanged();
    }

    List<Resource> rows(){
      return List.copyOf(rows);
    }

    Resource getAt(int index){
      if (index < 0 || index >= rows.size()){
        return null;
      }
      return rows.get(index);
    }

    void refreshAll(){
      if (!rows.isEmpty()){
        fireTableRowsUpdated(0, rows.size() - 1);
      }
    }
  }

  private static class IconRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setIcon(null);
      label.setText("");
      if (value != null){
        String key = value.toString();
        Icon icon = IconRegistry.small(key);
        if (icon != null){
          label.setIcon(icon);
        } else if (!key.isBlank()){
          label.setText(key);
        }
      }
      return label;
    }
  }

  private class PriceRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.RIGHT);
      if (value instanceof Number number){
        label.setText(priceFormat.format(number));
      } else {
        label.setText("");
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
