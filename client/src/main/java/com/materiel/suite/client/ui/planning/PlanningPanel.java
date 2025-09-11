package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PlanningPanel extends JPanel {
  private final PlanningBoard board = new PlanningBoard();

  public PlanningPanel(){
    super(new BorderLayout());
    add(buildToolbar(), BorderLayout.NORTH);
    
    var scroll = new JScrollPane(board);
    DayHeader header = new DayHeader(board);
    scroll.setColumnHeaderView(header);
    scroll.getHorizontalScrollBar().addAdjustmentListener(e -> header.repaint());

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
    add(scroll, BorderLayout.CENTER);
    board.reload();
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
    JButton addI = new JButton("+ Intervention");

    prev.addActionListener(e -> { board.setStartDate(board.getStartDate().minusDays(7)); });
    next.addActionListener(e -> { board.setStartDate(board.getStartDate().plusDays(7)); });
    today.addActionListener(e -> { board.setStartDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY)); });
    zoom.addChangeListener(e -> { board.setZoom(zoom.getValue()); revalidate(); repaint(); });
    snap.addChangeListener(e -> board.setSnapMinutes((Integer) snap.getValue()));
    addI.addActionListener(e -> addInterventionDialog());

    bar.add(prev); bar.add(next); bar.add(today);
    bar.add(Box.createHorizontalStrut(16)); bar.add(zoomL); bar.add(zoom);
    bar.add(new JLabel("Snap (min):")); bar.add(snap);
    bar.add(Box.createHorizontalStrut(16)); bar.add(addI);
    return bar;
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
    }
  }
}
