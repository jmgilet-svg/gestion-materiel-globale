package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.ui.icons.IconPickerDialog;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/** Sélecteur combinant saisie libre et bibliothèque d'icônes SVG. */
public class ResourceIconPicker extends JPanel {
  private final JLabel preview = new JLabel();
  private final JTextField valueField = new JTextField(8);
  private final JButton libraryButton = new JButton("Bibliothèque…");
  private boolean updating;

  public ResourceIconPicker(){
    super(new FlowLayout(FlowLayout.LEFT, 6, 0));
    setOpaque(false);

    preview.setPreferredSize(new Dimension(28, 28));
    preview.setHorizontalAlignment(SwingConstants.CENTER);

    valueField.setColumns(8);
    valueField.setToolTipText("Emoji, caractère ou clé d'icône (ex: truck)");

    valueField.getDocument().addDocumentListener(new DocumentListener() {
      private void sync(){
        if (updating){
          return;
        }
        updatePreview();
      }

      @Override public void insertUpdate(DocumentEvent e){ sync(); }
      @Override public void removeUpdate(DocumentEvent e){ sync(); }
      @Override public void changedUpdate(DocumentEvent e){ sync(); }
    });

    libraryButton.addActionListener(e -> openLibrary());

    add(preview);
    add(valueField);
    add(libraryButton);

    updatePreview();
  }

  private void openLibrary(){
    Window owner = SwingUtilities.getWindowAncestor(this);
    IconPickerDialog dialog = new IconPickerDialog(owner);
    String pick = dialog.pick();
    if (pick != null){
      setValue(pick);
    }
  }

  private void updatePreview(){
    String value = rawValue();
    Icon icon = IconRegistry.large(value);
    if (icon != null){
      preview.setIcon(icon);
      preview.setText("");
    } else if (value != null && !value.isBlank()){
      preview.setIcon(null);
      preview.setText(value);
    } else {
      preview.setIcon(IconRegistry.placeholder(28));
      preview.setText("");
    }
  }

  private String rawValue(){
    String txt = valueField.getText();
    return txt == null ? "" : txt.trim();
  }

  public void setValue(String value){
    updating = true;
    valueField.setText(value != null ? value : "");
    updating = false;
    updatePreview();
  }

  public String getValue(){
    String value = rawValue();
    return value != null ? value : "";
  }
}
