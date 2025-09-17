package com.materiel.suite.client.ui.common;

import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * Utilitaire minimaliste pour afficher des toasts légers dans l'interface Swing.
 */
public final class Toasts {
  private Toasts(){}

  public static void info(Component parent, String message){
    show(parent, message, "info");
  }

  public static void success(Component parent, String message){
    show(parent, message, "success");
  }

  public static void error(Component parent, String message){
    show(parent, message, "error");
  }

  public static void show(Component parent, String message, String iconKey){
    if (message == null || message.isBlank()){
      return;
    }
    Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
    if (owner == null){
      return;
    }

    JWindow toast = new JWindow(owner);
    toast.setFocusableWindowState(false);
    toast.setType(Window.Type.POPUP);

    JPanel panel = new JPanel(new BorderLayout(8, 0));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220)),
        BorderFactory.createEmptyBorder(8, 10, 8, 12)
    ));
    panel.setBackground(new Color(255, 255, 255, 240));

    Icon icon = IconRegistry.medium(iconKey);
    if (icon == null){
      icon = IconRegistry.placeholder(20);
    }
    JLabel iconLabel = new JLabel(icon);
    JLabel textLabel = new JLabel(message);
    textLabel.setFont(textLabel.getFont().deriveFont(Font.PLAIN, 12f));

    panel.add(iconLabel, BorderLayout.WEST);
    panel.add(textLabel, BorderLayout.CENTER);

    toast.setContentPane(panel);
    toast.pack();

    Rectangle screenBounds = owner.getGraphicsConfiguration() != null
        ? owner.getGraphicsConfiguration().getBounds()
        : owner.getBounds();
    int x = screenBounds.x + screenBounds.width - toast.getWidth() - 16;
    int y = screenBounds.y + screenBounds.height - toast.getHeight() - 16;
    toast.setLocation(x, y);
    toast.setAlwaysOnTop(true);

    boolean supportsOpacity = true;
    try {
      toast.setOpacity(1f);
    } catch (UnsupportedOperationException | IllegalComponentStateException ex){
      supportsOpacity = false;
    }

    toast.setVisible(true);

    if (!supportsOpacity){
      Timer hide = new Timer(2000, e -> {
        toast.setVisible(false);
        toast.dispose();
      });
      hide.setRepeats(false);
      hide.start();
      return;
    }

    Timer fade = new Timer(18, e -> {
      float alpha = toast.getOpacity();
      if (alpha <= 0.05f){
        ((Timer) e.getSource()).stop();
        toast.setVisible(false);
        toast.dispose();
      } else {
        toast.setOpacity(alpha - 0.06f);
      }
    });
    fade.setInitialDelay(1600);
    fade.start();
  }
}
