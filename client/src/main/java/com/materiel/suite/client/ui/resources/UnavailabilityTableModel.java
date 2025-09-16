package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.Unavailability;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UnavailabilityTableModel extends AbstractTableModel {
  private final List<Unavailability> rows = new ArrayList<>();
  private static final String[] COLS = {"Début", "Fin", "Motif"};
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public void setRows(List<Unavailability> list){
    rows.clear();
    if (list!=null) rows.addAll(list);
    fireTableDataChanged();
  }

  public List<Unavailability> getRows(){
    return new ArrayList<>(rows);
  }

  public void add(Unavailability u){
    rows.add(u);
    int i = rows.size()-1;
    fireTableRowsInserted(i, i);
  }

  public void remove(int idx){
    if (idx<0 || idx>=rows.size()) return;
    rows.remove(idx);
    fireTableRowsDeleted(idx, idx);
  }

  public Unavailability getAt(int idx){
    if (idx<0 || idx>=rows.size()) return null;
    return rows.get(idx);
  }

  @Override public int getRowCount(){ return rows.size(); }
  @Override public int getColumnCount(){ return COLS.length; }
  @Override public String getColumnName(int column){ return COLS[column]; }
  @Override public Object getValueAt(int rowIndex, int columnIndex){
    Unavailability u = rows.get(rowIndex);
    return switch (columnIndex){
      case 0 -> u.getStart()!=null? FMT.format(u.getStart()) : "";
      case 1 -> u.getEnd()!=null? FMT.format(u.getEnd()) : "";
      case 2 -> u.getReason()!=null? u.getReason() : "";
      default -> "";
    };
  }
}
