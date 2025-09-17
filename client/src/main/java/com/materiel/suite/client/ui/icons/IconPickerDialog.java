package com.materiel.suite.client.ui.icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Fenêtre modale permettant de sélectionner une icône parmi la bibliothèque SVG. */
public class IconPickerDialog extends JDialog {
  private String selectedKey;
  private final JTextField searchField = new JTextField();
  private final JPanel grid = new JPanel(new GridLayout(0, 6, 8, 8));
  private final List<String> keys = IconRegistry.listKeys();

  public IconPickerDialog(Window owner){
    super(owner, "Choisir une icône", ModalityType.APPLICATION_MODAL);
    buildUI();
    refresh();
    setMinimumSize(new Dimension(520, 420));
    setLocationRelativeTo(owner);
  }

  private void buildUI(){
    setLayout(new BorderLayout(8, 8));

    JPanel top = new JPanel(new BorderLayout(4, 4));
    top.add(new JLabel("Rechercher :"), BorderLayout.WEST);
    top.add(searchField, BorderLayout.CENTER);

    JScrollPane scroll = new JScrollPane(grid);
    scroll.setBorder(BorderFactory.createEmptyBorder());

    JButton close = new JButton("Fermer");
    close.addActionListener(e -> dispose());
    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    south.add(close);

    add(top, BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    searchField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e){
        refresh();
      }
    });
  }

  private void refresh(){
    grid.removeAll();
    String query = searchField.getText();
    String lower = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
    List<String> filtered = keys.stream()
        .filter(key -> lower.isEmpty() || key.toLowerCase(Locale.ROOT).contains(lower))
        .collect(Collectors.toList());
    for (String key : filtered){
      grid.add(createButton(key));
    }
    grid.revalidate();
    grid.repaint();
  }

  private JButton createButton(String key){
    JButton button = new JButton(key, IconRegistry.medium(key));
    button.setHorizontalAlignment(SwingConstants.LEFT);
    button.addActionListener(e -> {
      selectedKey = key;
      dispose();
    });
    return button;
  }

  /** Ouvre la fenêtre et renvoie la clé sélectionnée ou {@code null} si annulé. */
  public String pick(){
    setVisible(true);
    return selectedKey;
  }
}

