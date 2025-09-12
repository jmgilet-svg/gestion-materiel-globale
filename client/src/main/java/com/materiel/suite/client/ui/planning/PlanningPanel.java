package com.materiel.suite.client.ui.planning;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.commands.CommandBus;

public class PlanningPanel extends JPanel {
  private final PlanningBoard board = new PlanningBoard();
  private final AgendaBoard agenda = new AgendaBoard();
  private JButton conflictsBtn;

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
    conflictsBtn = new JButton("Conflits (0)");
    JButton addI = new JButton("+ Intervention");

    mode.addActionListener(e -> switchMode(mode.isSelected()));
    conflictsBtn.addActionListener(e -> openConflictsDialog());

    prev.addActionListener(e -> { board.setStartDate(board.getStartDate().minusDays(7)); agenda.setStartDate(board.getStartDate()); });
    next.addActionListener(e -> { board.setStartDate(board.getStartDate().plusDays(7)); agenda.setStartDate(board.getStartDate()); });
    today.addActionListener(e -> { board.setStartDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY)); agenda.setStartDate(board.getStartDate()); });
    zoom.addChangeListener(e -> { board.setZoom(zoom.getValue()); agenda.setDayWidth(zoom.getValue()); revalidate(); repaint(); });
    snap.addChangeListener(e -> { board.setSnapMinutes((Integer) snap.getValue()); agenda.setSnapMinutes((Integer) snap.getValue()); });
    addI.addActionListener(e -> addInterventionDialog());

    bar.add(prev); bar.add(next); bar.add(today); bar.add(mode);
    bar.add(Box.createHorizontalStrut(16)); bar.add(zoomL); bar.add(zoom);
    bar.add(new JLabel("Snap (min):")); bar.add(snap);
    bar.add(Box.createHorizontalStrut(8)); bar.add(conflictsBtn);
    bar.add(Box.createHorizontalStrut(16)); bar.add(addI);
    return bar;
  }

  private void switchMode(boolean agendaMode){
    CardLayout cl = (CardLayout) ((Container)getComponent(1)).getLayout();
    cl.show((Container)getComponent(1), agendaMode? "agenda" : "gantt");
  }

  private void openConflictsDialog(){
    var from = board.getStartDate();
    var to = from.plusDays(6);
    java.util.List<Conflict> conflicts = ServiceFactory.planning().listConflicts(from, to);
    conflictsBtn.setText("Conflits ("+conflicts.size()+")");
    if (conflicts.isEmpty()){
      JOptionPane.showMessageDialog(this, "Aucun conflit sur la période.", "Conflits", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    DefaultListModel<Conflict> model = new DefaultListModel<>();
    conflicts.forEach(model::addElement);
    JList<Conflict> list = new JList<>(model);
    list.setCellRenderer((jl,c,idx,sel,focus)->{
      JLabel l = new JLabel("Ressource "+c.getResourceId()+" — "+c.getA()+" ↔ "+c.getB());
      if(sel) l.setOpaque(true);
      return l;
    });
    list.setVisibleRowCount(12);

    JButton shift = new JButton("Décaler +30 min");
    JButton reassign = new JButton("Changer ressource…");
    JButton split = new JButton("Couper à…");
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(shift); actions.add(reassign); actions.add(split);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JScrollPane(list), BorderLayout.CENTER);
    panel.add(actions, BorderLayout.SOUTH);

    JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Conflits détectés", Dialog.ModalityType.APPLICATION_MODAL);
    dlg.setContentPane(panel);
    dlg.setSize(560,420);
    dlg.setLocationRelativeTo(this);

    PlanningService svc = ServiceFactory.planning();

    shift.addActionListener(e->{
      Conflict c=list.getSelectedValue(); if(c==null) return;
      svc.resolveShift(c.getB(),30); refreshPlanning(); conflictsBtn.setText("Conflits (?)");
    });
    reassign.addActionListener(e->{
      Conflict c=list.getSelectedValue(); if(c==null) return;
      var rs=ServiceFactory.planning().listResources();
      String[] names=rs.stream().map(Resource::getName).toArray(String[]::new);
      int idx=JOptionPane.showOptionDialog(dlg,"Choisir la ressource cible :","Reassigner",JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.QUESTION_MESSAGE,null,names,names[0]);
      if(idx>=0){ svc.resolveReassign(c.getB(), rs.get(idx).getId()); refreshPlanning(); conflictsBtn.setText("Conflits (?)"); }
    });
    split.addActionListener(e->{
      Conflict c=list.getSelectedValue(); if(c==null) return;
      String at=JOptionPane.showInputDialog(dlg,"Heure de coupe (HH:mm) :","10:00");
      try{
        LocalTime t=LocalTime.parse(at);
        LocalDateTime dt=from.atTime(t);
        svc.resolveSplit(c.getB(), dt); refreshPlanning(); conflictsBtn.setText("Conflits (?)");
      }catch(Exception ex){ JOptionPane.showMessageDialog(dlg,"Format invalide."); }
    });

    dlg.setVisible(true);
  }

  private void refreshPlanning(){ board.reload(); agenda.reload(); }

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
