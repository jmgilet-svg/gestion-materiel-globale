package com.materiel.suite.client.ui.shell;

import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Élément cliquable de la barre latérale, rend icône + texte quand expanded=true. */
public class SidebarButton extends JPanel {
  private static final Color BASE_COLOR = new Color(245, 245, 245);
  private static final Color HOVER_COLOR = new Color(234, 242, 255);
  private static final Color PRESSED_COLOR = new Color(210, 230, 255);
  private static final Color ACTIVE_COLOR = new Color(212, 232, 255);
  private final JLabel icon = new JLabel();
  private final Component spacer = Box.createHorizontalStrut(10);
  private final JLabel text = new JLabel();
  private final Runnable action;
  private final String iconKey;
  private boolean hovered = false;
  private boolean pressed = false;
  private boolean active = false;

  public SidebarButton(String iconKey, Icon iconSvg, String label, Runnable action) {
    super(new BorderLayout());
    this.action = action;
    this.iconKey = iconKey;
    setOpaque(true);
    setBackground(BASE_COLOR);
    setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    icon.setHorizontalAlignment(SwingConstants.CENTER);
    icon.setPreferredSize(new Dimension(28, 24));
    icon.setIcon(iconSvg);

    text.setText(label);
    text.setFont(text.getFont().deriveFont(Font.PLAIN, 12f));

    JPanel line = new JPanel();
    line.setOpaque(false);
    line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
    line.add(icon);
    line.add(spacer);
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
        boolean inside = contains(e.getPoint());
        if (inside && action != null) {
          action.run();
        }
        pressed = false;
        refreshBackground();
        if (inside) {
          Window w = SwingUtilities.getWindowAncestor(SidebarButton.this);
          Component anchor = w != null ? w : SidebarButton.this;
          String key = iconKey != null && !iconKey.isBlank() ? iconKey : "info";
          Toasts.show(anchor, "Ouverture : " + text.getText(), key);
        }
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
    spacer.setVisible(expanded);
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
