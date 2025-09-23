package com.materiel.suite.client.ui.common;

import com.materiel.suite.client.ui.theme.ThemeManager;
import com.materiel.suite.client.ui.theme.UiTokens;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Petite pilule de contexte (ex. « Semaine 2025-W38 »). */
public class Pill extends JLabel {
  private Color backgroundColor;

  public Pill(String text){
    super(text);
    setOpaque(false);
    setBorder(new EmptyBorder(2, 10, 2, 10));
    Font base = getFont();
    if (base != null){
      setFont(base.deriveFont(Font.PLAIN, 12f));
    }
    Color accent = UiTokens.brandPrimary();
    backgroundColor = ThemeManager.lighten(accent, 0.82f);
    if (backgroundColor == null){
      backgroundColor = new Color(0xE8F0FF);
    }
    setForeground(accent != null ? accent.darker() : new Color(0x1E3A8A));
  }

  public void setBackgroundColor(Color color){
    backgroundColor = color;
    repaint();
  }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(backgroundColor != null ? backgroundColor : UiTokens.bgSoft());
    int arc = Math.min(getHeight(), 18);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    g2.dispose();
    super.paintComponent(g);
  }
}
