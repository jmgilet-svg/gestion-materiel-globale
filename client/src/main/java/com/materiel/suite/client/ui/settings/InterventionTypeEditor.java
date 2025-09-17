package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconPickerDialog;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Éditeur simple pour les types d'intervention (nom + icône). */
public class InterventionTypeEditor extends JPanel {
  private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Icône", "Nom", "Ordre", "__Code"}, 0){
    @Override public boolean isCellEditable(int row, int column){
      return column == 1;
    }
  };
  private final JTable table = new JTable(model);
  private final JButton addButton = new JButton("Ajouter", IconRegistry.small("plus"));
  private final JButton editButton = new JButton("Modifier", IconRegistry.small("edit"));
  private final JButton deleteButton = new JButton("Supprimer", IconRegistry.small("trash"));
  private final JButton refreshButton = new JButton("Recharger", IconRegistry.small("refresh"));
  private final JButton duplicateButton = new JButton("Dupliquer", IconRegistry.small("file"));
  private final JButton moveUpButton = new JButton("Monter", IconRegistry.small("maximize"));
  private final JButton moveDownButton = new JButton("Descendre", IconRegistry.small("minimize"));
  private final List<InterventionType> data = new ArrayList<>();
  private boolean updatingModel;

  public InterventionTypeEditor(){
    super(new BorderLayout(8, 8));
    table.setRowHeight(28);
    table.setFillsViewportHeight(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.getColumnModel().getColumn(0).setCellRenderer(new IconRenderer());
    DefaultTableCellRenderer orderRenderer = new DefaultTableCellRenderer();
    orderRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    table.getColumnModel().getColumn(2).setMaxWidth(80);
    table.getColumnModel().getColumn(2).setCellRenderer(orderRenderer);
    table.getColumnModel().getColumn(3).setMinWidth(0);
    table.getColumnModel().getColumn(3).setMaxWidth(0);
    table.getColumnModel().getColumn(3).setPreferredWidth(0);

    table.setDragEnabled(true);
    table.setDropMode(DropMode.INSERT_ROWS);
    table.setTransferHandler(new RowReorderHandler());

    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "editName");
    table.getActionMap().put("editName", new AbstractAction(){
      @Override public void actionPerformed(java.awt.event.ActionEvent e){
        int viewRow = table.getSelectedRow();
        if (viewRow < 0){
          return;
        }
        if (table.editCellAt(viewRow, 1)){
          Component editor = table.getEditorComponent();
          if (editor != null){
            editor.requestFocus();
          }
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    add(scrollPane, BorderLayout.CENTER);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(addButton);
    toolbar.add(editButton);
    toolbar.add(duplicateButton);
    toolbar.add(deleteButton);
    toolbar.addSeparator();
    toolbar.add(moveUpButton);
    toolbar.add(moveDownButton);
    toolbar.addSeparator();
    toolbar.add(refreshButton);
    add(toolbar, BorderLayout.NORTH);

    addButton.addActionListener(e -> openEditor(null));
    editButton.addActionListener(e -> {
      InterventionType type = selectedType();
      if (type != null){
        openEditor(type);
      }
    });
    deleteButton.addActionListener(e -> deleteSelected());
    refreshButton.addActionListener(e -> reload());
    duplicateButton.addActionListener(e -> duplicateSelected());
    moveUpButton.addActionListener(e -> moveSelected(-1));
    moveDownButton.addActionListener(e -> moveSelected(1));

    table.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          InterventionType type = selectedType();
          if (type != null){
            openEditor(type);
          }
        }
      }
    });

    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()){
        updateButtonStates();
      }
    });

    model.addTableModelListener(new InlineRenameListener());

    reload();
  }

  private void reload(){
    updatingModel = true;
    try {
      List<InterventionType> list = ServiceLocator.interventionTypes().list();
      data.clear();
      if (list != null){
        data.addAll(list);
      }
      model.setRowCount(0);
      for (InterventionType type : data){
        model.addRow(new Object[]{type.getIconKey(), type.getLabel(), type.getOrderIndex(), type.getCode()});
      }
    } finally {
      updatingModel = false;
    }
    updateButtonStates();
  }

  private InterventionType selectedType(){
    int viewRow = table.getSelectedRow();
    if (viewRow < 0){
      return null;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    if (modelRow < 0 || modelRow >= data.size()){
      return null;
    }
    return data.get(modelRow);
  }

  private void deleteSelected(){
    InterventionType type = selectedType();
    if (type == null){
      return;
    }
    int confirm = JOptionPane.showConfirmDialog(this,
        "Supprimer le type \"" + safe(type.getLabel()) + "\" ?",
        "Confirmation", JOptionPane.YES_NO_OPTION);
    if (confirm != JOptionPane.YES_OPTION){
      return;
    }
    ServiceLocator.interventionTypes().delete(type.getCode());
    Toasts.success(this, "Type supprimé");
    reload();
  }

  private void moveSelected(int delta){
    if (delta == 0){
      return;
    }
    int viewRow = table.getSelectedRow();
    if (viewRow < 0){
      return;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    if (modelRow < 0 || modelRow >= data.size()){
      return;
    }
    int target = modelRow + delta;
    if (target < 0 || target >= data.size()){
      return;
    }
    List<InterventionType> reordered = new ArrayList<>(data);
    Collections.swap(reordered, modelRow, target);
    String codeToSelect = reordered.get(target).getCode();
    renumberAndPersist(reordered);
    reload();
    selectCode(codeToSelect);
  }

  private void duplicateSelected(){
    InterventionType source = selectedType();
    if (source == null){
      return;
    }
    List<InterventionType> reordered = new ArrayList<>(data);
    int index = reordered.indexOf(source);
    if (index < 0){
      index = reordered.size() - 1;
    }
    InterventionType duplicate = new InterventionType();
    duplicate.setLabel(suggestDuplicateName(source.getLabel(), reordered));
    duplicate.setIconKey(source.getIconKey());
    InterventionType saved = ServiceLocator.interventionTypes().save(duplicate);
    if (saved == null){
      return;
    }
    reordered.add(index + 1, saved);
    renumberAndPersist(reordered);
    reload();
    selectCode(saved.getCode());
    Toasts.success(this, "Type dupliqué");
  }

  private String suggestDuplicateName(String base, List<InterventionType> list){
    String cleaned = safe(base).trim();
    if (cleaned.isEmpty()){
      cleaned = "Type";
    }
    Set<String> names = new HashSet<>();
    for (InterventionType type : list){
      names.add(safe(type.getLabel()).trim());
    }
    String candidate = cleaned + " (copie)";
    if (!names.contains(candidate)){
      return candidate;
    }
    int counter = 2;
    while (names.contains(cleaned + " (copie " + counter + ")")){
      counter++;
    }
    return cleaned + " (copie " + counter + ")";
  }

  private void renumberAndPersist(List<InterventionType> ordered){
    for (int i = 0; i < ordered.size(); i++){
      InterventionType type = copy(ordered.get(i));
      type.setOrderIndex(i);
      ServiceLocator.interventionTypes().save(type);
    }
  }

  private InterventionType findByCode(String code){
    if (code == null){
      return null;
    }
    for (InterventionType type : data){
      if (type != null && code.equals(type.getCode())){
        return type;
      }
    }
    return null;
  }

  private void updateCellSilently(int row, int column, Object value){
    if (row < 0 || row >= model.getRowCount()){
      return;
    }
    updatingModel = true;
    try {
      model.setValueAt(value, row, column);
    } finally {
      updatingModel = false;
    }
  }

  private void openEditor(InterventionType existing){
    InterventionType working = existing == null ? new InterventionType() : copy(existing);
    Window owner = SwingUtilities.getWindowAncestor(this);
    JDialog dialog = new JDialog(owner, existing == null ? "Nouveau type" : "Modifier le type", Dialog.ModalityType.APPLICATION_MODAL);

    JTextField nameField = new JTextField(safe(working.getLabel()));
    JTextField iconField = new JTextField(safe(working.getIconKey()));
    iconField.setEditable(false);
    JLabel iconPreview = new JLabel(IconRegistry.large(iconField.getText()));
    iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
    iconPreview.setPreferredSize(new Dimension(36, 36));
    JButton chooseIcon = new JButton("Choisir…", IconRegistry.small("image"));
    chooseIcon.addActionListener(e -> {
      IconPickerDialog picker = new IconPickerDialog(owner);
      String chosen = picker.pick();
      if (chosen != null && !chosen.isBlank()){
        iconField.setText(chosen);
        iconPreview.setIcon(IconRegistry.large(chosen));
      }
    });

    JPanel iconRow = new JPanel(new BorderLayout(6, 0));
    iconRow.add(iconPreview, BorderLayout.WEST);
    iconRow.add(iconField, BorderLayout.CENTER);
    iconRow.add(chooseIcon, BorderLayout.EAST);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0; gc.gridy = 0;
    form.add(new JLabel("Nom"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(nameField, gc);
    gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
    form.add(new JLabel("Icône"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(iconRow, gc);

    JButton saveButton = new JButton("Enregistrer", IconRegistry.small("success"));
    JButton cancelButton = new JButton("Annuler");
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(saveButton);
    actions.add(cancelButton);

    dialog.getContentPane().add(form, BorderLayout.CENTER);
    dialog.getContentPane().add(actions, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);

    saveButton.addActionListener(e -> {
      String name = nameField.getText() != null ? nameField.getText().trim() : "";
      if (name.isEmpty()){
        Toasts.error(dialog, "Le nom est requis");
        return;
      }
      working.setLabel(name);
      String icon = iconField.getText();
      working.setIconKey(icon == null || icon.isBlank() ? null : icon.trim());
      InterventionType saved = ServiceLocator.interventionTypes().save(working);
      Toasts.success(this, "Type enregistré");
      dialog.dispose();
      reload();
      if (saved != null){
        selectCode(saved.getCode());
      }
    });

    cancelButton.addActionListener(e -> dialog.dispose());
    dialog.setVisible(true);
  }

  private void selectCode(String code){
    if (code == null){
      return;
    }
    for (int i = 0; i < data.size(); i++){
      InterventionType type = data.get(i);
      if (type != null && code.equals(type.getCode())){
        int viewRow = table.convertRowIndexToView(i);
        table.setRowSelectionInterval(viewRow, viewRow);
        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        updateButtonStates();
        break;
      }
    }
  }

  private InterventionType copy(InterventionType type){
    if (type == null){
      return null;
    }
    InterventionType copy = new InterventionType();
    copy.setCode(type.getCode());
    copy.setLabel(type.getLabel());
    copy.setIconKey(type.getIconKey());
    copy.setOrderIndex(type.getOrderIndex());
    return copy;
  }

  private String safe(String value){
    return value == null ? "" : value;
  }

  private void updateButtonStates(){
    int viewRow = table.getSelectedRow();
    int modelRow = viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    boolean hasSelection = modelRow >= 0 && modelRow < data.size();
    editButton.setEnabled(hasSelection);
    deleteButton.setEnabled(hasSelection);
    duplicateButton.setEnabled(hasSelection);
    moveUpButton.setEnabled(hasSelection && modelRow > 0);
    moveDownButton.setEnabled(hasSelection && modelRow >= 0 && modelRow < data.size() - 1);
  }

  private static class IconRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
      JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      String key = value == null ? "" : value.toString();
      label.setIcon(IconRegistry.small(key));
      return label;
    }
  }

  private final class InlineRenameListener implements TableModelListener {
    @Override public void tableChanged(TableModelEvent event){
      if (updatingModel){
        return;
      }
      if (event.getType() != TableModelEvent.UPDATE){
        return;
      }
      int column = event.getColumn();
      if (column != 1){
        return;
      }
      int row = event.getFirstRow();
      if (row < 0 || row >= data.size()){
        return;
      }
      Object raw = model.getValueAt(row, column);
      String newName = raw == null ? "" : raw.toString().trim();
      Object codeValue = model.getValueAt(row, 3);
      String code = codeValue == null ? null : codeValue.toString();
      InterventionType current = findByCode(code);
      if (current == null){
        reload();
        return;
      }
      String original = safe(current.getLabel()).trim();
      if (newName.isEmpty()){
        Toasts.error(InterventionTypeEditor.this, "Le nom est requis");
        updateCellSilently(row, column, original);
        return;
      }
      if (newName.equals(original)){
        String rawText = raw == null ? "" : raw.toString();
        if (!newName.equals(rawText)){
          updateCellSilently(row, column, original);
        }
        return;
      }
      try {
        InterventionType toSave = copy(current);
        toSave.setLabel(newName);
        InterventionType saved = ServiceLocator.interventionTypes().save(toSave);
        if (saved != null){
          current.setLabel(saved.getLabel());
          current.setIconKey(saved.getIconKey());
          current.setOrderIndex(saved.getOrderIndex());
          updateCellSilently(row, column, saved.getLabel());
          Toasts.success(InterventionTypeEditor.this, "Nom mis à jour");
        } else {
          updateCellSilently(row, column, original);
        }
      } catch (RuntimeException ex){
        updateCellSilently(row, column, original);
        Toasts.error(InterventionTypeEditor.this, "Échec de la mise à jour : " + ex.getMessage());
      }
    }
  }

  private final class RowReorderHandler extends TransferHandler {
    private final DataFlavor rowsFlavor = new DataFlavor(int[].class, "application/x-java-int-array");

    @Override public int getSourceActions(JComponent c){
      return MOVE;
    }

    @Override protected Transferable createTransferable(JComponent c){
      int[] selected = table.getSelectedRows();
      final int[] rows = Arrays.stream(selected).map(table::convertRowIndexToModel).toArray();
      return new Transferable(){
        @Override public DataFlavor[] getTransferDataFlavors(){
          return new DataFlavor[]{rowsFlavor};
        }

        @Override public boolean isDataFlavorSupported(DataFlavor flavor){
          return rowsFlavor.equals(flavor);
        }

        @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
          if (!isDataFlavorSupported(flavor)){
            throw new UnsupportedFlavorException(flavor);
          }
          return rows;
        }
      };
    }

    @Override public boolean canImport(TransferSupport support){
      return support.isDrop() && support.isDataFlavorSupported(rowsFlavor);
    }

    @Override public boolean importData(TransferSupport support){
      if (!canImport(support)){
        return false;
      }
      JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
      int dropRow = dropLocation.getRow();
      int dropModelIndex = dropRow < 0 ? model.getRowCount() : table.convertRowIndexToModel(dropRow);
      try {
        int[] dragged = (int[]) support.getTransferable().getTransferData(rowsFlavor);
        if (dragged.length == 0){
          return false;
        }
        List<InterventionType> reordered = new ArrayList<>(data);
        List<InterventionType> moving = new ArrayList<>();
        Arrays.sort(dragged);
        int insertIndex = dropModelIndex;
        for (int i = dragged.length - 1; i >= 0; i--){
          int index = dragged[i];
          if (index < 0 || index >= reordered.size()){
            continue;
          }
          moving.add(0, reordered.remove(index));
          if (index < insertIndex){
            insertIndex--;
          }
        }
        if (moving.isEmpty()){
          return false;
        }
        insertIndex = Math.max(0, Math.min(insertIndex, reordered.size()));
        reordered.addAll(insertIndex, moving);
        String codeToSelect = moving.get(0).getCode();
        renumberAndPersist(reordered);
        reload();
        selectCode(codeToSelect);
        return true;
      } catch (UnsupportedFlavorException | IOException ex){
        Toasts.error(InterventionTypeEditor.this, "Réorganisation impossible : " + ex.getMessage());
        return false;
      }
    }
  }
}
