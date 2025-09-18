package com.materiel.suite.client.ui.invoices;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.StatusBadgeRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class InvoicesPanel extends JPanel {
  private final JTable table;
  private final DefaultTableModel model;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  public InvoicesPanel(){
    super(new BorderLayout());
    setBorder(new EmptyBorder(8,8,8,8));
    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton bNew = new JButton("Nouveau");
    JButton bEdit = new JButton("Modifier");
    JButton bDel = new JButton("Supprimer");
    JButton bFromQuote = new JButton("Depuis devis…");
    toolbar.add(bNew); toolbar.add(bEdit); toolbar.add(bDel); toolbar.add(Box.createHorizontalStrut(12)); toolbar.add(bFromQuote);
    boolean canView = AccessControl.canViewSales();
    boolean canEdit = AccessControl.canEditSales();
    bEdit.setEnabled(canView);
    bNew.setEnabled(canEdit);
    bDel.setEnabled(canEdit);
    bFromQuote.setEnabled(canEdit);
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
    bDel.addActionListener(e -> { UUID id = selectedId(); if (id!=null){ ServiceFactory.invoices().delete(id); reload(); }});
    bFromQuote.addActionListener(e -> {
      String input = JOptionPane.showInputDialog(this, "UUID du devis :");
      if (input!=null && !input.isBlank()){
        try {
          var inv = ServiceFactory.invoices().createFromQuote(java.util.UUID.fromString(input.trim()));
          if (inv!=null) JOptionPane.showMessageDialog(this, "Facture créée : "+inv.getNumber());
        } catch(Exception ex){ JOptionPane.showMessageDialog(this, "UUID invalide"); }
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
    for (Invoice i : ServiceFactory.invoices().list()){
      model.addRow(new Object[]{
          i.getNumber(),
          i.getDate()==null? "" : DF.format(i.getDate()),
          i.getCustomerName(),
          i.getStatus(),
          i.getTotals().getTotalHT(),
          i.getTotals().getTotalTVA(),
          i.getTotals().getTotalTTC(),
          i.getId().toString()
      });
    }
  }
  private void edit(UUID id){
    InvoiceEditor dlg = new InvoiceEditor(SwingUtilities.getWindowAncestor(this), id);
    dlg.setVisible(true); reload();
  }
}
