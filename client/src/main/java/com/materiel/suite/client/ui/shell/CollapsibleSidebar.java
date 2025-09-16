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
  private boolean pinned = false;
  private final JPanel itemsPanel = new JPanel();
  private final List<SidebarButton> buttons = new ArrayList<>();
  private final Timer collapseTimer;
  private final JToggleButton pinToggle = new JToggleButton("📌");
  private final JLabel titleLabel = new JLabel("  Menu");

  public CollapsibleSidebar() {
    super(new BorderLayout());
    setBackground(new Color(245, 245, 245));
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

    buildHeader();

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
        if (!pinned) {
          setExpanded(true);
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (!pinned) {
          scheduleCollapse();
        }
      }
    };
    addMouseListener(hover);
    addMouseMotionListener(hover);
    itemsPanel.addMouseListener(hover);
    itemsPanel.addMouseMotionListener(hover);

    collapseTimer = new Timer(220, ev -> {
      if (!pinned && !isMouseInside()) {
        setExpanded(false);
      }
    });
    collapseTimer.setRepeats(false);
  }

  private void buildHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(true);
    header.setBackground(getBackground());
    header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
    header.add(titleLabel, BorderLayout.WEST);
    pinToggle.setFocusPainted(false);
    pinToggle.setToolTipText("Épingler la barre");
    pinToggle.addActionListener(e -> setPinned(pinToggle.isSelected()));
    header.add(pinToggle, BorderLayout.EAST);
    add(header, BorderLayout.NORTH);
  }

  private boolean isMouseInside() {
    Point p = getMousePosition();
    return p != null && p.x >= 0 && p.x < getWidth() && p.y >= 0 && p.y < getHeight();
  }

  private void scheduleCollapse() {
    if (pinned) {
      return;
    }
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
        if (!pinned) {
          setExpanded(true);
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (!pinned) {
          scheduleCollapse();
        }
      }
    };
    button.addMouseListener(hover);
    button.addMouseMotionListener(hover);
    return button;
  }

  public void setPinned(boolean pinned) {
    if (this.pinned == pinned) {
      return;
    }
    this.pinned = pinned;
    pinToggle.setSelected(pinned);
    if (pinned) {
      collapseTimer.stop();
      setExpanded(true);
    } else if (!isMouseInside()) {
      setExpanded(false);
    }
  }

  public boolean isPinned() {
    return pinned;
  }

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  public String getTitle() {
    return titleLabel.getText();
  }
}
