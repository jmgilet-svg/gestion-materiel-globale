package com.materiel.suite.client.ui.planning.agenda;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Simple day agenda view placeholder used for navigation tests.
 * It currently renders an empty component but exposes basic
 * setters to control the displayed day and hours range.
 */
public class AgendaView extends JComponent {
  private LocalDate day = LocalDate.now();
  private int dayStart = 7 * 60;  // minutes
  private int dayEnd = 18 * 60;   // minutes

  public AgendaView(){
    setPreferredSize(new Dimension(800, 600));
  }

  public void setDay(LocalDate d){
    day = d;
    repaint();
  }
  public LocalDate getDay(){
    return day;
  }
  public void setDayStart(int m){
    dayStart = m;
    repaint();
  }
  public void setDayEnd(int m){
    dayEnd = m;
    repaint();
  }

  @Override protected void paintComponent(Graphics g){
    super.paintComponent(g);
    // simple background placeholder
    g.setColor(new Color(0xF4F4F4));
    g.fillRect(0,0,getWidth(),getHeight());
  }
}

