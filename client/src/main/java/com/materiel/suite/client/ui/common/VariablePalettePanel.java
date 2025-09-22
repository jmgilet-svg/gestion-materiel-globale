package com.materiel.suite.client.ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Palette simple affichant la liste des variables disponibles pour les
 * templates et permettant de les insérer via un callback.
 */
public class VariablePalettePanel extends JPanel {
  public interface Inserter {
    void insert(String text);
  }

  private final DefaultListModel<String> model = new DefaultListModel<>();
  private final JList<String> list = new JList<>(model);
  private Inserter inserter;

  public VariablePalettePanel(String title){
    super(new BorderLayout());
    JLabel label = new JLabel(title == null || title.isBlank() ? "Variables" : title);
    label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    add(label, BorderLayout.NORTH);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setVisibleRowCount(12);
    list.addMouseListener(new MouseAdapter(){
      @Override
      public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          String value = list.getSelectedValue();
          if (value != null && inserter != null){
            inserter.insert("{{" + value + "}}");
          }
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane(list);
    add(scrollPane, BorderLayout.CENTER);
  }

  public void setVariables(List<String> vars){
    model.clear();
    if (vars == null){
      return;
    }
    for (String value : vars){
      if (value != null && !value.isBlank()){
        model.addElement(value);
      }
    }
  }

  public void setInserter(Inserter inserter){
    this.inserter = inserter;
  }

  @Override
  public void setEnabled(boolean enabled){
    super.setEnabled(enabled);
    list.setEnabled(enabled);
  }
}
