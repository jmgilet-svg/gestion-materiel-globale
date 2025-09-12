package com.materiel.suite.client.ui.quotes;

import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.StatusBadgeRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class QuotesPanel extends JPanel {
  private final JTable table;
  private final DefaultTableModel model;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public QuotesPanel(){
    super(new BorderLayout());
    setBorder(new EmptyBorder(8,8,8,8));
    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton bNew = new JButton("Nouveau");
    JButton bEdit = new JButton("Modifier");
    JButton bDel = new JButton("Supprimer");
    JButton bToOrder = new JButton("Créer BC…");
    toolbar.add(bNew); toolbar.add(bEdit); toolbar.add(bDel); toolbar.add(Box.createHorizontalStrut(12)); toolbar.add(bToOrder);
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
    bEdit.addActionListener(e -> {
      UUID id = selectedId();
      if (id!=null) edit(id);
    });
    bDel.addActionListener(e -> {
      UUID id = selectedId();
      if (id!=null){
        int ok = JOptionPane.showConfirmDialog(this,"Supprimer le devis ?","Confirmation",JOptionPane.OK_CANCEL_OPTION);
        if (ok==JOptionPane.OK_OPTION){ ServiceFactory.quotes().delete(id); reload(); }
      }
    });
    bToOrder.addActionListener(e -> {
      UUID id = selectedId();
      if (id!=null){
        var order = ServiceFactory.orders().createFromQuote(id);
        if (order!=null) JOptionPane.showMessageDialog(this, "BC créé : "+order.getNumber());
      }
    });
    reload();
  }

  private UUID selectedId(){
    int r = table.getSelectedRow();
    if (r<0) return null;
    return UUID.fromString(model.getValueAt(r,7).toString());
  }

  public void reload(){
    model.setRowCount(0);
    for (Quote q : ServiceFactory.quotes().list()){
      model.addRow(new Object[]{
          q.getNumber(),
          q.getDate()==null? "" : DF.format(q.getDate()),
          q.getCustomerName(),
          q.getStatus(),
          q.getTotals().getTotalHT(),
          q.getTotals().getTotalTVA(),
          q.getTotals().getTotalTTC(),
          q.getId().toString()
      });
    }
  }

  private void edit(UUID id){
    QuoteEditor dlg = new QuoteEditor(SwingUtilities.getWindowAncestor(this), id);
    dlg.setVisible(true);
    reload();
  }
}
