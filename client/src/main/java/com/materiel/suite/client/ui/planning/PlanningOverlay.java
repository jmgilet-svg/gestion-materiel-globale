package com.materiel.suite.client.ui.planning;

import javax.swing.*;
import java.awt.*;

/**
 * Lightweight wrapper that installs a transparent glass pane above the planning content so
 * interactive layers can be added without modifying the underlying component.
 */
public final class PlanningOverlay {
  private PlanningOverlay(){
  }

  /** Marker interface used to detect an already wrapped planning tab. */
  public interface ContainerMarker {
  }

  /** Wraps the given planning component inside a layered container prepared for overlays. */
  public static JComponent wrap(Component planningContent) {
    JComponent content = ensureJComponent(planningContent);
    detachFromParent(content);

    Wrapper wrapper = new Wrapper(content);
    wrapper.putClientProperty(ContainerMarker.class.getName(), Boolean.TRUE);
    return wrapper;
  }

  private static void detachFromParent(JComponent component) {
    Container parent = component.getParent();
    if (parent != null) {
      parent.remove(component);
    }
  }

  private static JComponent ensureJComponent(Component component) {
    if (component instanceof JComponent jc) {
      return jc;
    }
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
    return panel;
  }

  private static final class Wrapper extends JPanel implements ContainerMarker {
    private final LayeredPane layered;

    Wrapper(JComponent content) {
      super(new BorderLayout());
      content.setAlignmentX(0f);
      content.setAlignmentY(0f);
      layered = new LayeredPane(content);
      add(layered, BorderLayout.CENTER);
    }
  }

  private static final class LayeredPane extends JLayeredPane {
    private final JComponent content;
    private final GlassLayer overlay;

    LayeredPane(JComponent content) {
      this.content = content;
      this.overlay = new GlassLayer();
      setLayout(null);
      add(content, DEFAULT_LAYER);
      add(overlay, PALETTE_LAYER);
    }

    @Override
    public void doLayout() {
      Dimension size = getSize();
      content.setBounds(0, 0, size.width, size.height);
      overlay.setBounds(0, 0, size.width, size.height);
    }

    @Override
    public Dimension getPreferredSize() {
      return content.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
      return content.getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
      return content.getMaximumSize();
    }
  }

  /** Transparent layer placed above the planning content. */
  private static final class GlassLayer extends JComponent {
    GlassLayer() {
      setOpaque(false);
      setFocusable(false);
      putClientProperty("planning.overlay", Boolean.TRUE);
    }

    @Override
    public boolean contains(int x, int y) {
      // Let the underlying planning component handle the mouse events.
      return false;
    }
  }
}
