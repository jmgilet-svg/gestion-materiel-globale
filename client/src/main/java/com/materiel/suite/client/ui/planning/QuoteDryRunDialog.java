package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Aperçu avant génération de devis pour une sélection d'interventions. */
public class QuoteDryRunDialog extends JDialog {
  public QuoteDryRunDialog(Window owner,
                           List<Intervention> willCreate,
                           List<Intervention> willSkip,
                           Consumer<Boolean> onClose){
    super(owner, "Prévisualisation — Génération de devis", ModalityType.APPLICATION_MODAL);
    setSize(640, 420);
    setLocationRelativeTo(owner);
    setContentPane(buildContent(willCreate, willSkip, onClose));
  }

  private JComponent buildContent(List<Intervention> willCreate,
                                  List<Intervention> willSkip,
                                  Consumer<Boolean> onClose){
    JPanel root = new JPanel(new BorderLayout(8, 8));
    root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    JPanel header = new JPanel(new GridLayout(1, 2, 8, 8));
    header.add(summaryPanel("Seront générés", willCreate.size(), new Color(0x43A047)));
    header.add(summaryPanel("Ignorés", willSkip.size(), new Color(0xFB8C00)));
    root.add(header, BorderLayout.NORTH);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        listPanel("À créer", willCreate),
        listPanel("Ignorés", willSkip));
    split.setResizeWeight(0.5);
    root.add(split, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton cancel = new JButton("Annuler");
    JButton confirm = new JButton("Lancer la génération");
    actions.add(cancel);
    actions.add(confirm);
    cancel.addActionListener(e -> {
      dispose();
      if (onClose != null){
        onClose.accept(false);
      }
    });
    confirm.addActionListener(e -> {
      dispose();
      if (onClose != null){
        onClose.accept(true);
      }
    });
    root.add(actions, BorderLayout.SOUTH);
    return root;
  }

  private JPanel summaryPanel(String title, int count, Color color){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.gridx = 0;
    gc.gridy = 0;
    JLabel titleLabel = new JLabel(title);
    titleLabel.setForeground(color);
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    panel.add(titleLabel, gc);
    gc.gridy++;
    JLabel countLabel = new JLabel(String.valueOf(count));
    countLabel.setFont(countLabel.getFont().deriveFont(24f));
    panel.add(countLabel, gc);
    return panel;
  }

  private JScrollPane listPanel(String title, List<Intervention> interventions){
    DefaultListModel<String> model = new DefaultListModel<>();
    for (Intervention intervention : interventions){
      if (intervention == null){
        continue;
      }
      String label = intervention.getLabel();
      if (label == null || label.isBlank()){
        label = "(Sans titre)";
      }
      String client = intervention.getClientName();
      if (client != null && !client.isBlank()){
        label = label + " — " + client;
      }
      model.addElement(label);
    }
    JList<String> list = new JList<>(model);
    list.setVisibleRowCount(12);
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(new JLabel(title), BorderLayout.NORTH);
    wrapper.add(new JScrollPane(list), BorderLayout.CENTER);
    return new JScrollPane(wrapper);
  }
}
