package com.materiel.suite.client.ui.resources;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/** Sélecteur simple pour associer une icône (emoji ou caractère) à une ressource. */
public class ResourceIconPicker extends JPanel {
  private static final List<String> PALETTE = Arrays.asList(
      "🏗️", "🚚", "🚛", "👷", "🧰", "🛠️", "🔧", "🔩", "⚙️", "🏷️"
  );

  private final JTextField custom = new JTextField(4);
  private String value;

  public ResourceIconPicker(){
    super(new FlowLayout(FlowLayout.LEFT, 4, 0));
    setOpaque(false);
    ButtonGroup group = new ButtonGroup();
    for (String icon : PALETTE){
      JToggleButton toggle = new JToggleButton(icon);
      toggle.setMargin(new Insets(2,6,2,6));
      toggle.addActionListener(e -> {
        value = icon;
        custom.setText("");
      });
      group.add(toggle);
      add(toggle);
    }
    custom.setColumns(4);
    custom.setToolTipText("Saisir un caractère ou emoji personnalisé");
    custom.getDocument().addDocumentListener(new DocumentListener() {
      private void sync(){ value = custom.getText(); }
      @Override public void insertUpdate(DocumentEvent e){ sync(); }
      @Override public void removeUpdate(DocumentEvent e){ sync(); }
      @Override public void changedUpdate(DocumentEvent e){ sync(); }
    });
    add(new JLabel("Perso :"));
    add(custom);
  }

  public void setValue(String v){
    value = v;
    boolean match = v!=null && PALETTE.contains(v);
    for (Component c : getComponents()){
      if (c instanceof JToggleButton btn){
        btn.setSelected(match && btn.getText().equals(v));
      }
    }
    custom.setText(!match && v!=null? v : "");
  }

  public String getValue(){
    if (value!=null && !value.isBlank()) return value;
    String txt = custom.getText();
    return txt==null? "" : txt.trim();
  }
}
