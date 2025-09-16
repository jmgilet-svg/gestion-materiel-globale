package com.materiel.suite.client.ui.doc;

// === CRM-INJECT BEGIN: client-contact-binding ===
import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.ClientService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ClientContactBinding {
  private final JTextField clientField;
  private final JComboBox<Contact> contactCombo;
  private final DefaultComboBoxModel<Contact> contactModel = new DefaultComboBoxModel<>();
  private final ClientService clientService;
  private final List<Client> allClients = new ArrayList<>();
  private final DefaultListModel<Client> suggestionModel = new DefaultListModel<>();
  private final JList<Client> suggestionList = new JList<>(suggestionModel);
  private final JPopupMenu suggestionPopup = new JPopupMenu();
  private UUID selectedClientId;
  private UUID selectedContactId;
  private String selectedClientName = "";
  private boolean updatingField;
  private boolean updatingCombo;

  public ClientContactBinding(JTextField clientField, JComboBox<Contact> contactCombo) {
    this.clientField = clientField;
    this.contactCombo = contactCombo;
    this.clientService = ServiceFactory.clients();
    this.contactCombo.setModel(contactModel);
    this.contactCombo.setEditable(false);
    this.contactCombo.setEnabled(false);
    this.contactCombo.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText("(Aucun)");
        } else if (value instanceof Contact ct) {
          String fn = ct.getFirstName() == null ? "" : ct.getFirstName().trim();
          String ln = ct.getLastName() == null ? "" : ct.getLastName().trim();
          String joined = (fn + " " + ln).trim();
          setText(joined.isEmpty() ? "(Sans nom)" : joined);
        }
        return c;
      }
    });
    this.contactCombo.addActionListener(e -> {
      if (updatingCombo) return;
      Object sel = contactCombo.getSelectedItem();
      if (sel instanceof Contact ct) {
        selectedContactId = ct.getId();
      } else {
        selectedContactId = null;
      }
    });

    suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    suggestionList.setFocusable(false);
    suggestionList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Client client) {
          setText(client.getName() == null ? "" : client.getName());
        }
        return c;
      }
    });
    suggestionList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          Client client = suggestionList.getSelectedValue();
          if (client != null) {
            accept(client);
          }
        }
      }
    });

    JScrollPane scroll = new JScrollPane(suggestionList);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    suggestionList.setVisibleRowCount(6);
    scroll.setPreferredSize(new Dimension(Math.max(clientField.getPreferredSize().width, 240), 140));
    Border border = BorderFactory.createLineBorder(new Color(180, 180, 180));
    suggestionPopup.setBorder(border);
    suggestionPopup.setFocusable(false);
    suggestionPopup.add(scroll);

    clientField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
      @Override public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
      @Override public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
    });
    clientField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
          if (!suggestionPopup.isVisible()) {
            updateSuggestions();
          } else if (suggestionModel.getSize() > 0) {
            int idx = suggestionList.getSelectedIndex();
            if (idx < suggestionModel.getSize() - 1) {
              suggestionList.setSelectedIndex(idx + 1);
            }
          }
          e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_UP && suggestionPopup.isVisible()) {
          int idx = suggestionList.getSelectedIndex();
          if (idx > 0) {
            suggestionList.setSelectedIndex(idx - 1);
          }
          e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && suggestionPopup.isVisible()) {
          Client client = suggestionList.getSelectedValue();
          if (client != null) {
            accept(client);
            e.consume();
          }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && suggestionPopup.isVisible()) {
          suggestionPopup.setVisible(false);
          e.consume();
        }
      }
    });
    clientField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        suggestionPopup.setVisible(false);
      }
    });

    reloadClients();
    loadContacts(null, null);
  }

  public void loadFromBean(UUID clientId, String customerName, UUID contactId) {
    if (clientId != null && clientService != null) {
      Client client = findClient(clientId);
      selectedClientId = clientId;
      selectedClientName = client != null && client.getName() != null ? client.getName() : (customerName == null ? "" : customerName);
      updatingField = true;
      clientField.setText(selectedClientName);
      updatingField = false;
      loadContacts(clientId, contactId);
    } else {
      selectedClientId = null;
      selectedClientName = customerName == null ? "" : customerName;
      updatingField = true;
      clientField.setText(selectedClientName);
      updatingField = false;
      loadContacts(null, null);
    }
  }

  public UUID getClientId() {
    return selectedClientId;
  }

  public UUID getContactId() {
    return selectedContactId;
  }

  public String getCustomerName() {
    String text = clientField.getText().trim();
    return text.isEmpty() ? null : text;
  }

  private void updateSuggestions() {
    if (updatingField) {
      return;
    }
    String text = clientField.getText().trim();
    if (selectedClientId != null && !text.equals(selectedClientName)) {
      selectedClientId = null;
      selectedClientName = text;
      loadContacts(null, null);
    } else {
      selectedClientName = text;
    }
    if (text.isEmpty()) {
      suggestionPopup.setVisible(false);
      return;
    }
    if (allClients.isEmpty()) {
      reloadClients();
    }
    String lower = text.toLowerCase(Locale.ROOT);
    suggestionModel.clear();
    for (Client client : allClients) {
      String name = client.getName();
      if (name != null && name.toLowerCase(Locale.ROOT).contains(lower)) {
        suggestionModel.addElement(client);
        if (suggestionModel.size() >= 8) {
          break;
        }
      }
    }
    if (suggestionModel.isEmpty()) {
      suggestionPopup.setVisible(false);
    } else {
      suggestionList.setSelectedIndex(0);
      showPopup();
    }
  }

  private void showPopup() {
    suggestionPopup.setVisible(false);
    suggestionPopup.show(clientField, 0, clientField.getHeight());
  }

  private void accept(Client client) {
    suggestionPopup.setVisible(false);
    if (client == null) {
      return;
    }
    selectedClientId = client.getId();
    selectedClientName = client.getName() == null ? "" : client.getName();
    updatingField = true;
    clientField.setText(selectedClientName);
    updatingField = false;
    loadContacts(selectedClientId, null);
  }

  private void loadContacts(UUID clientId, UUID contactId) {
    updatingCombo = true;
    contactModel.removeAllElements();
    contactModel.addElement(null);
    selectedContactId = null;
    if (clientId != null) {
      try {
        List<Contact> contacts = clientService.listContacts(clientId);
        if (contacts != null) {
          for (Contact contact : contacts) {
            contactModel.addElement(contact);
          }
        }
      } catch (Exception ignore) {
      }
    }
    if (contactId != null) {
      for (int i = 0; i < contactModel.getSize(); i++) {
        Contact contact = contactModel.getElementAt(i);
        if (contact != null && contactId.equals(contact.getId())) {
          contactCombo.setSelectedIndex(i);
          selectedContactId = contact.getId();
          break;
        }
      }
    } else {
      contactCombo.setSelectedIndex(0);
    }
    contactCombo.setEnabled(clientId != null && contactModel.getSize() > 1);
    updatingCombo = false;
  }

  private Client findClient(UUID clientId) {
    for (Client client : allClients) {
      if (client != null && client.getId() != null && client.getId().equals(clientId)) {
        return client;
      }
    }
    if (clientService == null) {
      return null;
    }
    try {
      Client fetched = clientService.get(clientId);
      if (fetched != null) {
        allClients.add(fetched);
      }
      return fetched;
    } catch (Exception ignore) {
      return null;
    }
  }

  private void reloadClients() {
    if (clientService == null) {
      allClients.clear();
      return;
    }
    try {
      List<Client> clients = clientService.list();
      allClients.clear();
      if (clients != null) {
        allClients.addAll(clients);
      }
    } catch (Exception ignore) {
      allClients.clear();
    }
  }
}
// === CRM-INJECT END ===
