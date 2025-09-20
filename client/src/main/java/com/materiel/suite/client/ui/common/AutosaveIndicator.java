package com.materiel.suite.client.ui.common;

import javax.swing.*;
import java.awt.*;

/** Indicateur compact pour l'état de l'autosave. */
public class AutosaveIndicator extends JPanel {
  private final JLabel dot = new JLabel("•");
  private final JLabel text = new JLabel("Enregistré");
  private volatile long lastSave = System.currentTimeMillis();
  private volatile boolean saving = false;
  private final Timer tick = new Timer(1000, e -> refresh());

  public AutosaveIndicator(){
    super(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    setOpaque(false);
    dot.setForeground(new Color(0x2E7D32));
    dot.setFont(dot.getFont().deriveFont(Font.BOLD));
    text.setForeground(new Color(0x2E7D32));
    add(dot);
    add(text);
    tick.setRepeats(true);
    tick.start();
  }

  public void setSaving(boolean saving){
    this.saving = saving;
    if (saving){
      dot.setForeground(new Color(0xF9A825));
      text.setForeground(new Color(0xF9A825));
      text.setText("Enregistrement…");
    } else {
      dot.setForeground(new Color(0x2E7D32));
      text.setForeground(new Color(0x2E7D32));
      text.setText("Enregistré");
    }
  }

  public void markSavedNow(){
    lastSave = System.currentTimeMillis();
    setSaving(false);
  }

  private void refresh(){
    if (saving){
      return;
    }
    long elapsed = Math.max(0, (System.currentTimeMillis() - lastSave) / 1000);
    text.setText("Enregistré il y a " + elapsed + "s");
  }
}
