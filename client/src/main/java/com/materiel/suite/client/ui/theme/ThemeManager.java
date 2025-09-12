package com.materiel.suite.client.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * Charge FlatLaf si disponible, sinon laisse le LAF système.
 */
public final class ThemeManager {
  private static boolean dark = false;
  private ThemeManager(){}

  public static void applyInitial(){
    try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore){}
    tryLoad(false);
  }

  public static void toggleDark(){
    dark = !dark;
    tryLoad(dark);
    for (Frame f : Frame.getFrames())
      SwingUtilities.updateComponentTreeUI(f);
  }

  private static void tryLoad(boolean darkMode){
    try {
      String cls = darkMode ? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf";
      Class<?> laf = Class.forName(cls);
      laf.getMethod("setup").invoke(null);
    } catch(Throwable ignore){}
  }
}

