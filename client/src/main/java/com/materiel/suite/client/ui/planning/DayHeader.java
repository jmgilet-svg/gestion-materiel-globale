package com.materiel.suite.client.ui.planning;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DayHeader extends JComponent {
  private final PlanningBoard board;
  private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("EEE dd/MM");
  public DayHeader(PlanningBoard b){
    this.board=b; setPreferredSize(new Dimension(600,32)); setFont(PlanningUx.uiFont(this));
  }
  @Override public Dimension getPreferredSize(){
    return new Dimension(board.getPreferredSize().width, 32);
  }
  @Override protected void paintComponent(Graphics g){
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(PlanningUx.HEADER_BG);
    g2.fillRect(0,0,getWidth(),getHeight());
    g2.setColor(PlanningUx.GRID);
    g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
    int x=0, dayW = board.getDayPixelWidth();
    LocalDate d = board.getStartDate();
    for (int i=0;i<getWidth()/dayW+1;i++){
      g2.setColor(PlanningUx.GRID);
      g2.drawLine(x,0,x,getHeight());
      g2.setColor(PlanningUx.HEADER_TX);
      g2.drawString(DF.format(d.plusDays(i)), x+8, 14);
      g2.setFont(getFont().deriveFont(11f));
      g2.setColor(new Color(0x4B5563));
      int sph = board.getSlotsPerDay()/24; // slots per hour
      for (int h=0; h<24; h+=2){
        int px = x + h*sph*board.getSlotWidth();
        String label = (h<10? "0":"")+h+":00";
        g2.drawString(label, px+4, getHeight()-12);
      }
      x+=dayW;
    }
  }
}
