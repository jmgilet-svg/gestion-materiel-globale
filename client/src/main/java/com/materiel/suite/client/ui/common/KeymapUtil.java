package com.materiel.suite.client.ui.common;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

/** Petits helpers pour binder des raccourcis de manière portable. */
public final class KeymapUtil {
  private KeymapUtil() {
  }

  public static int menuMask(){
    return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
  }

  public static void bind(JComponent component, String name, int keyCode, int modifiers, Runnable action){
    if (component == null || name == null || action == null){
      return;
    }
    InputMap map = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actions = component.getActionMap();
    map.put(KeyStroke.getKeyStroke(keyCode, modifiers), name);
    actions.put(name, new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        action.run();
      }
    });
  }

  public static void bindGlobal(JComponent component, String name, KeyStroke keyStroke, Runnable action){
    if (component == null || name == null || keyStroke == null || action == null){
      return;
    }
    InputMap map = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actions = component.getActionMap();
    map.put(keyStroke, name);
    actions.put(name, new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        action.run();
      }
    });
  }

  public static KeyStroke ctrlK(){
    return KeyStroke.getKeyStroke(KeyEvent.VK_K, menuMask());
  }

  public static KeyStroke ctrlG(){
    return KeyStroke.getKeyStroke(KeyEvent.VK_G, menuMask());
  }

  public static KeyStroke ctrlR(){
    return KeyStroke.getKeyStroke(KeyEvent.VK_R, menuMask());
  }

  public static KeyStroke ctrlDigit(int digit){
    int base = KeyEvent.VK_0;
    int normalized = Math.max(0, Math.min(9, digit));
    return KeyStroke.getKeyStroke(base + normalized, menuMask());
  }
}
