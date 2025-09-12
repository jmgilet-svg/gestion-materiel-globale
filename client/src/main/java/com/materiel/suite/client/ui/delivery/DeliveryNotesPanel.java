package com.materiel.suite.client.ui.delivery;

import com.materiel.suite.client.model.DeliveryNote;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.StatusBadgeRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DeliveryNotesPanel extends JPanel {
  private final JTable table;
  private final DefaultTableModel model;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  public DeliveryNotesPanel(){
    super(new BorderLayout());
    setBorder(new EmptyBorder(8,8,8,8));
    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton bNew = new JButton("Nouveau");
    JButton bEdit = new JButton("Modifier");
    JButton bDel = new JButton("Supprimer");
    JButton bToInvoice = new JButton("Créer facture…");
    toolbar.add(bNew); toolbar.add(bEdit); toolbar.add(bDel); toolbar.add(Box.createHorizontalStrut(12)); toolbar.add(bToInvoice);
    add(toolbar, BorderLayout.NORTH);

    model = new DefaultTableModel(new Object[]{"Numéro","Date","Client","Statut","HT","TVA","TTC","ID"}, 0){
      @Override public boolean isCellEditable(int r,int c){ return false; }
    };
    table = new JTable(model);
    table.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
    table.getColumnModel().getColumn(7).setMinWidth(0);
    table.getColumnModel().getColumn(7).setMaxWidth(0);
    add(new JScrollPane(table), BorderLayout.CENTER);

    bNew.addActionListener(e -> edit(null));
    bEdit.addActionListener(e -> { UUID id = selectedId(); if (id!=null) edit(id); });
    bDel.addActionListener(e -> { UUID id = selectedId(); if (id!=null){ ServiceFactory.deliveryNotes().delete(id); reload(); }});
    bToInvoice.addActionListener(e -> {
      UUID id = selectedId();
      if (id!=null){
        var inv = ServiceFactory.invoices().createFromDeliveryNotes(java.util.List.of(id));
        if (inv!=null) JOptionPane.showMessageDialog(this, "Facture créée : "+inv.getNumber());
      }
    });
    reload();
  }
  private UUID selectedId(){
    int r = table.getSelectedRow(); if (r<0) return null;
    return java.util.UUID.fromString(model.getValueAt(r,7).toString());
  }
  public void reload(){
    model.setRowCount(0);
    for (DeliveryNote d : ServiceFactory.deliveryNotes().list()){
      model.addRow(new Object[]{
          d.getNumber(),
          d.getDate()==null? "" : DF.format(d.getDate()),
          d.getCustomerName(),
          d.getStatus(),
          d.getTotals().getTotalHT(),
          d.getTotals().getTotalTVA(),
          d.getTotals().getTotalTTC(),
          d.getId().toString()
      });
    }
  }
  private void edit(UUID id){
    DeliveryNoteEditor dlg = new DeliveryNoteEditor(SwingUtilities.getWindowAncestor(this), id);
    dlg.setVisible(true); reload();
  }
}
