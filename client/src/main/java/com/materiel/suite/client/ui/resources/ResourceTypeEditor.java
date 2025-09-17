package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.ResourceTypeService;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconPickerDialog;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Éditeur simple pour gérer les types de ressources (nom, icône, prix unitaire). */
public class ResourceTypeEditor extends JPanel {
  private final ResourceTypeService service = ServiceFactory.resourceTypes();
  private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Icône", "Nom", "PU HT"}, 0){
    @Override public boolean isCellEditable(int row, int column){ return false; }
  };
  private final JTable table = new JTable(model);
  private final JButton addButton = new JButton("Ajouter", IconRegistry.small("plus"));
  private final JButton editButton = new JButton("Modifier", IconRegistry.small("edit"));
  private final JButton deleteButton = new JButton("Supprimer", IconRegistry.small("trash"));
  private final JButton refreshButton = new JButton("Recharger", IconRegistry.small("refresh"));
  private List<ResourceType> current = new ArrayList<>();

  public ResourceTypeEditor(){
    super(new BorderLayout(6, 6));
    buildUI();
    reload();
  }

  private void buildUI(){
    table.setRowHeight(26);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getTableHeader().setReorderingAllowed(false);
    if (table.getColumnModel().getColumnCount() > 0){
      table.getColumnModel().getColumn(0).setMinWidth(48);
      table.getColumnModel().getColumn(0).setMaxWidth(64);
      table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer());
    }

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(addButton);
    toolbar.add(editButton);
    toolbar.add(deleteButton);
    toolbar.addSeparator();
    toolbar.add(refreshButton);

    add(toolbar, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);

    addButton.addActionListener(e -> openDialog(null));
    editButton.addActionListener(e -> {
      ResourceType type = currentSelection();
      if (type != null){
        openDialog(type);
      }
    });
    deleteButton.addActionListener(e -> onDelete());
    refreshButton.addActionListener(e -> reload());

    boolean available = service != null;
    addButton.setEnabled(available);
    editButton.setEnabled(available);
    deleteButton.setEnabled(available);
    refreshButton.setEnabled(available);
    table.setEnabled(available);
  }

  private void reload(){
    model.setRowCount(0);
    current = new ArrayList<>();
    if (service == null){
      return;
    }
    try {
      List<ResourceType> list = new ArrayList<>(service.listAll());
      list.sort(Comparator.comparing(rt -> normalize(rt.getName()), String.CASE_INSENSITIVE_ORDER));
      current.addAll(list);
      for (ResourceType type : list){
        BigDecimal price = type.getUnitPriceHt();
        model.addRow(new Object[]{ type.getIconKey(), type.getName(), price });
      }
    } catch (Exception ex){
      Toasts.error(this, "Chargement impossible : " + ex.getMessage());
    }
  }

  private String normalize(String value){
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private ResourceType currentSelection(){
    int row = table.getSelectedRow();
    if (row < 0){
      return null;
    }
    int modelRow = table.convertRowIndexToModel(row);
    if (modelRow < 0 || modelRow >= current.size()){
      return null;
    }
    return current.get(modelRow);
  }

  private void onDelete(){
    ResourceType type = currentSelection();
    if (type == null || service == null){
      return;
    }
    try {
      service.delete(type.getId());
      Toasts.success(this, "Type supprimé");
      reload();
    } catch (Exception ex){
      Toasts.error(this, "Suppression impossible : " + ex.getMessage());
    }
  }

  private void openDialog(ResourceType initial){
    if (service == null){
      return;
    }
    Window owner = SwingUtilities.getWindowAncestor(this);
    JDialog dialog = new JDialog(owner, "Type de ressource", Dialog.ModalityType.APPLICATION_MODAL);
    JTextField nameField = new JTextField(initial != null ? valueOrEmpty(initial.getName()) : "");
    JTextField iconField = new JTextField(initial != null ? valueOrEmpty(initial.getIconKey()) : "");
    iconField.setEditable(false);
    JButton pickIcon = new JButton("Choisir icône", IconRegistry.small("image"));

    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
    numberFormat.setMaximumFractionDigits(2);
    numberFormat.setMinimumFractionDigits(0);
    numberFormat.setGroupingUsed(true);
    JFormattedTextField priceField = new JFormattedTextField(numberFormat);
    priceField.setColumns(10);
    priceField.setValue(initial != null ? initial.getUnitPriceHt() : null);

    pickIcon.addActionListener(e -> {
      IconPickerDialog picker = new IconPickerDialog(owner);
      String chosen = picker.pick();
      if (chosen != null){
        iconField.setText(chosen);
      }
    });

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 6, 6, 6);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0;
    gc.gridy = 0;

    form.add(new JLabel("Nom"), gc);
    gc.gridx = 1;
    gc.weightx = 1;
    form.add(nameField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.weightx = 0;
    form.add(new JLabel("Icône"), gc);
    gc.gridx = 1;
    gc.weightx = 1;
    form.add(iconField, gc);
    gc.gridx = 2;
    gc.weightx = 0;
    form.add(pickIcon, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.weightx = 0;
    form.add(new JLabel("PU HT (€)"), gc);
    gc.gridx = 1;
    gc.weightx = 1;
    form.add(priceField, gc);

    JButton saveButton = new JButton("Enregistrer", IconRegistry.small("success"));
    JButton cancelButton = new JButton("Annuler");
    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    south.add(saveButton);
    south.add(cancelButton);

    JPanel content = new JPanel(new BorderLayout());
    content.add(form, BorderLayout.CENTER);
    content.add(south, BorderLayout.SOUTH);
    dialog.setContentPane(content);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);

    saveButton.addActionListener(e -> {
      String name = nameField.getText() != null ? nameField.getText().trim() : "";
      if (name.isEmpty()){
        Toasts.error(dialog, "Le nom est requis");
        return;
      }
      BigDecimal price = parsePrice(priceField);
      ResourceType toSave = initial != null ? copyOf(initial) : new ResourceType();
      toSave.setName(name);
      String icon = iconField.getText() != null ? iconField.getText().trim() : "";
      toSave.setIconKey(icon.isEmpty() ? "cube" : icon);
      toSave.setUnitPriceHt(price);
      try {
        ResourceType saved = service.save(toSave);
        Toasts.success(dialog, "Type enregistré");
        dialog.dispose();
        reload();
        selectById(saved != null ? saved.getId() : toSave.getId());
      } catch (Exception ex){
        Toasts.error(dialog, "Enregistrement impossible : " + ex.getMessage());
      }
    });

    cancelButton.addActionListener(e -> dialog.dispose());
    dialog.setVisible(true);
  }

  private void selectById(String id){
    if (id == null){
      return;
    }
    for (int i = 0; i < current.size(); i++){
      ResourceType type = current.get(i);
      if (id.equals(type.getId())){
        int viewRow = table.convertRowIndexToView(i);
        table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        break;
      }
    }
  }

  private ResourceType copyOf(ResourceType src){
    ResourceType copy = new ResourceType();
    copy.setId(src.getId());
    copy.setName(src.getName());
    copy.setIconKey(src.getIconKey());
    copy.setUnitPriceHt(src.getUnitPriceHt());
    return copy;
  }

  private String valueOrEmpty(String value){
    return value == null ? "" : value;
  }

  private BigDecimal parsePrice(JFormattedTextField field){
    try {
      field.commitEdit();
    } catch (ParseException ignore){}
    Object value = field.getValue();
    if (value == null){
      return BigDecimal.ZERO;
    }
    if (value instanceof BigDecimal bd){
      return bd;
    }
    if (value instanceof Number number){
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(value.toString().replace(',', '.'));
    } catch (NumberFormatException ex){
      return BigDecimal.ZERO;
    }
  }

  private static class IconCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      String key = value != null ? value.toString() : null;
      Icon icon = IconRegistry.small(key);
      label.setIcon(icon);
      label.setText(icon == null && key != null ? key : "");
      return label;
    }
  }
}
