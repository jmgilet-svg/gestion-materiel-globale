package com.materiel.suite.client.ui.planning;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DayHeader extends JComponent {
  private final PlanningBoard board;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("EEE dd MMM");
  public DayHeader(PlanningBoard b){
    this.board=b; setPreferredSize(new Dimension(600,34)); setFont(PlanningUx.uiFont(this));
  }
  @Override public Dimension getPreferredSize(){
    return new Dimension(board.getPreferredSize().width, 34);
  }
  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(PlanningUx.HEADER_BG);
    g2.fillRect(0,0,getWidth(),getHeight());
    g2.setColor(PlanningUx.GRID);
    g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
    int x=0, w = board.getColWidth();
    LocalDate d = board.getStartDate();
    for (int i=0;i<getWidth()/w+1;i++){
      g2.setColor(PlanningUx.GRID);
      g2.drawLine(x,0,x,getHeight());
      g2.setColor(PlanningUx.HEADER_TX);
      String txt = DF.format(d.plusDays(i));
      FontMetrics fm = g2.getFontMetrics();
      int tx = x + Math.max(8, (w - fm.stringWidth(txt))/2);
      int ty = (getHeight() + fm.getAscent())/2 - 2;
      g2.drawString(txt, tx, ty);
      x+=w;
    }
  }
}
