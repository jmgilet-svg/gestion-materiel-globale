package com.materiel.suite.client.ui.common;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/** En-tête de section avec titre + zone d'actions (droite). */
public class SectionPanel extends JPanel {
  private final JLabel title = new JLabel();
  private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

  public SectionPanel(String text){
    super(new BorderLayout());
    setBorder(new EmptyBorder(8, 8, 8, 8));
    title.setText(text);
    Font base = title.getFont();
    if (base != null){
      title.setFont(base.deriveFont(Font.BOLD, 14f));
    }
    add(title, BorderLayout.WEST);
    actions.setOpaque(false);
    add(actions, BorderLayout.EAST);
  }

  public JPanel actions(){
    return actions;
  }

  public void setTitle(String text){
    title.setText(text);
  }
}
