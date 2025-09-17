package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Regroupement par journée pour une lecture "agenda" rapide. */
public class InterventionCalendarView implements InterventionView {
  private final JPanel days = new JPanel();
  private final JScrollPane scroller = new JScrollPane(days);
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
  private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
  private Consumer<Intervention> onOpen = it -> {};
  private BiConsumer<Intervention, LocalDate> onMove = (it, day) -> {};
  private Intervention dragging;
  private JComponent dragSource;
  private static final Color HEADER_BG = new Color(245, 245, 245);
  private static final Color DROP_TARGET_BG = new Color(232, 245, 233);

  public InterventionCalendarView(){
    days.setLayout(new BoxLayout(days, BoxLayout.Y_AXIS));
    days.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    scroller.setBorder(BorderFactory.createEmptyBorder());
    scroller.getVerticalScrollBar().setUnitIncrement(18);
    days.addMouseListener(new MouseAdapter(){
      @Override public void mouseReleased(MouseEvent e){
        dragging = null;
        if (dragSource != null){
          dragSource.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          dragSource = null;
        }
      }
    });
  }

  @Override public JComponent getComponent(){
    return scroller;
  }

  @Override public void setData(List<Intervention> list){
    days.removeAll();
    dragging = null;
    dragSource = null;
    List<Intervention> data = new ArrayList<>();
    if (list != null){
      for (Intervention it : list){
        if (it != null){
          data.add(it);
        }
      }
    }
    if (data.isEmpty()){
      days.add(emptyState());
      refresh();
      return;
    }
    Map<LocalDate, List<Intervention>> byDay = new TreeMap<>();
    for (Intervention it : data){
      LocalDate day = dayOf(it);
      byDay.computeIfAbsent(day, d -> new ArrayList<>()).add(it);
    }
    for (Map.Entry<LocalDate, List<Intervention>> entry : byDay.entrySet()){
      days.add(dayHeader(entry.getKey()));
      entry.getValue().stream()
          .sorted(Comparator.comparing(this::startDateTime))
          .forEach(it -> days.add(row(it)));
      days.add(Box.createVerticalStrut(6));
    }
    refresh();
  }

  @Override public void setOnOpen(Consumer<Intervention> onOpen){
    this.onOpen = onOpen != null ? onOpen : it -> {};
  }

  @Override public void setOnMove(BiConsumer<Intervention, LocalDate> onMove){
    this.onMove = onMove != null ? onMove : (it, day) -> {};
  }

  private void refresh(){
    days.revalidate();
    days.repaint();
  }

  private JComponent emptyState(){
    JLabel label = new JLabel("Aucune intervention", JLabel.CENTER);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
    return label;
  }

  private JComponent dayHeader(LocalDate day){
    String text = dayFormatter.format(day);
    JLabel label = new JLabel(capitalize(text), IconRegistry.small("calendar"), JLabel.LEFT);
    label.setOpaque(true);
    label.setBackground(HEADER_BG);
    label.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
        BorderFactory.createEmptyBorder(6, 8, 4, 8)
    ));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    label.addMouseListener(new MouseAdapter(){
      @Override public void mouseEntered(MouseEvent e){
        if (dragging != null){
          label.setBackground(DROP_TARGET_BG);
        }
      }
      @Override public void mouseExited(MouseEvent e){
        label.setBackground(HEADER_BG);
      }
      @Override public void mouseReleased(MouseEvent e){
        if (dragging != null){
          try {
            onMove.accept(dragging, day);
          } finally {
            dragging = null;
          }
        }
        if (dragSource != null){
          dragSource.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          dragSource = null;
        }
        label.setBackground(HEADER_BG);
      }
    });
    return label;
  }

  private JComponent row(Intervention it){
    JPanel panel = new JPanel(new BorderLayout(8, 0));
    panel.setOpaque(true);
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
        BorderFactory.createEmptyBorder(6, 8, 6, 8)
    ));
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel iconLabel = new JLabel();
    InterventionType type = it.getType();
    if (type != null){
      iconLabel.setIcon(IconRegistry.small(type.getIconKey()));
    }
    panel.add(iconLabel, BorderLayout.WEST);

    LocalDateTime start = it.getDateHeureDebut();
    String time = start != null ? timeFormatter.format(start) : "";
    String title = escape(it.getLabel());
    String client = escape(it.getClientName());
    String address = escape(it.getAddress());
    StringBuilder html = new StringBuilder("<html><b>").append(title).append("</b>");
    if (!client.isBlank()){
      html.append(" — ").append(client);
    }
    if (!time.isBlank()){
      html.append(" — ").append(time);
    }
    if (!address.isBlank()){
      html.append("<br/><span style='color:#555555'>").append(address).append("</span>");
    }
    html.append("</html>");
    JLabel text = new JLabel(html.toString());
    panel.add(text, BorderLayout.CENTER);

    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    panel.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          onOpen.accept(it);
        }
      }
      @Override public void mousePressed(MouseEvent e){
        dragging = it;
        dragSource = panel;
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      }
      @Override public void mouseReleased(MouseEvent e){
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (dragging == it){
          dragging = null;
        }
        if (dragSource == panel){
          dragSource = null;
        }
      }
    });
    return panel;
  }

  private LocalDate dayOf(Intervention it){
    LocalDateTime start = it.getDateHeureDebut();
    if (start != null){
      return start.toLocalDate();
    }
    LocalDate day = it.getDateDebut();
    if (day != null){
      return day;
    }
    return LocalDate.of(1970, 1, 1);
  }

  private LocalDateTime startDateTime(Intervention it){
    LocalDateTime start = it.getDateHeureDebut();
    if (start != null){
      return start;
    }
    LocalDate day = it.getDateDebut();
    if (day != null){
      return day.atStartOfDay();
    }
    return LocalDateTime.MIN;
  }

  private String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .trim();
  }

  private String capitalize(String value){
    if (value == null || value.isBlank()){
      return "";
    }
    return value.substring(0, 1).toUpperCase(Locale.FRENCH) + value.substring(1);
  }
}
