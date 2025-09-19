package com.materiel.suite.client.ui.interventions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

/** Barre d'étapes interactive pour le suivi Intervention → Devis → Facturation. */
public class StepBar extends JPanel {
  private final JLabel[] steps = new JLabel[3];
  private IntConsumer onNavigate;

  public StepBar(){
    super(new GridLayout(1, 3, 8, 0));
    setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    String[] labels = {"Intervention", "Devis", "Facturation"};
    for (int i = 0; i < steps.length; i++){
      JLabel label = new JLabel(labels[i], SwingConstants.CENTER);
      label.setOpaque(true);
      label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
      label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      final int index = i;
      label.addMouseListener(new MouseAdapter(){
        @Override public void mouseClicked(MouseEvent e){
          if (onNavigate != null){
            onNavigate.accept(index);
          }
        }
      });
      steps[i] = label;
      add(label);
    }
    setState(0, false, false, false);
  }

  public void setOnNavigate(IntConsumer onNavigate){
    this.onNavigate = onNavigate;
  }

  public void setState(int active, boolean interventionReady, boolean quoteGenerated, boolean billingReady){
    int clamped = Math.max(0, Math.min(active, steps.length - 1));
    boolean[] done = {interventionReady, quoteGenerated, billingReady};
    for (int i = 0; i < steps.length; i++){
      JLabel label = steps[i];
      boolean isActive = i == clamped;
      boolean isDone = done[i];
      label.setBackground(isActive ? new Color(0xDCEAFB) : new Color(0xF5F7FA));
      label.setForeground(isDone ? new Color(0x1B5E20) : (isActive ? new Color(0x0D47A1) : new Color(0x455A64)));
      label.setText(stepLabel(i, isDone, isActive));
    }
    repaint();
  }

  private String stepLabel(int index, boolean done, boolean active){
    String base = switch (index){
      case 0 -> "Intervention";
      case 1 -> "Devis";
      default -> "Facturation";
    };
    if (done){
      return base + " ✅";
    }
    if (active){
      return base + " ⏳";
    }
    return base;
  }
}
