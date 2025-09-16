package com.materiel.suite.client.ui.shell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Barre latérale rétractable : au repos = icônes seules ; en survol = icône + libellé.
 */
public class CollapsibleSidebar extends JPanel {
  public static final int COLLAPSED_WIDTH = 56;
  public static final int EXPANDED_WIDTH = 220;

  private boolean expanded = false;
  private final JPanel itemsPanel = new JPanel();
  private final List<SidebarButton> buttons = new ArrayList<>();
  private final Timer collapseTimer;

  public CollapsibleSidebar() {
    super(new BorderLayout());
    setBackground(new Color(245, 245, 245));
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

    itemsPanel.setOpaque(false);
    itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
    itemsPanel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

    JScrollPane scroll = new JScrollPane(itemsPanel);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(12);
    add(scroll, BorderLayout.CENTER);

    setPreferredSize(new Dimension(COLLAPSED_WIDTH, 0));

    MouseAdapter hover = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        setExpanded(true);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        scheduleCollapse();
      }
    };
    addMouseListener(hover);
    addMouseMotionListener(hover);
    itemsPanel.addMouseListener(hover);
    itemsPanel.addMouseMotionListener(hover);

    collapseTimer = new Timer(220, ev -> {
      if (!isMouseInside()) {
        setExpanded(false);
      }
    });
    collapseTimer.setRepeats(false);
  }

  private boolean isMouseInside() {
    Point p = getMousePosition();
    return p != null && p.x >= 0 && p.x < getWidth() && p.y >= 0 && p.y < getHeight();
  }

  private void scheduleCollapse() {
    collapseTimer.restart();
  }

  public void setExpanded(boolean expanded) {
    if (this.expanded == expanded) {
      return;
    }
    this.expanded = expanded;
    for (SidebarButton b : buttons) {
      b.setExpanded(expanded);
    }
    setPreferredSize(new Dimension(expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH, getHeight()));
    revalidate();
    repaint();
  }

  /** Ajoute un bouton avec une icône (emoji/char) et un libellé. */
  public SidebarButton addItem(String iconText, String label, Runnable action) {
    SidebarButton button = new SidebarButton(iconText, label, action);
    buttons.add(button);
    itemsPanel.add(button);
    itemsPanel.add(Box.createVerticalStrut(4));
    MouseAdapter hover = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        setExpanded(true);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        scheduleCollapse();
      }
    };
    button.addMouseListener(hover);
    button.addMouseMotionListener(hover);
    return button;
  }
}
