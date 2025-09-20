package com.materiel.suite.client.ui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/** Renderers that highlight cells manually overridden by the user. */
public final class OverridableCellRenderers {
  private OverridableCellRenderers(){
  }

  public interface ManualOverrideAware {
    boolean isManualOverride(int rowIndex, int columnIndex);
  }

  /** Highlights cells when the underlying model marks them as manually overridden. */
  public static class ManualOverrideHighlightRenderer extends DefaultTableCellRenderer {
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

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
      String columnName = table.getColumnName(column);
      if (value instanceof Number number && columnName != null){
        String lower = columnName.toLowerCase(Locale.ROOT);
        if (lower.contains("total") || lower.contains("pu") || lower.contains("prix")){
          setText(currencyFormat.format(number.doubleValue()));
        }
      }
      return component;
    }
  }
}
