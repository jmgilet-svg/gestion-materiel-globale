package com.materiel.suite.client.ui.doc;

import com.materiel.suite.client.model.DocumentLine;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.function.Consumer;

public class DocumentLineTableModel extends AbstractTableModel {
  private final String[] cols = {"Désignation","Qté","Unité","PU HT","Remise %","TVA %","Ligne HT","TVA €","Ligne TTC"};
  private final List<DocumentLine> lines;
  private Consumer<List<DocumentLine>> onChange;
  public DocumentLineTableModel(List<DocumentLine> lines){ this.lines = lines; }
  public void onChange(Consumer<List<DocumentLine>> c){ this.onChange=c; }
  @Override public int getRowCount(){ return lines.size(); }
  @Override public int getColumnCount(){ return cols.length; }
  @Override public String getColumnName(int col){ return cols[col]; }
  @Override public boolean isCellEditable(int r, int c){ return c<=5; }
  @Override public Object getValueAt(int r, int c){
    DocumentLine l = lines.get(r);
    return switch (c){
      case 0 -> l.getDesignation();
      case 1 -> l.getQuantite();
      case 2 -> l.getUnite();
      case 3 -> l.getPrixUnitaireHT();
      case 4 -> l.getRemisePct();
      case 5 -> l.getTvaPct();
      case 6 -> l.lineHT();
      case 7 -> l.lineTVA();
      case 8 -> l.lineTTC();
      default -> "";
    };
  }
  @Override public void setValueAt(Object aValue, int r, int c){
    DocumentLine l = lines.get(r);
    switch (c){
      case 0 -> l.setDesignation(aValue.toString());
      case 1 -> l.setQuantite(parse(aValue));
      case 2 -> l.setUnite(aValue.toString());
      case 3 -> l.setPrixUnitaireHT(parse(aValue));
      case 4 -> l.setRemisePct(parse(aValue));
      case 5 -> l.setTvaPct(parse(aValue));
    }
    fireTableRowsUpdated(r,r);
    if (onChange!=null) onChange.accept(lines);
  }
  public void addEmpty(){
    lines.add(new DocumentLine("",1,"",0,0,20));
    int r = lines.size()-1;
    fireTableRowsInserted(r,r);
    if (onChange!=null) onChange.accept(lines);
  }
  public void remove(int row){
    if (row<0 || row>=lines.size()) return;
    lines.remove(row);
    fireTableDataChanged();
    if (onChange!=null) onChange.accept(lines);
  }
  private double parse(Object v){
    try { return Double.parseDouble(v.toString().replace(',','.')); }
    catch(Exception e){ return 0; }
  }
}
