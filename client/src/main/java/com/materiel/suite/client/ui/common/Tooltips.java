package com.materiel.suite.client.ui.common;

import javax.swing.JComponent;

/** Utilitaires pour formater les infobulles avec rappel de raccourcis. */
public final class Tooltips {
  private Tooltips(){
  }

  public static void setWithShortcut(JComponent component, String label, String shortcut){
    if (component == null){
      return;
    }
    String safeLabel = label != null ? escape(label) : "";
    String extra = shortcut != null && !shortcut.isBlank() ? " — <b>" + escape(shortcut) + "</b>" : "";
    component.setToolTipText("<html>" + safeLabel + extra + "</html>");
  }

  private static String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
