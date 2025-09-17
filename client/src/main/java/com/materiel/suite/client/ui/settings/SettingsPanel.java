package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.icons.IconPickerDialog;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.ui.resources.ResourceTypeEditDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Panneau de paramétrage (types de ressources, bibliothèque d'icônes, etc.). */
public class SettingsPanel extends JPanel {
  private final PlanningService planningService;

  public SettingsPanel(){
    super(new BorderLayout());
    this.planningService = ServiceFactory.planning();

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Types de ressources", IconRegistry.small("wrench"), buildResourceTypePanel());
    tabs.addTab("Bibliothèque d'icônes", IconRegistry.small("settings"), buildIconLibraryPanel());
    add(tabs, BorderLayout.CENTER);
  }

  private JComponent buildResourceTypePanel(){
    if (planningService == null){
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(new JLabel("Service de planification indisponible", SwingConstants.CENTER), BorderLayout.CENTER);
      return panel;
    }
    return new ResourceTypePanel(planningService);
  }

  private JComponent buildIconLibraryPanel(){
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JTextField searchField = new JTextField();
    DefaultListModel<String> model = new DefaultListModel<>();
    JList<String> list = new JList<>(model);
    list.setVisibleRowCount(12);
    list.setCellRenderer(new IconListRenderer());
    List<String> keys = new ArrayList<>(IconRegistry.listKeys());
    keys.sort(String::compareTo);
    updateIconList(model, keys, "");

    searchField.getDocument().addDocumentListener(new DocumentAdapter(){
      @Override public void update(DocumentEvent e){
        String query = searchField.getText();
        updateIconList(model, keys, query == null ? "" : query.trim());
      }
    });

    JButton openPicker = new JButton("Ouvrir le sélecteur…");
    openPicker.addActionListener(e -> {
      Window owner = SwingUtilities.getWindowAncestor(SettingsPanel.this);
      IconPickerDialog dialog = new IconPickerDialog(owner);
      dialog.setVisible(true);
    });

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(openPicker);

    JPanel north = new JPanel(new BorderLayout(4, 4));
    north.add(new JLabel("Rechercher une icône :"), BorderLayout.WEST);
    north.add(searchField, BorderLayout.CENTER);
    panel.add(north, BorderLayout.NORTH);
    panel.add(new JScrollPane(list), BorderLayout.CENTER);
    panel.add(south, BorderLayout.SOUTH);
    return panel;
  }

  private void updateIconList(DefaultListModel<String> model, List<String> keys, String query){
    model.clear();
    String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
    for (String key : keys){
      if (normalized.isBlank() || key.toLowerCase(Locale.ROOT).contains(normalized)){
        model.addElement(key);
      }
    }
  }

  private static class IconListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      String key = value != null ? value.toString() : "";
      label.setText(key);
      label.setIcon(IconRegistry.medium(key));
      return label;
    }
  }

  private static class ResourceTypePanel extends JPanel {
    private final PlanningService service;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Code", "Icône", "Libellé"}, 0){
      @Override public boolean isCellEditable(int row, int column){ return false; }
    };
    private final JTable table = new JTable(model);

    ResourceTypePanel(PlanningService service){
      super(new BorderLayout(8, 8));
      this.service = service;
      buildUI();
      reload();
    }

    private void buildUI(){
      JButton add = new JButton("Ajouter…");
      JButton edit = new JButton("Modifier…");
      JButton delete = new JButton("Supprimer");

      add.addActionListener(e -> onCreate());
      edit.addActionListener(e -> onEdit());
      delete.addActionListener(e -> onDelete());

      JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
      toolbar.add(add);
      toolbar.add(edit);
      toolbar.add(delete);

      table.setRowHeight(28);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if (table.getColumnModel().getColumnCount() > 1){
        table.getColumnModel().getColumn(1).setMinWidth(80);
        table.getColumnModel().getColumn(1).setMaxWidth(100);
        table.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());
      }

      add(toolbar, BorderLayout.NORTH);
      add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void reload(){
      model.setRowCount(0);
      List<ResourceType> types = service.listResourceTypes();
      for (ResourceType type : types){
        model.addRow(new Object[]{ type.getCode(), type.getIcon(), type.getLabel() });
      }
    }

    private String selectedCode(){
      int row = table.getSelectedRow();
      if (row < 0){
        return null;
      }
      int modelRow = table.convertRowIndexToModel(row);
      Object value = model.getValueAt(modelRow, 0);
      return value != null ? value.toString() : null;
    }

    private void onCreate(){
      ResourceTypeEditDialog dialog = new ResourceTypeEditDialog(SwingUtilities.getWindowAncestor(this), service, null);
      dialog.setVisible(true);
      if (dialog.isSaved()){
        reload();
      }
    }

    private void onEdit(){
      String code = selectedCode();
      if (code == null){
        return;
      }
      ResourceType existing = service.listResourceTypes().stream()
          .filter(t -> code.equals(t.getCode()))
          .findFirst()
          .orElse(null);
      if (existing == null){
        return;
      }
      ResourceTypeEditDialog dialog = new ResourceTypeEditDialog(SwingUtilities.getWindowAncestor(this), service, existing);
      dialog.setVisible(true);
      if (dialog.isSaved()){
        reload();
      }
    }

    private void onDelete(){
      String code = selectedCode();
      if (code == null){
        return;
      }
      int confirm = JOptionPane.showConfirmDialog(this,
          "Supprimer le type \"" + code + "\" ?",
          "Confirmation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
      if (confirm == JOptionPane.OK_OPTION){
        try {
          service.deleteResourceType(code);
          reload();
        } catch (Exception ex){
          JOptionPane.showMessageDialog(this, "Suppression impossible : " + ex.getMessage(),
              "Erreur", JOptionPane.ERROR_MESSAGE);
        }
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
        Icon icon = IconRegistry.medium(key);
        if (icon != null){
          label.setIcon(icon);
        } else if (key != null && !key.isBlank()){
          label.setText(key);
        }
        return label;
      }
    }
  }

  private abstract static class DocumentAdapter implements DocumentListener {
    @Override public void insertUpdate(DocumentEvent e){ update(e); }
    @Override public void removeUpdate(DocumentEvent e){ update(e); }
    @Override public void changedUpdate(DocumentEvent e){ update(e); }
    public abstract void update(DocumentEvent e);
  }
}
