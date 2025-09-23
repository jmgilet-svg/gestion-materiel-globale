package com.materiel.suite.client.ui.common;

import javax.swing.*;
import java.awt.*;

/**
 * Utilitaire pour encapsuler un formulaire dans une scrollpane alignée en haut.
 */
public final class TopAligned {
  private TopAligned(){}

  /**
   * Retourne une {@link JScrollPane} dont le contenu reste aligné en haut du viewport.
   *
   * @param formPanel panneau de formulaire à encapsuler
   * @return un composant scrollable, prêt à être inséré dans un onglet
   */
  public static JComponent wrap(JComponent formPanel){
    if (formPanel == null){
      throw new IllegalArgumentException("formPanel cannot be null");
    }
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(true);
    wrapper.setBackground(formPanel.getBackground());
    wrapper.add(formPanel, BorderLayout.NORTH);

    JScrollPane scrollPane = new JScrollPane(wrapper,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(18);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setViewportBorder(null);
    scrollPane.getViewport().setBackground(formPanel.getBackground());
    return scrollPane;
  }
}
