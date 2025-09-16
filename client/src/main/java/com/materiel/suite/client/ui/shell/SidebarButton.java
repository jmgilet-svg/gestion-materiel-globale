package com.materiel.suite.client.ui.shell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Élément cliquable de la barre latérale, rend icône + texte quand expanded=true. */
public class SidebarButton extends JPanel {
  private static final Color BASE_COLOR = new Color(245, 245, 245);
  private static final Color HOVER_COLOR = new Color(230, 240, 255);
  private static final Color PRESSED_COLOR = new Color(210, 230, 255);
  private static final Color ACTIVE_COLOR = new Color(212, 232, 255);
  private final JLabel icon = new JLabel();
  private final JLabel text = new JLabel();
  private final Runnable action;
  private boolean hovered = false;
  private boolean pressed = false;
  private boolean active = false;

  public SidebarButton(String iconText, String label, Runnable action) {
    super(new BorderLayout());
    this.action = action;
    setOpaque(true);
    setBackground(BASE_COLOR);
    setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    icon.setText(iconText);
    icon.setHorizontalAlignment(SwingConstants.CENTER);
    icon.setFont(icon.getFont().deriveFont(16f));
    icon.setPreferredSize(new Dimension(28, 24));

    text.setText(label);
    text.setFont(text.getFont().deriveFont(Font.PLAIN, 13f));

    JPanel line = new JPanel();
    line.setOpaque(false);
    line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
    line.add(icon);
    line.add(Box.createHorizontalStrut(10));
    line.add(text);
    line.add(Box.createHorizontalGlue());
    add(line, BorderLayout.CENTER);

    setExpanded(false);

    MouseAdapter ma = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        hovered = true;
        refreshBackground();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hovered = false;
        refreshBackground();
      }

      @Override
      public void mousePressed(MouseEvent e) {
        pressed = true;
        refreshBackground();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (pressed && contains(e.getPoint())) {
          if (action != null) {
            action.run();
          }
        }
        pressed = false;
        refreshBackground();
      }
    };
    addMouseListener(ma);
    addMouseMotionListener(ma);
    icon.addMouseListener(ma);
    icon.addMouseMotionListener(ma);
    text.addMouseListener(ma);
    text.addMouseMotionListener(ma);
  }

  public void setExpanded(boolean expanded) {
    text.setVisible(expanded);
    revalidate();
    repaint();
  }

  public void setActive(boolean active) {
    this.active = active;
    refreshBackground();
  }

  private void refreshBackground() {
    if (pressed) {
      setBackground(PRESSED_COLOR);
    } else if (hovered) {
      setBackground(active ? PRESSED_COLOR : HOVER_COLOR);
    } else if (active) {
      setBackground(ACTIVE_COLOR);
    } else {
      setBackground(BASE_COLOR);
    }
  }
}
