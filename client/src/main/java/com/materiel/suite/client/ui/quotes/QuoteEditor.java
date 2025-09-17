package com.materiel.suite.client.ui.quotes;

import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.DocumentLine;
import com.materiel.suite.client.model.Quote;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.doc.DocumentLineTableModel;
import com.materiel.suite.client.ui.doc.DocumentTotalsPanel;
// === CRM-INJECT BEGIN: quote-client-binding-import ===
import com.materiel.suite.client.ui.doc.ClientContactBinding;
// === CRM-INJECT END ===

import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.UUID;

public class QuoteEditor extends JDialog {
  private final JTextField tfNumber = new JTextField(12);
  private final JTextField tfCustomer = new JTextField(24);
  private final JTextField tfDate = new JTextField(10);
  private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Brouillon","Envoyé","Accepté","Refusé","Expiré"});
  // === CRM-INJECT BEGIN: quote-client-binding-fields ===
  private final JComboBox<Contact> cbContact = new JComboBox<>();
  private final ClientContactBinding clientBinding = new ClientContactBinding(tfCustomer, cbContact);
  // === CRM-INJECT END ===
  private final DocumentTotalsPanel totalsPanel = new DocumentTotalsPanel();
  private DocumentLineTableModel lineModel;
  private Quote bean;

  public QuoteEditor(Window owner, UUID id){
    super(owner, "Édition devis", ModalityType.APPLICATION_MODAL);
    setSize(900, 560);
    setLocationRelativeTo(owner);
    setLayout(new BorderLayout());

    if (id==null){
      bean = new Quote(java.util.UUID.randomUUID(), null, LocalDate.now(), "", "Brouillon");
      bean.getLines().add(new DocumentLine("Nouvelle ligne",1,"",0,0,20));
      bean.recomputeTotals();
    } else {
      bean = ServiceFactory.quotes().get(id);
      if (bean==null) throw new IllegalArgumentException("Quote not found: "+id);
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
    // === CRM-INJECT BEGIN: quote-contact-row ===
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
    JButton toOrder = new JButton("Créer BC…");
    cancel.addActionListener(e -> dispose());
    save.addActionListener(e -> {
      flushToBean();
      ServiceFactory.quotes().save(bean);
      Window anchor = getOwner() != null ? getOwner() : this;
      Toasts.success(anchor, "Devis enregistré");
      dispose();
    });
    toOrder.addActionListener(e -> {
      flushToBean(); ServiceFactory.quotes().save(bean);
      var o = ServiceFactory.orders().createFromQuote(bean.getId());
      JOptionPane.showMessageDialog(this, "BC créé : "+o.getNumber());
    });
    buttons.add(toOrder);
    buttons.add(cancel);
    buttons.add(save);
    south.add(buttons, BorderLayout.SOUTH);
    return south;
  }

  private void refreshFromBean(){
    tfNumber.setText(bean.getNumber()==null? "" : bean.getNumber());
    // === CRM-INJECT BEGIN: quote-client-refresh ===
    clientBinding.loadFromBean(bean.getClientId(), bean.getCustomerName(), bean.getContactId());
    // === CRM-INJECT END ===
    tfDate.setText(bean.getDate()==null? "" : bean.getDate().toString());
    cbStatus.setSelectedItem(bean.getStatus()==null? "Brouillon" : bean.getStatus());
    totalsPanel.setTotals(bean.getTotals());
  }
  private void flushToBean(){
    bean.setNumber(tfNumber.getText().trim().isEmpty()? null : tfNumber.getText().trim());
    // === CRM-INJECT BEGIN: quote-client-flush ===
    String customerName = clientBinding.getCustomerName();
    bean.setCustomerName(customerName==null? "" : customerName);
    bean.setClientId(clientBinding.getClientId());
    bean.setContactId(clientBinding.getContactId());
    // === CRM-INJECT END ===
    try { bean.setDate(LocalDate.parse(tfDate.getText().trim())); } catch(Exception ignore){}
    bean.setStatus(cbStatus.getSelectedItem().toString());
    bean.recomputeTotals();
  }
}
