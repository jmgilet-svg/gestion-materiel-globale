package com.materiel.suite.client.ui.orders;

import com.materiel.suite.client.model.Order;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.StatusBadgeRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OrdersPanel extends JPanel {
  private final JTable table;
  private final DefaultTableModel model;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  public OrdersPanel(){
    super(new BorderLayout());
    setBorder(new EmptyBorder(8,8,8,8));
    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton bNew = new JButton("Nouveau");
    JButton bEdit = new JButton("Modifier");
    JButton bDel = new JButton("Supprimer");
    JButton bToDN = new JButton("Générer BL…");
    JButton bConfirm = new JButton("Confirmer");
    JButton bLock = new JButton("Verrouiller");
    JButton bCancel = new JButton("Annuler");
    toolbar.add(bNew); toolbar.add(bEdit); toolbar.add(bDel); toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(bToDN);
    toolbar.add(Box.createHorizontalStrut(12));
    toolbar.add(bConfirm); toolbar.add(bLock); toolbar.add(bCancel);
    add(toolbar, BorderLayout.NORTH);

    model = new DefaultTableModel(new Object[]{"Numéro","Date","Client","Statut","HT","TVA","TTC","ID","Version"}, 0){
      @Override public boolean isCellEditable(int r,int c){ return false; }
    };
    table = new JTable(model);
    table.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
    table.getColumnModel().getColumn(7).setMinWidth(0);
    table.getColumnModel().getColumn(7).setMaxWidth(0);
    table.getColumnModel().getColumn(8).setMinWidth(0);
    table.getColumnModel().getColumn(8).setMaxWidth(0);
    add(new JScrollPane(table), BorderLayout.CENTER);

    bNew.addActionListener(e -> edit(null));
    bEdit.addActionListener(e -> { UUID id = selectedId(); if (id!=null) edit(id); });
    bDel.addActionListener(e -> { UUID id = selectedId(); if (id!=null){ ServiceFactory.orders().delete(id); reload(); }});
    bToDN.addActionListener(e -> {
      UUID id = selectedId();
      if (id!=null){
        var dn = ServiceFactory.deliveryNotes().createFromOrder(id);
        if (dn!=null) JOptionPane.showMessageDialog(this, "BL créé : "+dn.getNumber());
      }
    });
    bConfirm.addActionListener(e -> {
      UUID id = selectedId(); if (id!=null) call(() -> ServiceFactory.workflow().orderConfirm(id, selectedVersion()));
    });
    bLock.addActionListener(e -> {
      UUID id = selectedId(); if (id!=null) call(() -> ServiceFactory.workflow().orderLock(id, selectedVersion()));
    });
    bCancel.addActionListener(e -> {
      UUID id = selectedId(); if (id!=null) call(() -> ServiceFactory.workflow().orderCancel(id, selectedVersion()));
    });
    reload();
  }
  private UUID selectedId(){
    int r = table.getSelectedRow(); if (r<0) return null;
    return java.util.UUID.fromString(model.getValueAt(r,7).toString());
  }
  private long selectedVersion(){
    int r = table.getSelectedRow(); if (r<0) return 0L;
    return Long.parseLong(model.getValueAt(r,8).toString());
  }
  public void reload(){
    model.setRowCount(0);
    for (Order o : ServiceFactory.orders().list()){
      model.addRow(new Object[]{
          o.getNumber(),
          o.getDate()==null? "" : DF.format(o.getDate()),
          o.getCustomerName(),
          o.getStatus(),
          o.getTotals().getTotalHT(),
          o.getTotals().getTotalTVA(),
          o.getTotals().getTotalTTC(),
          o.getId().toString(),
          o.getVersion()
      });
    }
  }
  private void edit(UUID id){
    OrderEditor dlg = new OrderEditor(SwingUtilities.getWindowAncestor(this), id);
    dlg.setVisible(true); reload();
  }
  private void call(Throwing r){
    try { r.run(); reload(); }
    catch(Exception ex){ JOptionPane.showMessageDialog(this, "Erreur: "+ex.getMessage()); }
  }
  private interface Throwing { void run() throws Exception; }
}

