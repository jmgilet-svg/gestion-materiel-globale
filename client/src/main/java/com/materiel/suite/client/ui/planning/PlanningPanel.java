package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.ui.commands.CommandBus;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PlanningPanel extends JPanel {
  private final PlanningBoard board = new PlanningBoard();
  private final AgendaBoard agenda = new AgendaBoard();


  public PlanningPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);
    var scroll = new JScrollPane(board);
    DayHeader header = new DayHeader(board);
    scroll.setColumnHeaderView(header);
    scroll.getHorizontalScrollBar().addAdjustmentListener(e -> header.repaint());
    var scrollAgenda = new JScrollPane(agenda);

    JPanel center = new JPanel(new CardLayout());
    center.add(scroll, "gantt");
    center.add(scrollAgenda, "agenda");

    JComponent rowHeader = new JComponent(){
      @Override public Dimension getPreferredSize(){ return new Dimension(220, board.getPreferredSize().height); }
      @Override protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(0xF7F7F7));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(new Color(0xDDDDDD));
        g2.drawLine(getWidth()-1,0,getWidth()-1,getHeight());
        int y=0;
        List<Resource> rs = ServiceFactory.planning().listResources();
        for (Resource r : rs){
          int rowH = boardRowHeight(r);
          g2.setColor(new Color(0xF7F7F7));
          g2.fillRect(0,y,getWidth(),rowH);
          g2.setColor(Color.DARK_GRAY);
          g2.drawString(r.getName(), 12, y + rowH/2 + 4);
          g2.setColor(new Color(0xE0E0E0));
          g2.drawLine(0, y+rowH-1, getWidth(), y+rowH-1);
          y+=rowH;
        }
      }
      private int boardRowHeight(Resource r){
        var list = ServiceFactory.planning().listInterventions(board.getStartDate(), board.getStartDate().plusDays(6));
        list.removeIf(it -> !it.getResourceId().equals(r.getId()));
        var lanes = LaneLayout.computeLanes(list, Intervention::getDateHeureDebut, Intervention::getDateHeureFin);
        int lanesCount = lanes.values().stream().mapToInt(l -> l.index).max().orElse(-1) + 1;
        return Math.max(22, lanesCount * (22 + 4)) + 6;
      }
    };
    scroll.setRowHeaderView(rowHeader);
    add(center, BorderLayout.CENTER);
    board.reload();
    agenda.reload();

    putUndoRedoKeymap();
  }

  private JComponent buildToolbar(){
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bar.setBorder(new EmptyBorder(6,6,6,6));
    JButton prev = new JButton("◀ Semaine");
    JButton next = new JButton("Semaine ▶");
    JButton today = new JButton("Aujourd'hui");
    JLabel zoomL = new JLabel("Zoom:");
    JSlider zoom = new JSlider(60,200,100);
    JSpinner snap = new JSpinner(new SpinnerNumberModel(15,5,60,5));
    JToggleButton mode = new JToggleButton("Agenda");
    JButton addI = new JButton("+ Intervention");

    mode.addActionListener(e -> switchMode(mode.isSelected()));

    prev.addActionListener(e -> { board.setStartDate(board.getStartDate().minusDays(7)); agenda.setStartDate(board.getStartDate()); });
    next.addActionListener(e -> { board.setStartDate(board.getStartDate().plusDays(7)); agenda.setStartDate(board.getStartDate()); });
    today.addActionListener(e -> { board.setStartDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY)); agenda.setStartDate(board.getStartDate()); });
    zoom.addChangeListener(e -> { board.setZoom(zoom.getValue()); agenda.setDayWidth(zoom.getValue()); revalidate(); repaint(); });
    snap.addChangeListener(e -> { board.setSnapMinutes((Integer) snap.getValue()); agenda.setSnapMinutes((Integer) snap.getValue()); });
    addI.addActionListener(e -> addInterventionDialog());

    bar.add(prev); bar.add(next); bar.add(today); bar.add(mode);
    bar.add(Box.createHorizontalStrut(16)); bar.add(zoomL); bar.add(zoom);
    bar.add(new JLabel("Snap (min):")); bar.add(snap);
    bar.add(Box.createHorizontalStrut(16)); bar.add(addI);
    return bar;
  }

  private void switchMode(boolean agendaMode){
    CardLayout cl = (CardLayout) ((Container)getComponent(1)).getLayout();
    cl.show((Container)getComponent(1), agendaMode? "agenda" : "gantt");
  }


  private void addInterventionDialog(){
    var rs = ServiceFactory.planning().listResources();
    if (rs.isEmpty()){ JOptionPane.showMessageDialog(this,"Aucune ressource"); return; }
    String[] names = rs.stream().map(Resource::getName).toArray(String[]::new);
    JTextField tfLabel = new JTextField(20);
    JComboBox<String> cbRes = new JComboBox<>(names);
    JTextField tfStart = new JTextField(LocalDateTime.now().withSecond(0).withNano(0).toString(), 16);
    JTextField tfEnd = new JTextField(LocalDateTime.now().plusHours(4).withSecond(0).withNano(0).toString(), 16);
    Object[] msg = {"Libellé:", tfLabel, "Ressource:", cbRes, "Début (YYYY-MM-DDThh:mm):", tfStart, "Fin (YYYY-MM-DDThh:mm):", tfEnd};
    int ok = JOptionPane.showConfirmDialog(this, msg, "Nouvelle intervention", JOptionPane.OK_CANCEL_OPTION);
    if (ok==JOptionPane.OK_OPTION){
      Resource r = rs.get(cbRes.getSelectedIndex());
      Intervention it = new Intervention(UUID.randomUUID(), r.getId(), tfLabel.getText().trim(),
          LocalDateTime.parse(tfStart.getText().trim()), LocalDateTime.parse(tfEnd.getText().trim()), "#88C0D0");
      ServiceFactory.planning().saveIntervention(it);
      board.reload();
      agenda.reload();
    }
  }

  private void putUndoRedoKeymap(){
    int WHEN = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
    getInputMap(WHEN).put(KeyStroke.getKeyStroke("control Z"), "undo");
    getInputMap(WHEN).put(KeyStroke.getKeyStroke("control Y"), "redo");
    getActionMap().put("undo", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ CommandBus.get().undo(); board.reload(); agenda.reload(); }});
    getActionMap().put("redo", new AbstractAction(){ public void actionPerformed(java.awt.event.ActionEvent e){ CommandBus.get().redo(); board.reload(); agenda.reload(); }});
  }

}
