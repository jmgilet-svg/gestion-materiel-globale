package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/** Sélecteur multi-contacts pour associer des interlocuteurs client. */
public class ContactMultiPicker extends JPanel {
  private final JTextField searchField = new JTextField();
  private final ContactTableModel availableModel = new ContactTableModel();
  private final JTable availableTable = new JTable(availableModel);
  private final ContactTableModel selectedModel = new ContactTableModel();
  private final JTable selectedTable = new JTable(selectedModel);

  private final List<Contact> allContacts = new ArrayList<>();
  private final List<Contact> availableContacts = new ArrayList<>();
  private final List<Contact> selectedContacts = new ArrayList<>();

  public ContactMultiPicker(){
    super(new BorderLayout(8, 8));
    buildNorth();
    buildTables();
    buildButtons();
    refreshTables();
  }

  private void buildNorth(){
    JPanel north = new JPanel(new BorderLayout(6, 0));
    north.add(new JLabel(IconRegistry.small("user")), BorderLayout.WEST);
    north.add(searchField, BorderLayout.CENTER);
    add(north, BorderLayout.NORTH);
    searchField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override public void update(DocumentEvent e){ refreshTables(); }
    });
  }

  private void buildTables(){
    configureTable(availableTable);
    configureTable(selectedTable);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        wrap("Disponibles", availableTable),
        wrap("Sélectionnés", selectedTable));
    split.setResizeWeight(0.5);
    add(split, BorderLayout.CENTER);
  }

  private void buildButtons(){
    JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
    JButton add = new JButton("→ Ajouter");
    JButton remove = new JButton("← Retirer");
    buttons.add(add);
    buttons.add(remove);
    add(buttons, BorderLayout.EAST);
    add.addActionListener(e -> move(availableTable, availableContacts, selectedContacts));
    remove.addActionListener(e -> move(selectedTable, selectedContacts, availableContacts));
  }

  private void configureTable(JTable table){
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setFillsViewportHeight(true);
    table.setRowHeight(24);
    table.getColumnModel().getColumn(0).setPreferredWidth(40);
    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer());
  }

  private JComponent wrap(String title, JTable table){
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(new JScrollPane(table), BorderLayout.CENTER);
    return panel;
  }

  private void move(JTable source, List<Contact> from, List<Contact> to){
    int[] rows = source.getSelectedRows();
    if (rows.length == 0){
      return;
    }
    List<Contact> moved = new ArrayList<>();
    for (int viewRow : rows){
      int modelRow = source.convertRowIndexToModel(viewRow);
      Contact contact = (source.getModel() instanceof ContactTableModel model)
          ? model.getAt(modelRow)
          : null;
      if (contact != null){
        moved.add(contact);
      }
    }
    if (moved.isEmpty()){
      return;
    }
    from.removeAll(moved);
    for (Contact contact : moved){
      if (!containsContact(to, contact.getId())){
        to.add(contact);
      }
    }
    rebuildAvailableList();
    refreshTables();
  }

  private boolean containsContact(List<Contact> list, UUID id){
    if (id == null){
      return false;
    }
    return list.stream().anyMatch(c -> id.equals(c.getId()));
  }

  private void rebuildAvailableList(){
    availableContacts.clear();
    for (Contact contact : allContacts){
      UUID id = contact.getId();
      if (id != null && containsContact(selectedContacts, id)){
        continue;
      }
      availableContacts.add(contact);
    }
  }

  private void refreshTables(){
    String query = searchField.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<Contact> filtered = availableContacts.stream()
        .filter(c -> normalized.isBlank() || matches(c, normalized))
        .collect(Collectors.toList());
    availableModel.setRows(filtered);
    selectedModel.setRows(new ArrayList<>(selectedContacts));
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
    return phone != null && phone.toLowerCase(Locale.ROOT).contains(query);
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

  public void setContacts(List<Contact> contacts){
    allContacts.clear();
    if (contacts != null){
      for (Contact contact : contacts){
        if (contact != null){
          allContacts.add(copy(contact));
        }
      }
    }
    rebuildAvailableList();
    refreshTables();
  }

  public void setSelectedContacts(List<Contact> contacts){
    selectedContacts.clear();
    if (contacts != null){
      for (Contact contact : contacts){
        if (contact != null){
          selectedContacts.add(copy(contact));
        }
      }
    }
    rebuildAvailableList();
    refreshTables();
  }

  public List<Contact> getSelectedContacts(){
    List<Contact> list = new ArrayList<>();
    for (Contact contact : selectedContacts){
      list.add(copy(contact));
    }
    return list;
  }

  private static Contact copy(Contact src){
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

  private static class ContactTableModel extends AbstractTableModel {
    private final List<Contact> rows = new ArrayList<>();
    private final String[] columns = {"", "Nom", "Email", "Téléphone"};

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return columns.length; }
    @Override public String getColumnName(int column){ return columns[column]; }

    @Override public Object getValueAt(int rowIndex, int columnIndex){
      Contact contact = rows.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> "user";
        case 1 -> fullName(contact);
        case 2 -> contact.getEmail() != null ? contact.getEmail() : "";
        case 3 -> contact.getPhone() != null ? contact.getPhone() : "";
        default -> "";
      };
    }

    public Contact getAt(int index){
      return rows.get(index);
    }

    public void setRows(List<Contact> contacts){
      rows.clear();
      if (contacts != null){
        rows.addAll(contacts);
      }
      fireTableDataChanged();
    }
  }

  private static class IconCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setIcon(IconRegistry.small("user"));
      label.setText("");
      return label;
    }
  }

  private abstract static class DocumentAdapter implements DocumentListener {
    @Override public void insertUpdate(DocumentEvent e){ update(e); }
    @Override public void removeUpdate(DocumentEvent e){ update(e); }
    @Override public void changedUpdate(DocumentEvent e){ update(e); }
    public abstract void update(DocumentEvent e);
  }
}
