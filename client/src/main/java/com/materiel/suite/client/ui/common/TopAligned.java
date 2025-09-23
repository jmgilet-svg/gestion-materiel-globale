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

  /**
   * Force l'alignement haut d'un formulaire déjà inséré dans un conteneur.
   *
   * <p>Si le panneau est déjà placé dans une {@link JScrollPane}, aucune action n'est effectuée.
   * Dans le cas contraire, la zone {@link BorderLayout#CENTER} est remplacée par la variante
   * scrollable produite par {@link #wrap(JComponent)}.</p>
   *
   * @param root conteneur parent, supposé utiliser un {@link BorderLayout}
   * @param formPanel panneau de formulaire à réaligner
   */
  public static void enforceOn(Container root, JComponent formPanel){
    if (root == null){
      throw new IllegalArgumentException("root cannot be null");
    }
    if (formPanel == null){
      throw new IllegalArgumentException("formPanel cannot be null");
    }
    if (SwingUtilities.getAncestorOfClass(JScrollPane.class, formPanel) != null){
      return;
    }

    JComponent wrapped = wrap(formPanel);
    if (root.getLayout() instanceof BorderLayout layout){
      for (Component component : root.getComponents()){
        Object constraint = layout.getConstraints(component);
        if (constraint == null || BorderLayout.CENTER.equals(constraint)){
          root.remove(component);
          break;
        }
      }
      root.add(wrapped, BorderLayout.CENTER);
      root.revalidate();
      root.repaint();
    }
  }
}
