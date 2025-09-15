package com.materiel.suite.client.ui.crm;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientsPanel extends JPanel {
  private final JTable tblClients = new JTable();
  private final JTable tblContacts = new JTable();
  private final ClientModel clientModel = new ClientModel();
  private final ContactModel contactModel = new ContactModel();
  private final ClientEditor editor = new ClientEditor();

  public ClientsPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);

    tblClients.setModel(clientModel);
    tblContacts.setModel(contactModel);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    split.setResizeWeight(0.35);
    split.setLeftComponent(new JScrollPane(tblClients));

    JPanel right = new JPanel(new BorderLayout());
    right.add(editor, BorderLayout.NORTH);
    JPanel contactBox = new JPanel(new BorderLayout());
    contactBox.setBorder(new EmptyBorder(6,6,6,6));
    contactBox.add(new JLabel("Contacts"), BorderLayout.NORTH);
    contactBox.add(new JScrollPane(tblContacts), BorderLayout.CENTER);
    contactBox.add(buildContactBar(), BorderLayout.SOUTH);
    right.add(contactBox, BorderLayout.CENTER);

    split.setRightComponent(right);
    add(split, BorderLayout.CENTER);

    tblClients.getSelectionModel().addListSelectionListener(e -> {
      int i = tblClients.getSelectedRow();
      if (i>=0) {
        Client c = clientModel.items.get(i);
        editor.setClient(c);
        loadContacts(c.getId());
      }
    });
    reloadClients();
  }

  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    JButton add = new JButton("+ Client");
    JButton save = new JButton("Enregistrer");
    JButton del = new JButton("Supprimer");
    add.addActionListener(e -> {
      Client c = new Client(); c.setId(UUID.randomUUID()); c.setName("Nouveau client");
      ServiceFactory.clients().save(c);
      reloadClients();
      selectClient(c.getId());
    });
    save.addActionListener(e -> {
      Client c = editor.toClient();
      if (c!=null){ ServiceFactory.clients().save(c); reloadClients(); selectClient(c.getId()); }
    });
    del.addActionListener(e -> {
      int i = tblClients.getSelectedRow();
      if (i>=0){
        Client c = clientModel.items.get(i);
        int ok = JOptionPane.showConfirmDialog(this, "Supprimer "+c.getName()+" ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
        if (ok==JOptionPane.OK_OPTION){ ServiceFactory.clients().delete(c.getId()); reloadClients(); }
      }
    });
    bar.add(add); bar.add(save); bar.add(del);
    return bar;
  }
  private JComponent buildContactBar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton add = new JButton("+ Contact");
    JButton edit = new JButton("Modifier");
    JButton del = new JButton("Supprimer");
    add.addActionListener(e -> editContact(null));
    edit.addActionListener(e -> {
      int i = tblContacts.getSelectedRow();
      if (i>=0) editContact(contactModel.items.get(i));
    });
    del.addActionListener(e -> {
      int i = tblContacts.getSelectedRow();
      int j = tblClients.getSelectedRow();
      if (i>=0 && j>=0){
        Client c = clientModel.items.get(j);
        Contact ct = contactModel.items.get(i);
        int ok = JOptionPane.showConfirmDialog(this, "Supprimer "+ct+" ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
        if (ok==JOptionPane.OK_OPTION){ ServiceFactory.clients().deleteContact(c.getId(), ct.getId()); loadContacts(c.getId()); }
      }
    });
    bar.add(add); bar.add(edit); bar.add(del);
    return bar;
  }

  private void editContact(Contact ct){
    int j = tblClients.getSelectedRow();
    if (j<0){ JOptionPane.showMessageDialog(this,"Sélectionnez d'abord un client."); return; }
    Client c = clientModel.items.get(j);
    JTextField fn = new JTextField(ct!=null? ct.getFirstName():"", 12);
    JTextField ln = new JTextField(ct!=null? ct.getLastName():"", 12);
    JTextField em = new JTextField(ct!=null? ct.getEmail():"", 18);
    JTextField ph = new JTextField(ct!=null? ct.getPhone():"", 12);
    JTextField ro = new JTextField(ct!=null? ct.getRole():"", 12);
    JCheckBox ar = new JCheckBox("Archivé", ct!=null && ct.isArchived());
    Object[] msg = {"Prénom:", fn, "Nom:", ln, "Email:", em, "Téléphone:", ph, "Rôle:", ro, ar};
    int ok = JOptionPane.showConfirmDialog(this, msg, (ct==null? "Nouveau contact":"Modifier contact"), JOptionPane.OK_CANCEL_OPTION);
    if (ok==JOptionPane.OK_OPTION){
      Contact x = (ct==null? new Contact() : ct);
      if (x.getId()==null) x.setId(UUID.randomUUID());
      x.setClientId(c.getId());
      x.setFirstName(fn.getText().trim()); x.setLastName(ln.getText().trim());
      x.setEmail(em.getText().trim()); x.setPhone(ph.getText().trim());
      x.setRole(ro.getText().trim()); x.setArchived(ar.isSelected());
      ServiceFactory.clients().saveContact(c.getId(), x);
      loadContacts(c.getId());
    }
  }

  private void reloadClients(){
    clientModel.items = new ArrayList<>(ServiceFactory.clients().list());
    clientModel.fireTableDataChanged();
    if (!clientModel.items.isEmpty()) {
      tblClients.setRowSelectionInterval(0,0);
      editor.setClient(clientModel.items.get(0));
      loadContacts(clientModel.items.get(0).getId());
    } else {
      editor.setClient(null);
      contactModel.items = new ArrayList<>();
      contactModel.fireTableDataChanged();
    }
  }
  private void selectClient(UUID id){
    for (int i=0;i<clientModel.items.size();i++){
      if (clientModel.items.get(i).getId().equals(id)){
        tblClients.setRowSelectionInterval(i,i);
        editor.setClient(clientModel.items.get(i));
        loadContacts(id);
        break;
      }
    }
  }
  private void loadContacts(UUID clientId){
    contactModel.items = new ArrayList<>(ServiceFactory.clients().listContacts(clientId));
    contactModel.fireTableDataChanged();
  }

  /* Models */
  private static class ClientModel extends AbstractTableModel {
    List<Client> items = new ArrayList<>();
    String[] cols = {"Nom", "Code", "Email", "Téléphone", "TVA"};
    @Override public int getRowCount(){ return items.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }
    @Override public Object getValueAt(int r, int c){
      Client x = items.get(r);
      return switch(c){
        case 0 -> x.getName();
        case 1 -> x.getCode();
        case 2 -> x.getEmail();
        case 3 -> x.getPhone();
        case 4 -> x.getVatNumber();
        default -> "";
      };
    }
  }
  private static class ContactModel extends AbstractTableModel {
    List<Contact> items = new ArrayList<>();
    String[] cols = {"Prénom", "Nom", "Email", "Téléphone", "Rôle", "Archivé"};
    @Override public int getRowCount(){ return items.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }
    @Override public Object getValueAt(int r, int c){
      Contact x = items.get(r);
      return switch(c){
        case 0 -> x.getFirstName();
        case 1 -> x.getLastName();
        case 2 -> x.getEmail();
        case 3 -> x.getPhone();
        case 4 -> x.getRole();
        case 5 -> x.isArchived();
        default -> "";
      };
    }
  }

  /* Editor */
  private static class ClientEditor extends JPanel {
    private JTextField name = new JTextField(20);
    private JTextField code = new JTextField(10);
    private JTextField email = new JTextField(16);
    private JTextField phone = new JTextField(14);
    private JTextField vat = new JTextField(12);
    private JTextArea bill = new JTextArea(3, 28);
    private JTextArea ship = new JTextArea(3, 28);
    private JTextArea notes = new JTextArea(4, 28);
    private UUID id;

    public ClientEditor(){
      super(new GridBagLayout());
      setBorder(new EmptyBorder(8,8,8,8));
      bill.setLineWrap(true); bill.setWrapStyleWord(true);
      ship.setLineWrap(true); ship.setWrapStyleWord(true);
      notes.setLineWrap(true); notes.setWrapStyleWord(true);
      int y=0;
      addL("Nom",0,y); add(name,1,y++);
      addL("Code",0,y); add(code,1,y++);
      addL("Email",0,y); add(email,1,y++);
      addL("Téléphone",0,y); add(phone,1,y++);
      addL("TVA",0,y); add(vat,1,y++);
      addL("Adresse facturation",0,y); add(new JScrollPane(bill),1,y++);
      addL("Adresse livraison",0,y); add(new JScrollPane(ship),1,y++);
      addL("Notes",0,y); add(new JScrollPane(notes),1,y++);
      GridBagConstraints g = new GridBagConstraints(); g.gridx=0; g.gridy=y; g.gridwidth=2; g.weightx=1; g.weighty=1; g.fill=GridBagConstraints.BOTH;
      add(Box.createGlue(), g);
    }
    private void addL(String t, int x, int y){
      GridBagConstraints g = new GridBagConstraints(); g.gridx=x; g.gridy=y; g.anchor=GridBagConstraints.LINE_START; g.insets=new Insets(4,4,4,8);
      add(new JLabel(t), g);
    }
    private void add(Component c, int x, int y){
      GridBagConstraints g = new GridBagConstraints(); g.gridx=x; g.gridy=y; g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1; g.insets=new Insets(4,4,4,4);
      add(c, g);
    }
    public void setClient(Client c){
      if (c==null){ id=null; name.setText(""); code.setText(""); email.setText(""); phone.setText(""); vat.setText(""); bill.setText(""); ship.setText(""); notes.setText(""); return; }
      id = c.getId(); name.setText(nz(c.getName())); code.setText(nz(c.getCode())); email.setText(nz(c.getEmail())); phone.setText(nz(c.getPhone())); vat.setText(nz(c.getVatNumber()));
      bill.setText(nz(c.getBillingAddress())); ship.setText(nz(c.getShippingAddress())); notes.setText(nz(c.getNotes()));
    }
    public Client toClient(){
      if (name.getText().trim().isEmpty()){ JOptionPane.showMessageDialog(this,"Le nom du client est requis."); return null; }
      Client c = new Client(); c.setId(id!=null? id : UUID.randomUUID());
      c.setName(name.getText().trim()); c.setCode(code.getText().trim()); c.setEmail(email.getText().trim()); c.setPhone(phone.getText().trim()); c.setVatNumber(vat.getText().trim());
      c.setBillingAddress(bill.getText()); c.setShippingAddress(ship.getText()); c.setNotes(notes.getText());
      return c;
    }
    private static String nz(String s){ return s==null? "": s; }
  }
}

