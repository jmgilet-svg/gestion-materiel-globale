package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ResourceTypeListDialog extends JDialog {
  private final PlanningService service;
  private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Code", "Icône", "Libellé"}, 0){
    @Override public boolean isCellEditable(int row, int column){ return false; }
  };
  private final JTable table = new JTable(model);

  public ResourceTypeListDialog(Window owner, PlanningService service){
    super(owner, "Types de ressource", ModalityType.APPLICATION_MODAL);
    this.service = service;
    buildUI();
    table.setRowHeight(32);
    if (table.getColumnModel().getColumnCount() > 1){
      table.getColumnModel().getColumn(1).setMinWidth(70);
      table.getColumnModel().getColumn(1).setMaxWidth(90);
      table.getColumnModel().getColumn(1).setPreferredWidth(80);
      table.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());
    }
    reload();
    pack();
    setLocationRelativeTo(owner);
  }

  private void buildUI(){
    JButton add = new JButton("Nouveau…");
    JButton edit = new JButton("Modifier…");
    JButton del = new JButton("Supprimer");
    JButton close = new JButton("Fermer");

    add.addActionListener(e -> onCreate());
    edit.addActionListener(e -> onEdit());
    del.addActionListener(e -> onDelete());
    close.addActionListener(e -> dispose());

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    top.add(add); top.add(edit); top.add(del);

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setFillsViewportHeight(true);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    south.add(close);

    setLayout(new BorderLayout(8,8));
    add(top, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);
  }

  private void reload(){
    model.setRowCount(0);
    List<ResourceType> types = service.listResourceTypes();
    for (ResourceType type : types){
      model.addRow(new Object[]{ type.getCode(), type.getIcon(), type.getLabel() });
    }
  }

  private String selectedCode(){
    int idx = table.getSelectedRow();
    if (idx<0) return null;
    Object value = model.getValueAt(idx, 0);
    return value!=null? value.toString() : null;
  }

  private void onCreate(){
    ResourceTypeEditDialog dlg = new ResourceTypeEditDialog(getOwner(), service, null);
    dlg.setVisible(true);
    if (dlg.isSaved()) reload();
  }

  private void onEdit(){
    String code = selectedCode();
    if (code==null) return;
    ResourceType current = service.listResourceTypes().stream()
        .filter(t -> code.equals(t.getCode()))
        .findFirst()
        .orElse(null);
    if (current==null) return;
    ResourceTypeEditDialog dlg = new ResourceTypeEditDialog(getOwner(), service, current);
    dlg.setVisible(true);
    if (dlg.isSaved()) reload();
  }

  private void onDelete(){
    String code = selectedCode();
    if (code==null) return;
    int res = JOptionPane.showConfirmDialog(this,
        "Supprimer le type \""+code+"\" ?",
        "Confirmation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    if (res==JOptionPane.OK_OPTION){
      try {
        service.deleteResourceType(code);
        reload();
      } catch(Exception ex){
        JOptionPane.showMessageDialog(this, "Erreur: "+ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
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
}
