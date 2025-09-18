package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.ui.icons.IconPickerDialog;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.ui.resources.ResourceTypeEditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Panneau de paramétrage (types de ressources, bibliothèque d'icônes, etc.). */
public class SettingsPanel extends JPanel {

  public SettingsPanel(){
    super(new BorderLayout());

    if (!AccessControl.canViewSettings()){
      JLabel label = new JLabel("Vous n'avez pas accès aux paramètres.", JLabel.CENTER);
      label.setBorder(new EmptyBorder(24, 16, 24, 16));
      add(label, BorderLayout.CENTER);
      return;
    }

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Types de ressources", IconRegistry.small("wrench"), new ResourceTypeEditor());
    tabs.addTab("Types d'intervention", IconRegistry.small("task"), new InterventionTypeEditor());
    tabs.addTab("Bibliothèque d'icônes", IconRegistry.small("settings"), buildIconLibraryPanel());
    add(tabs, BorderLayout.CENTER);
  }

  private JComponent buildIconLibraryPanel(){
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JTextField searchField = new JTextField();
    DefaultListModel<String> model = new DefaultListModel<>();
    JList<String> list = new JList<>(model);
    list.setVisibleRowCount(12);
    list.setCellRenderer(new IconListRenderer());
    List<String> keys = new ArrayList<>(IconRegistry.listKeys());
    keys.sort(String::compareTo);
    updateIconList(model, keys, "");

    searchField.getDocument().addDocumentListener(new DocumentAdapter(){
      @Override public void update(DocumentEvent e){
        String query = searchField.getText();
        updateIconList(model, keys, query == null ? "" : query.trim());
      }
    });

    JButton openPicker = new JButton("Ouvrir le sélecteur…");
    openPicker.addActionListener(e -> {
      Window owner = SwingUtilities.getWindowAncestor(SettingsPanel.this);
      IconPickerDialog dialog = new IconPickerDialog(owner);
      dialog.setVisible(true);
    });

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(openPicker);

    JPanel north = new JPanel(new BorderLayout(4, 4));
    north.add(new JLabel("Rechercher une icône :"), BorderLayout.WEST);
    north.add(searchField, BorderLayout.CENTER);
    panel.add(north, BorderLayout.NORTH);
    panel.add(new JScrollPane(list), BorderLayout.CENTER);
    panel.add(south, BorderLayout.SOUTH);
    return panel;
  }

  private void updateIconList(DefaultListModel<String> model, List<String> keys, String query){
    model.clear();
    String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
    for (String key : keys){
      if (normalized.isBlank() || key.toLowerCase(Locale.ROOT).contains(normalized)){
        model.addElement(key);
      }
    }
  }

  private static class IconListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      String key = value != null ? value.toString() : "";
      label.setText(key);
      label.setIcon(IconRegistry.medium(key));
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
