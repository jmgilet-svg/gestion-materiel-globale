package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Vue tabulaire compacte des interventions. */
public class InterventionTableView implements InterventionView {
  private final DefaultTableModel model = new DefaultTableModel(
      new Object[]{"Début", "Fin", "Type", "Titre", "Client", "Adresse", "Ressources"}, 0){
    @Override public boolean isCellEditable(int row, int column){ return false; }
    @Override public Class<?> getColumnClass(int columnIndex){
      if (columnIndex == 0 || columnIndex == 1){
        return LocalDateTime.class;
      }
      if (columnIndex == 6){
        return List.class;
      }
      return Object.class;
    }
  };
  private final JTable table = new JTable(model);
  private final JScrollPane scroller = new JScrollPane(table);
  private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private List<Intervention> current = List.of();
  private Consumer<Intervention> onOpen = it -> {};

  public InterventionTableView(){
    table.setRowHeight(24);
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getColumnModel().getColumn(0).setCellRenderer(dateRenderer());
    table.getColumnModel().getColumn(1).setCellRenderer(dateRenderer());
    table.getColumnModel().getColumn(2).setCellRenderer(typeRenderer());
    table.getColumnModel().getColumn(6).setCellRenderer(resourceRenderer());
    table.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          int row = table.getSelectedRow();
          if (row < 0){
            return;
          }
          int modelRow = table.convertRowIndexToModel(row);
          if (modelRow >= 0 && modelRow < current.size()){
            onOpen.accept(current.get(modelRow));
          }
        }
      }
    });
    scroller.setBorder(BorderFactory.createEmptyBorder());
  }

  @Override public JComponent getComponent(){
    return scroller;
  }

  @Override public void setData(List<Intervention> list){
    List<Intervention> data = new ArrayList<>();
    if (list != null){
      for (Intervention it : list){
        if (it != null){
          data.add(it);
        }
      }
    }
    current = data;
    model.setRowCount(0);
    for (Intervention it : current){
      model.addRow(new Object[]{
          it.getDateHeureDebut(),
          it.getDateHeureFin(),
          it.getType(),
          safe(it.getLabel()),
          safe(it.getClientName()),
          safe(it.getAddress()),
          it.getResources()
      });
    }
  }

  @Override public void setOnOpen(Consumer<Intervention> onOpen){
    this.onOpen = onOpen != null ? onOpen : it -> {};
  }

  private DefaultTableCellRenderer dateRenderer(){
    return new DefaultTableCellRenderer(){
      @Override public void setValue(Object value){
        if (value instanceof LocalDateTime ldt){
          setText(dateFormatter.format(ldt));
        } else {
          setText("");
        }
      }
    };
  }

  private DefaultTableCellRenderer typeRenderer(){
    return new DefaultTableCellRenderer(){
      @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        label.setIcon(null);
        if (value instanceof InterventionType type){
          label.setText(type.toString());
          label.setIcon(IconRegistry.small(type.getIconKey()));
          label.setIconTextGap(6);
        } else {
          label.setText("");
        }
        return label;
      }
    };
  }

  private DefaultTableCellRenderer resourceRenderer(){
    return new DefaultTableCellRenderer(){
      @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        panel.setOpaque(true);
        panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        if (value instanceof List<?> list){
          for (Object obj : list){
            if (obj instanceof ResourceRef ref){
              JLabel label = new JLabel();
              label.setOpaque(false);
              String iconKey = ref.getIcon();
              if (IconRegistry.isKnownKey(iconKey)){
                label.setIcon(IconRegistry.small(iconKey));
              } else if (iconKey != null && !iconKey.isBlank()){
                label.setText(iconKey);
              } else {
                label.setIcon(IconRegistry.placeholder(16));
              }
              label.setToolTipText(ref.getName());
              panel.add(label);
            }
          }
        }
        if (panel.getComponentCount() == 0){
          JLabel empty = new JLabel("—");
          empty.setForeground(table.getForeground());
          panel.add(empty);
        }
        return panel;
      }
    };
  }

  private String safe(String value){
    return value == null ? "" : value;
  }
}
