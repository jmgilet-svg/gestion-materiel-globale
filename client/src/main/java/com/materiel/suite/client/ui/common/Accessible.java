package com.materiel.suite.client.ui.common;

import javax.swing.*;

/** Utilitaires pour définir nom et description accessibles (lecteurs d'écran/tests UI). */
public final class Accessible {
  private Accessible(){}

  public static <T extends JComponent> T name(T component, String name){
    if (component != null && name != null){
      component.getAccessibleContext().setAccessibleName(name);
    }
    return component;
  }

  public static <T extends JComponent> T describe(T component, String description){
    if (component != null && description != null){
      component.getAccessibleContext().setAccessibleDescription(description);
    }
    return component;
  }

  public static <T extends JComponent> T a11y(T component, String name, String description){
    name(component, name);
    describe(component, description);
    return component;
  }
}
