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
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setColor(PlanningUx.HEADER_BG);
    g2.fillRect(0,0,getWidth(),getHeight());
    g2.setColor(PlanningUx.GRID);
    g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
    int x=0, dayW = board.getDayPixelWidth();
    LocalDate d = board.getStartDate();
    for (int i=0;i<getWidth()/dayW+1;i++){
      final int slotW = board.getSlotWidth();
      g2.setColor(PlanningUx.GRID);
      g2.drawLine(x,0,x,getHeight());
      // Libellé du jour
      g2.setColor(new Color(0x111827));
      g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
      g2.drawString(DF.format(d.plusDays(i)), x+8, 13);

      // Heures adaptatives : espace minimal 48 px entre libellés
      int minutes = board.getSlotMinutes();
      int pxPerHour = (60/minutes) * slotW;
      int minSpacing = 48; // px
      int[] steps = {1,2,3,4,6,12};
      int stepH = 2;
      for (int s : steps){ if (pxPerHour * s >= minSpacing){ stepH = s; break; } }

      g2.setFont(getFont().deriveFont(11f));
      g2.setColor(new Color(0x4B5563));
      for (int h=0; h<24; h+=stepH){
        int px = x + h*(60/minutes)*slotW;
        String label = (h<10? "0":"")+h+":00";
        int w = g2.getFontMetrics().stringWidth(label) + 6;
        g2.setColor(PlanningUx.HEADER_BG);
        g2.fillRoundRect(px+2, getHeight()-18, w, 16, 8, 8);
        g2.setColor(new Color(0x4B5563));
        g2.drawString(label, px+5, getHeight()-6);
      }
      x+=dayW;
    }
  }
}
