package com.materiel.suite.client.ui.planning;

import javax.swing.Icon;
import javax.swing.border.AbstractBorder;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/** Dessine une petite icône en surcouche dans le coin supérieur droit d'un composant. */
final class BadgeBorder extends AbstractBorder {
  private final Icon icon;
  private final int margin;

  BadgeBorder(Icon icon){
    this(icon, 4);
  }

  BadgeBorder(Icon icon, int margin){
    this.icon = icon;
    this.margin = Math.max(0, margin);
  }

  @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height){
    if (icon == null){
      return;
    }
    int ix = x + width - icon.getIconWidth() - margin;
    int iy = y + margin;
    icon.paintIcon(c, g, ix, iy);
  }

  @Override public Insets getBorderInsets(Component c){
    return new Insets(0, 0, 0, 0);
  }

  @Override public Insets getBorderInsets(Component c, Insets insets){
    if (insets == null){
      return new Insets(0, 0, 0, 0);
    }
    insets.top = 0;
    insets.left = 0;
    insets.bottom = 0;
    insets.right = 0;
    return insets;
  }
}
