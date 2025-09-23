package com.materiel.suite.client.ui.shell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Bouton de navigation de la barre latérale avec gestion compact/étendu. */
public class SidebarButton extends JButton {
  private static final int ITEM_HEIGHT = 36;
  private static final int ITEM_HEIGHT_COMPACT = 32;
  private static final int RADIUS = 10;

  private final Runnable action;
  private final String fullLabel;
  private boolean expanded;
  private boolean active;
  private boolean hover;

  public SidebarButton(String iconKey, Icon iconSvg, String label, Runnable action) {
    super(label, iconSvg);
    this.action = action;
    this.fullLabel = label;

    setHorizontalAlignment(SwingConstants.LEFT);
    setIconTextGap(10);
    setFocusPainted(false);
    setFocusable(false);
    setRolloverEnabled(false);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setOpaque(false);
    setMargin(new Insets(0, 10, 0, 10));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setToolTipText(label);
    setAlignmentX(Component.CENTER_ALIGNMENT);
    if (iconKey != null && !iconKey.isBlank()) {
      putClientProperty("sidebar.iconKey", iconKey);
    }

    Font baseFont = getFont();
    if (baseFont != null) {
      setFont(baseFont.deriveFont(Font.PLAIN, 13f));
    }

    addActionListener(e -> {
      if (this.action != null) {
        this.action.run();
      }
    });

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        hover = true;
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hover = false;
        repaint();
      }
    });

    setExpanded(false);
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
    super.setText(expanded ? fullLabel : "");
    setHorizontalAlignment(expanded ? SwingConstants.LEFT : SwingConstants.CENTER);
    revalidate();
    repaint();
  }

  public void setActive(boolean active) {
    this.active = active;
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    int minHeight = expanded ? ITEM_HEIGHT : ITEM_HEIGHT_COMPACT;
    d.height = Math.max(minHeight, d.height);
    int targetWidth = expanded ? CollapsibleSidebar.EXPANDED_WIDTH : CollapsibleSidebar.COLLAPSED_WIDTH;
    d.width = Math.max(targetWidth, d.width);
    return d;
  }

  @Override
  public Dimension getMaximumSize() {
    Dimension pref = getPreferredSize();
    return new Dimension(Integer.MAX_VALUE, pref.height);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    if (active) {
      g2.setColor(new Color(0xE8F0FF));
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
    } else if (hover) {
      g2.setColor(new Color(0xF2F4F7));
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
    }
    g2.dispose();
    super.paintComponent(g);
  }
}
