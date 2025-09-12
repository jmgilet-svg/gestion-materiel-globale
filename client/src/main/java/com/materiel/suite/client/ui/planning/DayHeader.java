package com.materiel.suite.client.ui.planning;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DayHeader extends JComponent {
  private final PlanningBoard board;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("EEE dd/MM");
  public DayHeader(PlanningBoard b){ this.board=b; setPreferredSize(new Dimension(600,28)); }
  @Override public Dimension getPreferredSize(){
    return new Dimension(board.getPreferredSize().width, 28);
  }
  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(new Color(0xF5F5F5));
    g2.fillRect(0,0,getWidth(),getHeight());
    g2.setColor(new Color(0xDDDDDD));
    g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
    int x=0, w = board.getColWidth();
    LocalDate d = board.getStartDate();
    for (int i=0;i<getWidth()/w+1;i++){
      g2.setColor(new Color(0xDDDDDD));
      g2.drawLine(x,0,x,getHeight());
      g2.setColor(Color.DARK_GRAY);
      g2.drawString(DF.format(d.plusDays(i)), x+8, getHeight()/2+5);
      g2.setColor(new Color(0xCCCCCC));
      int sub = w/4;
      g2.drawLine(x+sub, getHeight()-8, x+sub, getHeight());
      g2.drawLine(x+2*sub, getHeight()-8, x+2*sub, getHeight());
      g2.drawLine(x+3*sub, getHeight()-8, x+3*sub, getHeight());
      x+=w;
    }
  }
}
