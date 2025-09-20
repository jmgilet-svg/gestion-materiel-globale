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

  /**
   * Affiche un toast avec un bouton d'action sur la droite.
   */
  public static void showWithAction(Component parent, String message, String actionLabel, Runnable action){
    if (message == null || message.isBlank()){
      return;
    }
    Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
    if (owner == null){
      return;
    }

    JDialog dialog = new JDialog(owner);
    dialog.setUndecorated(true);
    dialog.setFocusableWindowState(false);
    dialog.setType(Window.Type.POPUP);

    JPanel panel = new JPanel(new BorderLayout(12, 0));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0, 0, 0, 30)),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)
    ));
    panel.setBackground(new Color(0xE8F0FE));

    JLabel textLabel = new JLabel(message);
    textLabel.setForeground(new Color(0x0D47A1));
    textLabel.setFont(textLabel.getFont().deriveFont(Font.PLAIN, 12f));

    JButton button = new JButton(actionLabel == null || actionLabel.isBlank() ? "OK" : actionLabel);
    button.setFocusable(false);

    panel.add(textLabel, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);

    dialog.setContentPane(panel);
    dialog.pack();

    Rectangle screenBounds = owner.getGraphicsConfiguration() != null
        ? owner.getGraphicsConfiguration().getBounds()
        : owner.getBounds();
    int x = screenBounds.x + screenBounds.width - dialog.getWidth() - 16;
    int y = screenBounds.y + screenBounds.height - dialog.getHeight() - 16;
    dialog.setLocation(x, y);
    dialog.setAlwaysOnTop(true);

    final Timer hideTimer = new Timer(5000, e -> {
      dialog.setVisible(false);
      dialog.dispose();
    });
    hideTimer.setRepeats(false);

    button.addActionListener(e -> {
      hideTimer.stop();
      try {
        if (action != null){
          action.run();
        }
      } finally {
        dialog.setVisible(false);
        dialog.dispose();
      }
    });

    dialog.setVisible(true);
    hideTimer.start();
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
