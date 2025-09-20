package com.materiel.suite.client.ui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/** Renderers that highlight cells manually overridden by the user. */
public final class OverridableCellRenderers {
  private OverridableCellRenderers(){
  }

  public interface ManualOverrideAware {
    boolean isManualOverride(int rowIndex, int columnIndex);
  }

  /** Highlights cells when the underlying model marks them as manually overridden. */
  public static class ManualOverrideHighlightRenderer extends DefaultTableCellRenderer {
    public ManualOverrideHighlightRenderer(){
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column){
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (table == null || isSelected){
        return component;
      }
      component.setBackground(Color.WHITE);
      TableModel model = table.getModel();
      if (model instanceof ManualOverrideAware aware){
        int modelRow = table.convertRowIndexToModel(row);
        int modelColumn = table.convertColumnIndexToModel(column);
        if (aware.isManualOverride(modelRow, modelColumn)){
          component.setBackground(new Color(0xFFF8E1));
        }
      }
      return component;
    }
  }
}
