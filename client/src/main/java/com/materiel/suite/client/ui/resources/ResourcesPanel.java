package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResourcesPanel extends JPanel {
  private final JTable table = new JTable();
  private final ResourceModel model = new ResourceModel();

  public ResourcesPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);
    table.setModel(model);
    add(new JScrollPane(table), BorderLayout.CENTER);
    reload();
  }
  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    JButton add = new JButton("+ Ressource");
    JButton edit = new JButton("Modifier");
    JButton del = new JButton("Supprimer");
    add.addActionListener(e -> editResource(null));
    edit.addActionListener(e -> {
      int i = table.getSelectedRow();
      if (i>=0) editResource(model.items.get(i));
    });
    del.addActionListener(e -> {
      int i = table.getSelectedRow();
      if (i>=0){
        Resource r = model.items.get(i);
        int ok = JOptionPane.showConfirmDialog(this, "Supprimer "+r.getName()+" ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
        if (ok==JOptionPane.OK_OPTION){ ServiceFactory.planning().deleteResource(r.getId()); reload(); }
      }
    });
    bar.add(add); bar.add(edit); bar.add(del);
    return bar;
  }
  private void editResource(Resource r){
    Window owner = SwingUtilities.getWindowAncestor(this);
    ResourceEditDialog dlg = new ResourceEditDialog(owner, ServiceFactory.planning(), r);
    dlg.setVisible(true);
    if (dlg.isSaved()) reload();
  }
  private void reload(){
    model.items = new ArrayList<>(ServiceFactory.planning().listResources());
    model.fireTableDataChanged();
  }
  private static String typeLabel(Resource r){
    ResourceType t = r.getType();
    if (t==null) return "";
    String label = t.getLabel();
    if (label!=null && !label.isBlank()) return label;
    String code = t.getCode();
    return code!=null? code : "";
  }
  private static class ResourceModel extends AbstractTableModel {
    List<Resource> items = new ArrayList<>();
    String[] cols = {"Nom", "Icône", "Type", "Couleur", "Notes"
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
        case 0 -> x.getName();
        case 1 -> x.getIcon();
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
  }
}

