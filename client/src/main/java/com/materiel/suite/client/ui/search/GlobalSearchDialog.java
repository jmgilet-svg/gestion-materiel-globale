package com.materiel.suite.client.ui.search;

import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;

/**
 * Fenêtre de recherche globale (prototype léger) qui illustre l'usage du renderer avec icônes.
 */
public class GlobalSearchDialog extends JDialog {
  private final JTextField input = new JTextField();
  private final DefaultListModel<Object> model = new DefaultListModel<>();
  private final JList<Object> results = new JList<>(model);
  private final List<IconSearchRenderer.SearchItem> sampleItems = List.of(
      new IconSearchRenderer.SearchItem("quote", "Devis #DV-2024-001", "Client : Axone BTP", "file"),
      new IconSearchRenderer.SearchItem("order", "Commande #BC-458", "Client : Atlas", "pallet"),
      new IconSearchRenderer.SearchItem("delivery", "BL #BL-210", "Destination : Chantier Nord", "truck"),
      new IconSearchRenderer.SearchItem("invoice", "Facture #FA-2024-018", "Montant : 12 500 €", "invoice"),
      new IconSearchRenderer.SearchItem("client", "Client : Société Rimbaud", "Dernier contact : 12/05", "user"),
      new IconSearchRenderer.SearchItem("resource", "Ressource : Groupe électrogène", "Disponibilité : 3 restants", "generator")
  );

  public GlobalSearchDialog(Window owner) {
    super(owner, "Recherche globale", ModalityType.MODELESS);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setSize(600, 420);
    setLocationRelativeTo(owner);
    setLayout(new BorderLayout(0, 8));

    results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    results.setCellRenderer(new IconSearchRenderer());
    results.setVisibleRowCount(10);

    add(buildHeader(), BorderLayout.NORTH);
    add(new JScrollPane(results), BorderLayout.CENTER);

    input.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { updateResults(); }
      @Override public void removeUpdate(DocumentEvent e) { updateResults(); }
      @Override public void changedUpdate(DocumentEvent e) { updateResults(); }
    });
    input.addActionListener(e -> openSelection());

    results.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && results.getSelectedIndex() == -1 && model.getSize() > 0) {
        results.setSelectedIndex(0);
      }
    });
    results.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2 && results.locationToIndex(e.getPoint()) >= 0) {
          openSelection();
        }
      }
    });

    InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    getRootPane().getActionMap().put("close", new AbstractAction() {
      @Override public void actionPerformed(java.awt.event.ActionEvent e) {
        dispose();
      }
    });

    updateResults();
    SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
  }

  private JComponent buildHeader() {
    JPanel panel = new JPanel(new BorderLayout(8, 0));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
    JLabel title = new JLabel("Rechercher (Ctrl+K)");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
    panel.add(title, BorderLayout.WEST);
    input.putClientProperty("JTextField.placeholderText", "Tapez un mot-clé (ex. client, devis…)");
    panel.add(input, BorderLayout.CENTER);
    return panel;
  }

  private void updateResults() {
    String query = input.getText() == null ? "" : input.getText().trim();
    model.clear();
    if (query.length() < 2) {
      model.addElement("info: Tapez au moins 2 caractères pour rechercher");
      results.setSelectedIndex(0);
      return;
    }
    String normalized = query.toLowerCase(Locale.ROOT);
    sampleItems.stream()
        .filter(item -> matches(item, normalized))
        .forEach(model::addElement);
    if (model.isEmpty()) {
      model.addElement("error: Aucun résultat pour \"" + query + "\"");
    }
    results.setSelectedIndex(0);
  }

  private boolean matches(IconSearchRenderer.SearchItem item, String normalizedQuery) {
    return item.label().toLowerCase(Locale.ROOT).contains(normalizedQuery)
        || (item.subtitle() != null && item.subtitle().toLowerCase(Locale.ROOT).contains(normalizedQuery))
        || (item.type() != null && item.type().toLowerCase(Locale.ROOT).contains(normalizedQuery));
  }

  private void openSelection() {
    Object value = results.getSelectedValue();
    if (value instanceof IconSearchRenderer.SearchItem item) {
      String key = item.iconKey() != null && !item.iconKey().isBlank() ? item.iconKey() : "search";
      Toasts.show(this, "Ouverture : " + item.label(), key);
      dispose();
    } else if (value instanceof String str && !str.startsWith("error:")) {
      Toasts.info(this, str.replaceFirst("^[^:]+:\\s*", ""));
    }
  }
}
