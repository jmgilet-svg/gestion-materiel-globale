package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ResourcesPanel extends JPanel {
  private final PlanningService service = ServiceFactory.planning();
  private final ResourceModel model = new ResourceModel();
  private final JTable table = new JTable(model);
  private final TableRowSorter<ResourceModel> sorter = new TableRowSorter<>(model);
  private final JComboBox<Object> typeFilter = new JComboBox<>();
  private final ResourceEditorPanel editor = new ResourceEditorPanel(service);
  private final JButton addButton = new JButton("+ Ressource");
  private final JButton deleteButton = new JButton("Supprimer");
  private final JButton typesButton = new JButton("Types…");

  public ResourcesPanel(){
    super(new BorderLayout(8,8));
    add(buildToolbar(), BorderLayout.NORTH);

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setFillsViewportHeight(true);
    table.setRowSorter(sorter);
    configureSorters();
    table.setRowHeight(32);
    if (table.getColumnModel().getColumnCount() > 0){
      table.getColumnModel().getColumn(0).setMinWidth(42);
      table.getColumnModel().getColumn(0).setMaxWidth(48);
      table.getColumnModel().getColumn(0).setPreferredWidth(44);
      table.getColumnModel().getColumn(0).setCellRenderer(new TypeIconRenderer());
    }

    JScrollPane tableScroll = new JScrollPane(table);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, editor);
    split.setResizeWeight(0.55);
    split.setBorder(null);
    add(split, BorderLayout.CENTER);

    editor.setOnSaved(this::onResourceSaved);

    table.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
    addButton.addActionListener(e -> onCreate());
    deleteButton.addActionListener(e -> onDelete());
    typesButton.addActionListener(e -> manageTypes());
    typeFilter.addActionListener(e -> applyFilter());

    deleteButton.setEnabled(false);

    loadTypeFilter();
    editor.reloadTypes();
    reload(null);
  }

  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    bar.add(addButton);
    bar.add(deleteButton);
    bar.add(typesButton);
    bar.add(Box.createHorizontalStrut(12));
    bar.add(new JLabel("Filtrer par type:"));
    typeFilter.setPrototypeDisplayValue("Ressource générique (long)");
    bar.add(typeFilter);
    return bar;
  }

  private void configureSorters(){
    Comparator<String> stringComparator = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    sorter.setComparator(1, stringComparator);
    sorter.setComparator(2, stringComparator);
  }

  private void loadTypeFilter(){
    Object previous = typeFilter.getSelectedItem();
    DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
    model.addElement("(Tous)");
    if (service != null){
      try {
        for (ResourceType type : service.listResourceTypes()){
          if (type != null){
            model.addElement(type);
          }
        }
      } catch(Exception ignore){}
    }
    typeFilter.setModel(model);
    if (previous != null){
      for (int i = 0; i < model.getSize(); i++){
        Object value = model.getElementAt(i);
        if (Objects.equals(value, previous)){
          typeFilter.setSelectedItem(value);
          break;
        }
      }
    }
  }

  private void applyFilter(){
    Object selected = typeFilter.getSelectedItem();
    if (selected instanceof ResourceType type){
      sorter.setRowFilter(new RowFilter<>() {
        @Override
        public boolean include(Entry<? extends ResourceModel, ? extends Integer> entry) {
          Resource resource = model.getAt(entry.getIdentifier());
          ResourceType rt = resource.getType();
          return rt != null && Objects.equals(rt.getCode(), type.getCode());
        }
      });
    } else {
      sorter.setRowFilter(null);
    }
    if (table.getSelectedRow() < 0){
      editor.clear();
      deleteButton.setEnabled(false);
    }
  }

  private void onSelectionChanged(ListSelectionEvent e){
    if (e.getValueIsAdjusting()){
      return;
    }
    Resource resource = selectedResource();
    if (resource != null){
      editor.edit(loadResourceDetails(resource));
      deleteButton.setEnabled(true);
    } else {
      editor.clear();
      deleteButton.setEnabled(false);
    }
  }

  private void onCreate(){
    table.clearSelection();
    Resource resource = new Resource();
    Object filter = typeFilter.getSelectedItem();
    if (filter instanceof ResourceType type){
      resource.setType(type);
    }
    editor.edit(resource);
    deleteButton.setEnabled(false);
  }

  private void onDelete(){
    Resource resource = selectedResource();
    if (resource == null || service == null){
      return;
    }
    int res = JOptionPane.showConfirmDialog(this,
        "Supprimer " + resource.getName() + " ?",
        "Confirmation",
        JOptionPane.OK_CANCEL_OPTION);
    if (res == JOptionPane.OK_OPTION){
      service.deleteResource(resource.getId());
      editor.clear();
      reload(null);
    }
  }

  private void manageTypes(){
    if (service == null){
      return;
    }
    Window owner = SwingUtilities.getWindowAncestor(this);
    new ResourceTypeListDialog(owner, service).setVisible(true);
    loadTypeFilter();
    editor.reloadTypes();
    reload(getSelectedResourceId());
  }

  private void onResourceSaved(Resource resource){
    reload(resource != null ? resource.getId() : null);
  }

  private void reload(UUID preferredSelection){
    UUID target = preferredSelection != null ? preferredSelection : getSelectedResourceId();
    List<Resource> items = new ArrayList<>();
    if (service != null){
      try {
        items.addAll(service.listResources());
      } catch(Exception ignore){}
    }
    model.setItems(items);
    applyFilter();
    if (target != null){
      selectResource(target);
    } else if (table.getSelectedRow() < 0){
      editor.clear();
      deleteButton.setEnabled(false);
    }
  }

  private void selectResource(UUID id){
    if (id == null){
      return;
    }
    for (int i = 0; i < model.getRowCount(); i++){
      Resource resource = model.getAt(i);
      if (resource.getId() != null && resource.getId().equals(id)){
        int viewIndex = table.convertRowIndexToView(i);
        if (viewIndex >= 0){
          table.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
        }
        return;
      }
    }
  }

  private Resource selectedResource(){
    int viewIndex = table.getSelectedRow();
    if (viewIndex < 0){
      return null;
    }
    int modelIndex = table.convertRowIndexToModel(viewIndex);
    return model.getAt(modelIndex);
  }

  private UUID getSelectedResourceId(){
    Resource resource = selectedResource();
    return resource != null ? resource.getId() : null;
  }

  private Resource loadResourceDetails(Resource base){
    Resource copy = cloneResource(base);
    if (service != null && copy.getId() != null){
      try {
        List<Unavailability> list = service.listResourceUnavailabilities(copy.getId());
        copy.setUnavailabilities(list);
      } catch(Exception ignore){}
    }
    return copy;
  }

  private Resource cloneResource(Resource source){
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

  private static String typeLabel(Resource r){
    ResourceType t = r.getType();
    if (t==null) return "";
    String label = t.getLabel();
    if (label!=null && !label.isBlank()) return label;
    String code = t.getCode();
    return code!=null? code : "";
  }
  private static String typeIcon(Resource r){
    ResourceType t = r.getType();
    String icon = t!=null? t.getIcon() : null;
    return icon!=null? icon : "";
  }

  private static class TypeIconRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setIcon(null);
      label.setText("");
      String iconKey = value != null ? value.toString() : null;
      Icon icon = IconRegistry.medium(iconKey);
      if (icon != null){
        label.setIcon(icon);
      } else if (iconKey != null && !iconKey.isBlank()){
        label.setText(iconKey);
      }
      return label;
    }
  }

  private static class ResourceModel extends AbstractTableModel {
    private final List<Resource> items = new ArrayList<>();
    private final String[] cols = {"Icône", "Nom", "Type", "Couleur", "Notes"
        // === CRM-INJECT BEGIN: resource-table-advanced-cols ===
        , "Capacité", "Tags", "Indispos hebdo"
        // === CRM-INJECT END ===
    };
    @Override public int getRowCount(){ return items.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }
    @Override public Object getValueAt(int r, int c){
      Resource x = items.get(r);
      return switch(c){
        case 0 -> typeIcon(x);
        case 1 -> x.getName();
        case 2 -> typeLabel(x);
        case 3 -> x.getColor();
        case 4 -> x.getNotes();
        // === CRM-INJECT BEGIN: resource-table-advanced-values ===
        case 5 -> x.getCapacity();
        case 6 -> x.getTags();
        case 7 -> x.getWeeklyUnavailability();
        // === CRM-INJECT END ===
        default -> "";
      };
    }

    void setItems(List<Resource> resources){
      items.clear();
      if (resources != null){
        items.addAll(resources);
      }
      fireTableDataChanged();
    }

    Resource getAt(int index){
      return items.get(index);
    }
  }
}

