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

/** Petit badge (fond doux + coins arrondis) pour comptes/états. */
public class Badge extends JLabel {
  public enum Tone { DEFAULT, INFO, OK, WARN, ERR }

  private Tone tone = Tone.DEFAULT;

  public Badge(String text){
    this(text, Tone.DEFAULT);
  }

  public Badge(String text, Tone tone){
    super(text);
    this.tone = tone != null ? tone : Tone.DEFAULT;
    setOpaque(false);
    setBorder(new EmptyBorder(2, 8, 2, 8));
    Font base = getFont();
    if (base != null){
      setFont(base.deriveFont(Font.PLAIN, 11f));
    }
    setForeground(UiTokens.textPrimary());
  }

  public void setTone(Tone tone){
    this.tone = tone != null ? tone : Tone.DEFAULT;
    repaint();
  }

  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g.create();
    Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(backgroundColor());
    int arc = Math.min(getHeight(), 18);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    g2.dispose();
    super.paintComponent(g);
  }

  private Color backgroundColor(){
    Color base = switch (tone){
      case INFO -> UiTokens.info();
      case OK -> UiTokens.ok();
      case WARN -> UiTokens.warn();
      case ERR -> UiTokens.err();
      default -> UiTokens.brandPrimary();
    };
    Color tinted = ThemeManager.lighten(base, 0.85f);
    return tinted != null ? tinted : new Color(0xEEF3FE);
  }
}
