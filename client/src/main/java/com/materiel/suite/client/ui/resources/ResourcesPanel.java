package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Gestion des ressources avec édition inline du PU HT. */
public class ResourcesPanel extends JPanel {
  private final ResourcesTableModel model = new ResourcesTableModel();
  private final JTable table = new JTable(model);
  private final JTextField search = new JTextField();
  private final JComboBox<String> typeFilter = new JComboBox<>();
  private final JButton refreshBtn = new JButton("Recharger", IconRegistry.small("refresh"));
  private final JButton editPriceBtn = new JButton("Tarif…", IconRegistry.small("edit"));

  public ResourcesPanel(){
    super(new BorderLayout(8,8));

    // Toolbar
    JToolBar tb = new JToolBar(); tb.setFloatable(false);
    tb.add(new JLabel("Filtrer")); tb.add(Box.createHorizontalStrut(6));
    tb.add(typeFilter); tb.add(Box.createHorizontalStrut(6));
    tb.add(new JLabel(IconRegistry.small("search"))); tb.add(search);
    tb.add(Box.createHorizontalGlue());
    tb.add(editPriceBtn); tb.add(Box.createHorizontalStrut(6)); tb.add(refreshBtn);
    add(tb, BorderLayout.NORTH);

    // Table
    table.setRowHeight(24);
    table.setAutoCreateRowSorter(true);
    // Rendu type (icône + nom)
    table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
      @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
        JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", s, f, r, c);
        if(v instanceof String name){
          l.setText(name);
          String icon = model.iconAt(table.convertRowIndexToModel(r));
          l.setIcon(IconRegistry.small(icon));
          l.setIconTextGap(6);
        }
        return l;
      }
    });
    // Rendu PU HT (aligné à droite, 2 décimales)
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMinimumFractionDigits(0); nf.setMaximumFractionDigits(2);
    DefaultTableCellRenderer priceRenderer = new DefaultTableCellRenderer(){
      @Override protected void setValue(Object value){
        if(value instanceof BigDecimal bd) setText(nf.format(bd));
        else if(value instanceof Number n) setText(nf.format(n.doubleValue()));
        else setText("");
        setHorizontalAlignment(RIGHT);
      }
    };
    table.getColumnModel().getColumn(3).setCellRenderer(priceRenderer);
    // Éditeur PU HT (JFormattedTextField)
    JFormattedTextField priceField = new JFormattedTextField(
        new DefaultFormatterFactory(new NumberFormatter(nf)));
    priceField.setHorizontalAlignment(SwingConstants.RIGHT);
    table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(priceField){
      @Override public boolean stopCellEditing(){
        boolean ok = super.stopCellEditing();
        // Le setValueAt() du model déclenche le save() → rien d’autre à faire
        return ok;
      }
    });

    add(new JScrollPane(table), BorderLayout.CENTER);

    // Actions
    refreshBtn.addActionListener(e -> reload());
    editPriceBtn.addActionListener(e -> openPriceDialogSelected());
    search.getDocument().addDocumentListener((SimpleDoc) e -> applyFilter());
    typeFilter.addActionListener(e -> applyFilter());

    // Initial load
    reload();
  }

  private void reload(){
    var resources = ServiceLocator.resources().listAll();
    model.setData(resources);
    // Types pour filtre
    typeFilter.removeAllItems();
    typeFilter.addItem("Tous les types");
    model.distinctTypes().forEach(typeFilter::addItem);
    applyFilter();
    Toasts.info(this, resources.size()+" ressource(s) chargée(s)");
  }

  private void applyFilter(){
    String q = search.getText()==null? "" : search.getText().toLowerCase();
    String type = Objects.toString(typeFilter.getSelectedItem(), "Tous les types");
    model.applyFilter(q, "Tous les types".equals(type)? null : type);
  }

  private void openPriceDialogSelected(){
    int row = table.getSelectedRow();
    if(row<0) return;
    int modelRow = table.convertRowIndexToModel(row);
    Resource r = model.resourceAt(modelRow);
    if(r==null) return;
    // commit l’édition en cours (si curseur dans la cellule)
    if(table.isEditing()) table.getCellEditor().stopCellEditing();
    new ResourcePriceEditorDialog(SwingUtilities.getWindowAncestor(this), r).setVisible(true);
    // Après fermeture, r a été sauvegardée (via ServiceLocator.resources().save(r))
    model.refreshRow(modelRow, r);
  }

  /* ----------- Table model ----------- */
  private static class ResourcesTableModel extends AbstractTableModel {
    private final String[] cols = {"Nom", "Type", "État", "PU HT"};
    private List<Resource> all = new ArrayList<>();
    private List<Integer> idx = new ArrayList<>(); // index filtré -> all

    public void setData(List<Resource> data){
      all = data!=null? data : new ArrayList<>();
      idx = new ArrayList<>();
      for(int i=0;i<all.size();i++) idx.add(i);
      fireTableDataChanged();
    }
    public void applyFilter(String q, String type){
      idx.clear();
      for(int i=0;i<all.size();i++){
        Resource r = all.get(i);
        boolean tOk = (type==null) || (r.getType()!=null && type.equals(r.getType().getName()));
        boolean qOk = (q==null || q.isBlank())
            || (r.getName()!=null && r.getName().toLowerCase().contains(q))
            || (r.getState()!=null && r.getState().toLowerCase().contains(q))
            || (r.getType()!=null && r.getType().getName()!=null && r.getType().getName().toLowerCase().contains(q));
        if(tOk && qOk) idx.add(i);
      }
      fireTableDataChanged();
    }
    public List<String> distinctTypes(){
      java.util.Set<String> s = new java.util.LinkedHashSet<>();
      for(Resource r : all){ if(r.getType()!=null && r.getType().getName()!=null) s.add(r.getType().getName()); }
      return new ArrayList<>(s);
    }

    public Resource resourceAt(int viewRow){
      if(viewRow<0 || viewRow>=idx.size()) return null;
      return all.get(idx.get(viewRow));
    }
    public void refreshRow(int viewRow, Resource updated){
      int i = idx.get(viewRow);
      all.set(i, updated);
      fireTableRowsUpdated(viewRow, viewRow);
    }
    public String iconAt(int viewRow){
      Resource r = resourceAt(viewRow);
      return r!=null && r.getType()!=null? r.getType().getIconKey() : null;
    }

    @Override public int getRowCount(){ return idx.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }
    @Override public Class<?> getColumnClass(int c){
      return switch (c){
        case 3 -> BigDecimal.class;
        default -> String.class;
      };
    }
    @Override public boolean isCellEditable(int r, int c){ return c==3; } // PU HT éditable

    @Override public Object getValueAt(int row, int col){
      Resource r = resourceAt(row);
      if(r==null) return null;
      return switch (col){
        case 0 -> r.getName();
        case 1 -> r.getType()!=null? r.getType().getName() : "";
        case 2 -> r.getState();
        case 3 -> r.getUnitPriceHt();
        default -> null;
      };
    }

    @Override public void setValueAt(Object aValue, int row, int col){
      if(col!=3) return;
      Resource r = resourceAt(row);
      if(r==null) return;
      BigDecimal newVal = toBD(aValue);
      // No-op si identique
      if(eq(r.getUnitPriceHt(), newVal)) return;
      r.setUnitPriceHt(newVal);
      // Persiste tout de suite
      try{
        Resource saved = ServiceLocator.resources().save(r);
        all.set(idx.get(row), saved);
        fireTableRowsUpdated(row, row);
        // Toast côté EDT safe
        SwingUtilities.invokeLater(() -> Toasts.success(null, "PU mis à jour"));
      }catch(Exception ex){
        SwingUtilities.invokeLater(() -> Toasts.error(null, "Échec de sauvegarde: "+ ex.getMessage()));
      }
    }
    private BigDecimal toBD(Object v){
      if(v==null) return BigDecimal.ZERO;
      if(v instanceof BigDecimal bd) return bd;
      if(v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
      try { return new BigDecimal(String.valueOf(v).replace(',','.')); } catch(Exception e){ return BigDecimal.ZERO; }
    }
    private boolean eq(BigDecimal a, BigDecimal b){
      if(a==null && b==null) return true;
      if(a==null || b==null) return false;
      return a.compareTo(b)==0;
    }
  }

  /** petit helper doc listener sans boilerplate */
  private interface SimpleDoc extends javax.swing.event.DocumentListener{
    void update(javax.swing.event.DocumentEvent e);
    @Override default void insertUpdate(javax.swing.event.DocumentEvent e){ update(e); }
    @Override default void removeUpdate(javax.swing.event.DocumentEvent e){ update(e); }
    @Override default void changedUpdate(javax.swing.event.DocumentEvent e){ update(e); }
  }
}
