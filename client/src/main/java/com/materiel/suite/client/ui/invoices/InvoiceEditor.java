package com.materiel.suite.client.ui.invoices;

import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Invoice;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.doc.DocumentLineTableModel;
import com.materiel.suite.client.ui.doc.DocumentTotalsPanel;
// === CRM-INJECT BEGIN: invoice-client-binding-import ===
import com.materiel.suite.client.ui.doc.ClientContactBinding;
// === CRM-INJECT END ===

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.UUID;

public class InvoiceEditor extends JDialog {
  private final JTextField tfNumber = new JTextField(12);
  private final JTextField tfCustomer = new JTextField(24);
  private final JTextField tfDate = new JTextField(10);
  private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Brouillon","Envoyée","Partiellement payée","Payée","Annulée"});
  // === CRM-INJECT BEGIN: invoice-client-binding-fields ===
  private final JComboBox<Contact> cbContact = new JComboBox<>();
  private final ClientContactBinding clientBinding = new ClientContactBinding(tfCustomer, cbContact);
  // === CRM-INJECT END ===
  private final DocumentTotalsPanel totalsPanel = new DocumentTotalsPanel();
  private DocumentLineTableModel lineModel;
  private Invoice bean;

  public InvoiceEditor(Window owner, UUID id){
    super(owner, "Édition facture", ModalityType.APPLICATION_MODAL);
    setSize(900, 560);
    setLocationRelativeTo(owner);
    setLayout(new BorderLayout());

    if (id==null){
      bean = new Invoice();
      bean.setId(java.util.UUID.randomUUID());
      bean.setDate(LocalDate.now());
      bean.setStatus("Brouillon");
      bean.getLines().add(new DocumentLine("Nouvelle ligne",1,"",0,0,20));
      bean.recomputeTotals();
    } else {
      bean = ServiceFactory.invoices().get(id);
      if (bean==null) throw new IllegalArgumentException("Invoice not found: "+id);
    }

    add(buildHeader(), BorderLayout.NORTH);
    add(buildCenter(), BorderLayout.CENTER);
    add(buildSouth(), BorderLayout.SOUTH);
    refreshFromBean();
  }
  private JComponent buildHeader(){
    JPanel p = new JPanel(new GridBagLayout());
    p.setBorder(new EmptyBorder(8,8,8,8));
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx=0;c.gridy=0; p.add(new JLabel("Numéro:"), c);
    c.gridx=1; p.add(tfNumber, c);
    c.gridx=2; p.add(new JLabel("Date (YYYY-MM-DD):"), c);
    c.gridx=3; p.add(tfDate, c);
    c.gridx=0;c.gridy=1; p.add(new JLabel("Client:"), c);
    c.gridx=1;c.gridwidth=2; p.add(tfCustomer, c);
    c.gridx=3;c.gridwidth=1; p.add(new JLabel("Statut:"), c);
    c.gridx=4; p.add(cbStatus, c);
    // === CRM-INJECT BEGIN: invoice-contact-row ===
    c.gridx=0;c.gridy=2;c.gridwidth=1; p.add(new JLabel("Contact:"), c);
    c.gridx=1;c.gridwidth=2; p.add(cbContact, c);
    c.gridx=3;c.gridwidth=1;
    // === CRM-INJECT END ===
    return p;
  }
  private JComponent buildCenter(){
    JPanel p = new JPanel(new BorderLayout());
    lineModel = new DocumentLineTableModel(bean.getLines());
    JTable table = new JTable(lineModel);
    lineModel.onChange(lines -> {
      bean.recomputeTotals();
      totalsPanel.setTotals(bean.getTotals());
    });
    JToolBar tb = new JToolBar(); tb.setFloatable(false);
    JButton bAdd = new JButton("Ajouter ligne");
    JButton bDel = new JButton("Supprimer ligne");
    tb.add(bAdd); tb.add(bDel);
    bAdd.addActionListener(e -> lineModel.addEmpty());
    bDel.addActionListener(e -> lineModel.remove(table.getSelectedRow()));
    p.add(tb, BorderLayout.NORTH);
    p.add(new JScrollPane(table), BorderLayout.CENTER);
    return p;
  }
  private JComponent buildSouth(){
    JPanel south = new JPanel(new BorderLayout());
    totalsPanel.setTotals(bean.getTotals());
    south.add(totalsPanel, BorderLayout.CENTER);
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton cancel = new JButton("Annuler");
    JButton save = new JButton("Enregistrer");
    cancel.addActionListener(e -> dispose());
    save.addActionListener(e -> { flushToBean(); ServiceFactory.invoices().save(bean); dispose(); });
    buttons.add(cancel); buttons.add(save);
    south.add(buttons, BorderLayout.SOUTH);
    return south;
  }
  private void refreshFromBean(){
    tfNumber.setText(bean.getNumber()==null? "" : bean.getNumber());
    // === CRM-INJECT BEGIN: invoice-client-refresh ===
    clientBinding.loadFromBean(bean.getClientId(), bean.getCustomerName(), bean.getContactId());
    // === CRM-INJECT END ===
    tfDate.setText(bean.getDate()==null? "" : bean.getDate().toString());
    cbStatus.setSelectedItem(bean.getStatus()==null? "Brouillon" : bean.getStatus());
    totalsPanel.setTotals(bean.getTotals());
  }
  private void flushToBean(){
    bean.setNumber(tfNumber.getText().trim().isEmpty()? null : tfNumber.getText().trim());
    // === CRM-INJECT BEGIN: invoice-client-flush ===
    String customerName = clientBinding.getCustomerName();
    bean.setCustomerName(customerName==null? "" : customerName);
    bean.setClientId(clientBinding.getClientId());
    bean.setContactId(clientBinding.getContactId());
    // === CRM-INJECT END ===
    try { bean.setDate(java.time.LocalDate.parse(tfDate.getText().trim())); } catch(Exception ignore){}
    bean.setStatus(cbStatus.getSelectedItem().toString());
    bean.recomputeTotals();
  }
}
