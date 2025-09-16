package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    JTextField name = new JTextField(r!=null? r.getName():"", 20);
    JTextField type = new JTextField(r!=null? r.getType():"", 12);
    JTextField color= new JTextField(r!=null? r.getColor():"", 8);
    JTextArea notes = new JTextArea(r!=null? r.getNotes():"", 5, 30);
    notes.setLineWrap(true); notes.setWrapStyleWord(true);
    // === CRM-INJECT BEGIN: resource-editor-advanced-fields ===
    int baseCapacity = (r!=null && r.getCapacity()!=null)? r.getCapacity():1;
    if (baseCapacity<1) baseCapacity = 1;
    JSpinner capacity = new JSpinner(new SpinnerNumberModel(baseCapacity, 1, 999, 1));
    JTextField tags = new JTextField(r!=null && r.getTags()!=null? r.getTags():"", 20);
    JTextArea weekly = new JTextArea(r!=null && r.getWeeklyUnavailability()!=null? r.getWeeklyUnavailability():"", 4, 30);
    weekly.setLineWrap(true); weekly.setWrapStyleWord(true);
    // === CRM-INJECT END ===
    Object[] msg = {"Nom:", name, "Type:", type, "Couleur (hex):", color, "Notes:", new JScrollPane(notes)
        // === CRM-INJECT BEGIN: resource-editor-advanced-layout ===
        , "Capacité:", capacity, "Tags:", tags, "Indisponibilités récurrentes:", new JScrollPane(weekly)
        // === CRM-INJECT END ===
    };
    int ok = JOptionPane.showConfirmDialog(this, msg, (r==null? "Nouvelle ressource":"Modifier ressource"), JOptionPane.OK_CANCEL_OPTION);
    if (ok==JOptionPane.OK_OPTION){
      Resource x = (r==null? new Resource() : r);
      if (x.getId()==null) x.setId(UUID.randomUUID());
      x.setName(name.getText().trim());
      x.setType(type.getText().trim());
      x.setColor(color.getText().trim());
      x.setNotes(notes.getText());
      // === CRM-INJECT BEGIN: resource-editor-advanced-save ===
      Object capVal = capacity.getValue();
      int cap = 1;
      if (capVal instanceof Number n) cap = Math.max(1, n.intValue());
      else {
        try { cap = Math.max(1, Integer.parseInt(String.valueOf(capVal))); } catch(Exception ignore){}
      }
      x.setCapacity(cap);
      x.setTags(tags.getText().trim());
      x.setWeeklyUnavailability(weekly.getText());
      // === CRM-INJECT END ===
      ServiceFactory.planning().saveResource(x);
      reload();
    }
  }
  private void reload(){
    model.items = new ArrayList<>(ServiceFactory.planning().listResources());
    model.fireTableDataChanged();
  }
  private static class ResourceModel extends AbstractTableModel {
    List<Resource> items = new ArrayList<>();
    String[] cols = {"Nom", "Type", "Couleur", "Notes"
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
        case 1 -> x.getType();
        case 2 -> x.getColor();
        case 3 -> x.getNotes();
        // === CRM-INJECT BEGIN: resource-table-advanced-values ===
        case 4 -> x.getCapacity();
        case 5 -> x.getTags();
        case 6 -> x.getWeeklyUnavailability();
        // === CRM-INJECT END ===
        default -> "";
      };
    }
  }
}

