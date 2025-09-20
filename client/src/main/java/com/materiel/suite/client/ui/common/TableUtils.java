package com.materiel.suite.client.ui.common;

import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;
import java.util.prefs.Preferences;

/** Persistance simple des largeurs de colonnes par clé. */
public final class TableUtils {
  private static final String NODE_PATH = "gestion-materiel/tablewidths";

  private TableUtils(){
  }

  public static void persistColumnWidths(JTable table, String key){
    if (table == null || key == null || key.isBlank()){
      return;
    }
    TableColumnModel columnModel = table.getColumnModel();
    if (columnModel == null){
      return;
    }
    Preferences prefs = Preferences.userRoot().node(NODE_PATH);
    for (int i = 0; i < columnModel.getColumnCount(); i++){
      int stored = prefs.getInt(key + "#" + i, -1);
      if (stored > 0){
        columnModel.getColumn(i).setPreferredWidth(stored);
      }
    }
    columnModel.addColumnModelListener(new TableColumnModelListener(){
      @Override public void columnAdded(TableColumnModelEvent e){ }
      @Override public void columnRemoved(TableColumnModelEvent e){ }
      @Override public void columnMoved(TableColumnModelEvent e){ }
      @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e){ }

      @Override
      public void columnMarginChanged(javax.swing.event.ChangeEvent e){
        for (int i = 0; i < columnModel.getColumnCount(); i++){
          int width = columnModel.getColumn(i).getWidth();
          if (width > 0){
            prefs.putInt(key + "#" + i, width);
          }
        }
      }
    });
  }
}
