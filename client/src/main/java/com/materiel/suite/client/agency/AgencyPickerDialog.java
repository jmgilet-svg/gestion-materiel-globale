package com.materiel.suite.client.agency;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialogue simple permettant de sélectionner une agence pour le contexte courant.
 */
public class AgencyPickerDialog extends JDialog {
  private final JComboBox<Agency> agencies = new JComboBox<>();
  private final DefaultComboBoxModel<Agency> model = new DefaultComboBoxModel<>();
  private Agency selected;

  public AgencyPickerDialog(Window owner, List<Agency> available){
    super(owner, "Sélection de l'agence", ModalityType.APPLICATION_MODAL);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(10, 10));
    add(buildForm(), BorderLayout.CENTER);
    add(buildActions(), BorderLayout.SOUTH);
    setSize(420, 200);
    setLocationRelativeTo(owner);
    setResizable(false);
    loadAgencies(available);
  }

  public Agency getSelectedAgency(){
    return selected;
  }

  private JPanel buildForm(){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(12, 12, 12, 12);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.HORIZONTAL;

    gc.gridx = 0;
    gc.gridy = 0;
    panel.add(new JLabel("Agence"), gc);

    gc.gridx = 1;
    gc.weightx = 1.0;
    agencies.setModel(model);
    panel.add(agencies, gc);

    return panel;
  }

  private JPanel buildActions(){
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton cancel = new JButton("Annuler");
    JButton ok = new JButton("Valider");
    cancel.addActionListener(e -> {
      selected = null;
      dispose();
    });
    ok.addActionListener(e -> {
      Object value = agencies.getSelectedItem();
      if (value instanceof Agency agency){
        selected = cloneAgency(agency);
        AgencyContext.setAgency(selected);
        if (AuthContext.get() != null){
          AuthContext.get().setAgency(cloneAgency(selected));
        }
      } else {
        selected = null;
        AgencyContext.clear();
      }
      dispose();
    });
    actions.add(cancel);
    actions.add(ok);
    return actions;
  }

  private void loadAgencies(List<Agency> available){
    model.removeAllElements();
    List<Agency> safe = available == null ? List.of() : new ArrayList<>(available);
    for (Agency agency : safe){
      model.addElement(cloneAgency(agency));
    }
    if (model.getSize() > 0){
      Agency current = AuthContext.get() != null ? AuthContext.get().getAgency() : null;
      if (current != null){
        selectMatching(current);
      } else {
        agencies.setSelectedIndex(0);
      }
    }
  }

  private void selectMatching(Agency current){
    if (current == null){
      return;
    }
    String id = current.getId();
    String name = current.getName();
    for (int i = 0; i < model.getSize(); i++){
      Agency candidate = model.getElementAt(i);
      if (candidate == null){
        continue;
      }
      boolean idMatch = id != null && id.equalsIgnoreCase(candidate.getId());
      boolean nameMatch = name != null && name.equalsIgnoreCase(candidate.getName());
      if (idMatch || nameMatch){
        agencies.setSelectedIndex(i);
        return;
      }
    }
    agencies.setSelectedIndex(0);
  }

  private Agency cloneAgency(Agency source){
    if (source == null){
      return null;
    }
    Agency copy = new Agency();
    copy.setId(source.getId());
    copy.setName(source.getName());
    return copy;
  }
}
