package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Sélecteur de contacts client avec recherche et sélection multiple par cases à cocher. */
public class ContactPickerPanel extends JPanel {
  private final JTextField searchField = new JTextField();
  private final JButton selectAllButton = new JButton("Tout");
  private final JButton clearAllButton = new JButton("Aucun");
  private final ContactTableModel model = new ContactTableModel();
  private final JTable table = new JTable(model);

  private final List<Contact> allContacts = new ArrayList<>();
  private final Map<String, Contact> selectedContacts = new LinkedHashMap<>();
  private boolean readOnly;

  public ContactPickerPanel(){
    super(new BorderLayout(8, 8));
    buildNorth();
    buildTable();
    installListeners();
    applyFilter();
  }

  private void buildNorth(){
    JPanel north = new JPanel(new BorderLayout(6, 0));

    JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
    searchPanel.add(new JLabel(IconRegistry.small("search")), BorderLayout.WEST);
    searchPanel.add(searchField, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    actions.add(selectAllButton);
    actions.add(clearAllButton);

    north.add(searchPanel, BorderLayout.CENTER);
    north.add(actions, BorderLayout.EAST);
    add(north, BorderLayout.NORTH);
  }

  private void buildTable(){
    table.setFillsViewportHeight(true);
    table.setRowHeight(24);
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setMaxWidth(50);
    add(new JScrollPane(table), BorderLayout.CENTER);
  }

  private void installListeners(){
    searchField.getDocument().addDocumentListener(new DocumentAdapter(){
      @Override public void update(DocumentEvent e){ applyFilter(); }
    });
    selectAllButton.addActionListener(e -> selectFiltered(true));
    clearAllButton.addActionListener(e -> selectFiltered(false));
  }

  private void selectFiltered(boolean select){
    if (readOnly){
      return;
    }
    List<Contact> rows = model.rows();
    if (rows.isEmpty()){
      return;
    }
    for (Contact contact : rows){
      if (contact == null){
        continue;
      }
      String key = keyOf(contact);
      if (select){
        selectedContacts.put(key, copy(contact));
      } else {
        selectedContacts.remove(key);
      }
    }
    model.refreshAll();
  }

  public void setContacts(List<Contact> contacts){
    allContacts.clear();
    if (contacts != null){
      for (Contact contact : contacts){
        if (contact == null){
          continue;
        }
        allContacts.add(copy(contact));
      }
    }
    ensureSelectedContactsPresent();
    applyFilter();
  }

  public void setSelectedContacts(List<Contact> contacts){
    selectedContacts.clear();
    if (contacts != null){
      for (Contact contact : contacts){
        if (contact == null){
          continue;
        }
        Contact copy = copy(contact);
        selectedContacts.put(keyOf(copy), copy);
      }
    }
    ensureSelectedContactsPresent();
    applyFilter();
  }

  public List<Contact> getSelectedContacts(){
    List<Contact> list = new ArrayList<>();
    for (Contact contact : selectedContacts.values()){
      if (contact != null){
        list.add(copy(contact));
      }
    }
    return list;
  }

  public void setReadOnly(boolean readOnly){
    this.readOnly = readOnly;
    searchField.setEditable(!readOnly);
    searchField.setEnabled(!readOnly);
    selectAllButton.setEnabled(!readOnly);
    clearAllButton.setEnabled(!readOnly);
    table.setEnabled(!readOnly);
    table.setRowSelectionAllowed(!readOnly);
    if (readOnly){
      table.clearSelection();
    }
  }

  private void ensureSelectedContactsPresent(){
    for (Contact selected : selectedContacts.values()){
      if (selected == null){
        continue;
      }
      if (!containsContact(selected)){
        allContacts.add(copy(selected));
      }
    }
  }

  private boolean containsContact(Contact contact){
    String key = keyOf(contact);
    for (Contact existing : allContacts){
      if (Objects.equals(key, keyOf(existing))){
        return true;
      }
    }
    return false;
  }

  private void applyFilter(){
    String query = searchField.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<Contact> filtered = new ArrayList<>();
    for (Contact contact : allContacts){
      if (contact == null){
        continue;
      }
      if (!normalized.isBlank() && !matches(contact, normalized)){
        continue;
      }
      filtered.add(contact);
    }
    model.setRows(filtered);
  }

  private boolean matches(Contact contact, String query){
    if (contact == null){
      return false;
    }
    String name = fullName(contact).toLowerCase(Locale.ROOT);
    if (name.contains(query)){
      return true;
    }
    String email = contact.getEmail();
    if (email != null && email.toLowerCase(Locale.ROOT).contains(query)){
      return true;
    }
    String phone = contact.getPhone();
    if (phone != null && phone.toLowerCase(Locale.ROOT).contains(query)){
      return true;
    }
    String role = contact.getRole();
    return role != null && role.toLowerCase(Locale.ROOT).contains(query);
  }

  private String keyOf(Contact contact){
    if (contact == null){
      return "";
    }
    UUID id = contact.getId();
    if (id != null){
      return id.toString();
    }
    String email = contact.getEmail();
    if (email != null && !email.isBlank()){
      return "email:" + email.trim().toLowerCase(Locale.ROOT);
    }
    String phone = contact.getPhone();
    if (phone != null && !phone.isBlank()){
      return "phone:" + phone.replaceAll("\\s+", "");
    }
    return "name:" + fullName(contact).trim().toLowerCase(Locale.ROOT);
  }

  private static String fullName(Contact contact){
    StringBuilder sb = new StringBuilder();
    if (contact.getFirstName() != null && !contact.getFirstName().isBlank()){
      sb.append(contact.getFirstName().trim());
    }
    if (contact.getLastName() != null && !contact.getLastName().isBlank()){
      if (!sb.isEmpty()){
        sb.append(' ');
      }
      sb.append(contact.getLastName().trim());
    }
    String value = sb.toString().trim();
    if (!value.isEmpty()){
      return value;
    }
    return contact.getEmail() != null ? contact.getEmail() : "(contact)";
  }

  private Contact copy(Contact src){
    Contact copy = new Contact();
    copy.setId(src.getId());
    copy.setClientId(src.getClientId());
    copy.setFirstName(src.getFirstName());
    copy.setLastName(src.getLastName());
    copy.setEmail(src.getEmail());
    copy.setPhone(src.getPhone());
    copy.setRole(src.getRole());
    copy.setArchived(src.isArchived());
    return copy;
  }

  private class ContactTableModel extends AbstractTableModel {
    private final List<Contact> rows = new ArrayList<>();
    private final String[] columns = {"", "Nom", "Email", "Téléphone"};

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return columns.length; }
    @Override public String getColumnName(int column){ return columns[column]; }

    @Override public Class<?> getColumnClass(int columnIndex){
      return columnIndex == 0 ? Boolean.class : Object.class;
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex){
      return !readOnly && columnIndex == 0;
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      Contact contact = rows.get(rowIndex);
      return switch (columnIndex){
        case 0 -> selectedContacts.containsKey(keyOf(contact));
        case 1 -> fullName(contact);
        case 2 -> contact.getEmail() != null ? contact.getEmail() : "";
        case 3 -> contact.getPhone() != null ? contact.getPhone() : "";
        default -> "";
      };
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex){
      if (columnIndex != 0){
        return;
      }
      Contact contact = rows.get(rowIndex);
      boolean selected = Boolean.TRUE.equals(aValue);
      String key = keyOf(contact);
      if (selected){
        selectedContacts.put(key, copy(contact));
      } else {
        selectedContacts.remove(key);
      }
      fireTableRowsUpdated(rowIndex, rowIndex);
    }

    void setRows(List<Contact> list){
      rows.clear();
      if (list != null){
        rows.addAll(list);
      }
      fireTableDataChanged();
    }

    List<Contact> rows(){
      return List.copyOf(rows);
    }

    void refreshAll(){
      if (!rows.isEmpty()){
        fireTableRowsUpdated(0, rows.size() - 1);
      }
    }
  }

  private abstract static class DocumentAdapter implements DocumentListener {
    @Override public void insertUpdate(DocumentEvent e){ update(e); }
    @Override public void removeUpdate(DocumentEvent e){ update(e); }
    @Override public void changedUpdate(DocumentEvent e){ update(e); }
    public abstract void update(DocumentEvent e);
  }
}
