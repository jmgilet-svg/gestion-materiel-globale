package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplatesGateway;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Boîte de dialogue minimaliste pour sélectionner un template d'un type donné.
 */
public class TemplatePickerDialog extends JDialog {
  private final JList<TemplatesGateway.Template> list = new JList<>(new DefaultListModel<>());
  private TemplatesGateway.Template selected;

  public TemplatePickerDialog(Window owner, String type){
    super(owner, "Sélectionner un modèle (" + type + ")", ModalityType.APPLICATION_MODAL);
    setLayout(new BorderLayout(8, 8));

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer((lst, value, index, isSelected, cellHasFocus) -> {
      String label = value == null ? "" : buildLabel(value);
      JLabel renderer = new JLabel(label);
      if (isSelected){
        renderer.setOpaque(true);
        renderer.setBackground(lst.getSelectionBackground());
        renderer.setForeground(lst.getSelectionForeground());
      }
      renderer.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
      return renderer;
    });
    JScrollPane scroll = new JScrollPane(list);
    add(scroll, BorderLayout.CENTER);

    JButton choose = new JButton("Choisir");
    JButton cancel = new JButton("Annuler");
    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    south.add(cancel);
    south.add(choose);
    add(south, BorderLayout.SOUTH);

    setSize(520, 420);
    setLocationRelativeTo(owner);

    load(type);

    choose.addActionListener(e -> {
      selected = list.getSelectedValue();
      dispose();
    });
    cancel.addActionListener(e -> {
      selected = null;
      dispose();
    });
  }

  private void load(String type){
    DefaultListModel<TemplatesGateway.Template> model = (DefaultListModel<TemplatesGateway.Template>) list.getModel();
    model.clear();
    try {
      List<TemplatesGateway.Template> templates = ServiceLocator.templates().list(type);
      for (TemplatesGateway.Template template : templates){
        model.addElement(template);
      }
      if (!model.isEmpty()){
        list.setSelectedIndex(0);
      }
    } catch (Exception ignore){
      // aucun template disponible
    }
  }

  private String buildLabel(TemplatesGateway.Template template){
    StringBuilder sb = new StringBuilder();
    if (template.name() != null && !template.name().isBlank()){
      sb.append(template.name());
    } else if (template.key() != null && !template.key().isBlank()){
      sb.append(template.key());
    } else {
      sb.append(template.id() == null ? "" : template.id());
    }
    if (template.key() != null && !template.key().isBlank()){
      sb.append("  [").append(template.key()).append(']');
    }
    return sb.toString();
  }

  public TemplatesGateway.Template pick(){
    setVisible(true);
    return selected;
  }
}
