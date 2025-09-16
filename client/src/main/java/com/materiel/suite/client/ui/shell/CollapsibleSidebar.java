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

  public enum PinMode { AUTO, PIN_EXPANDED, PIN_COLLAPSED }

  private boolean expanded = false;
  private PinMode pinMode = PinMode.AUTO;
  private final JPanel itemsPanel = new JPanel();
  private final List<SidebarButton> buttons = new ArrayList<>();
  private final Timer collapseTimer;
  private final JToggleButton pinExpandToggle = new JToggleButton("📌");
  private final JToggleButton pinCompactToggle = new JToggleButton("📎");
  private final JLabel titleLabel = new JLabel("  Menu");
  private boolean adjustingPinToggle = false;

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
        if (pinMode == PinMode.AUTO) {
          setExpanded(true);
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (pinMode == PinMode.AUTO) {
          scheduleCollapse();
        }
      }
    };
    addMouseListener(hover);
    addMouseMotionListener(hover);
    itemsPanel.addMouseListener(hover);
    itemsPanel.addMouseMotionListener(hover);

    collapseTimer = new Timer(220, ev -> {
      if (pinMode == PinMode.AUTO && !isMouseInside()) {
        setExpanded(false);
      }
    });
    collapseTimer.setRepeats(false);
    setPinMode(PinMode.AUTO);
  }

  private void buildHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(true);
    header.setBackground(getBackground());
    header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
    header.add(titleLabel, BorderLayout.WEST);
    JPanel pinPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    pinPanel.setOpaque(false);
    pinCompactToggle.setFocusPainted(false);
    pinCompactToggle.setToolTipText("Épingler en mode compact");
    pinExpandToggle.setFocusPainted(false);
    pinExpandToggle.setToolTipText("Épingler en mode élargi");
    pinCompactToggle.addActionListener(e -> onCompactPinToggled());
    pinExpandToggle.addActionListener(e -> onExpandPinToggled());
    pinPanel.add(pinCompactToggle);
    pinPanel.add(pinExpandToggle);
    header.add(pinPanel, BorderLayout.EAST);
    add(header, BorderLayout.NORTH);
  }

  private boolean isMouseInside() {
    Point p = getMousePosition();
    return p != null && p.x >= 0 && p.x < getWidth() && p.y >= 0 && p.y < getHeight();
  }

  private void scheduleCollapse() {
    if (pinMode != PinMode.AUTO) {
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
        if (pinMode == PinMode.AUTO) {
          setExpanded(true);
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (pinMode == PinMode.AUTO) {
          scheduleCollapse();
        }
      }
    };
    button.addMouseListener(hover);
    button.addMouseMotionListener(hover);
    return button;
  }

  private void onExpandPinToggled(){
    if (adjustingPinToggle) {
      return;
    }
    if (pinExpandToggle.isSelected()) {
      pinCompactToggle.setSelected(false);
      setPinMode(PinMode.PIN_EXPANDED);
    } else {
      setPinMode(pinCompactToggle.isSelected() ? PinMode.PIN_COLLAPSED : PinMode.AUTO);
    }
  }

  private void onCompactPinToggled(){
    if (adjustingPinToggle) {
      return;
    }
    if (pinCompactToggle.isSelected()) {
      pinExpandToggle.setSelected(false);
      setPinMode(PinMode.PIN_COLLAPSED);
    } else {
      setPinMode(pinExpandToggle.isSelected() ? PinMode.PIN_EXPANDED : PinMode.AUTO);
    }
  }

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  public String getTitle() {
    return titleLabel.getText();
  }

  public void setPinMode(PinMode mode){
    if (mode == null){
      mode = PinMode.AUTO;
    }
    if (this.pinMode != mode) {
      this.pinMode = mode;
      switch (mode) {
        case PIN_EXPANDED -> {
          collapseTimer.stop();
          setExpanded(true);
        }
        case PIN_COLLAPSED -> {
          collapseTimer.stop();
          setExpanded(false);
        }
        case AUTO -> {
          if (!isMouseInside()) {
            setExpanded(false);
          }
        }
      }
    } else if (mode == PinMode.AUTO && !isMouseInside()) {
      setExpanded(false);
    }
    updatePinButtons();
  }

  public PinMode getPinMode(){
    return pinMode;
  }

  public void setPinned(boolean pinned) {
    setPinMode(pinned ? PinMode.PIN_EXPANDED : PinMode.AUTO);
  }

  public boolean isPinned() {
    return pinMode == PinMode.PIN_EXPANDED;
  }

  private void updatePinButtons(){
    adjustingPinToggle = true;
    try {
      pinExpandToggle.setSelected(pinMode == PinMode.PIN_EXPANDED);
      pinCompactToggle.setSelected(pinMode == PinMode.PIN_COLLAPSED);
    } finally {
      adjustingPinToggle = false;
    }
  }
}
