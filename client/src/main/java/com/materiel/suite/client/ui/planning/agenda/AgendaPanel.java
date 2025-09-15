package com.materiel.suite.client.ui.planning.agenda;

import com.materiel.suite.client.ui.MainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/**
 * Standalone agenda panel showing a single day.
 * Provides navigation back to the planning view.
 */
public class AgendaPanel extends JPanel {
  private final AgendaView view = new AgendaView();

  public AgendaPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);
    add(view, BorderLayout.CENTER);
    view.setDay(LocalDate.now());
  }

  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    JButton prev = new JButton("◀ Jour");
    JButton next = new JButton("Jour ▶");
    JButton today = new JButton("Aujourd'hui");
    JButton toPlanning = new JButton("↔ Planning");
    JLabel l = new JLabel("Début:");
    JComboBox<String> start = new JComboBox<>(new String[]{"06:00", "07:00", "08:00"});
    start.setSelectedItem("07:00");
    JLabel l2 = new JLabel("Fin:");
    JComboBox<String> end = new JComboBox<>(new String[]{"17:00", "18:00", "19:00"});
    end.setSelectedItem("18:00");

    next.addActionListener(e -> view.setDay(view.getDay().plusDays(1)));
    prev.addActionListener(e -> view.setDay(view.getDay().minusDays(1)));
    today.addActionListener(e -> view.setDay(LocalDate.now()));
    start.addActionListener(e -> view.setDayStart(parse((String)start.getSelectedItem())));
    end.addActionListener(e -> view.setDayEnd(parse((String)end.getSelectedItem())));
    toPlanning.addActionListener(e -> navigate("planning"));

    bar.add(prev); bar.add(next); bar.add(today);
    bar.add(Box.createHorizontalStrut(16)); bar.add(toPlanning);
    bar.add(Box.createHorizontalStrut(16)); bar.add(l); bar.add(start);
    bar.add(Box.createHorizontalStrut(8)); bar.add(l2); bar.add(end);
    return bar;
  }

  private int parse(String hhmm){
    String[] p = hhmm.split(":" );
    return Integer.parseInt(p[0])*60 + Integer.parseInt(p[1]);
  }

  private void navigate(String key){
    var w = SwingUtilities.getWindowAncestor(this);
    if (w instanceof MainFrame mf) mf.openCard(key);
  }
}

