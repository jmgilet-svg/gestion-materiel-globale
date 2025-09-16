package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.service.PlanningService;

import javax.swing.*;
import java.awt.*;

public class ResourceTypeEditDialog extends JDialog {
  private final JTextField codeField = new JTextField(10);
  private final JTextField labelField = new JTextField(18);
  private final ResourceIconPicker iconPicker = new ResourceIconPicker();
  private final PlanningService service;
  private ResourceType type;
  private final boolean createMode;
  private boolean saved;

  public ResourceTypeEditDialog(Window owner, PlanningService service, ResourceType type){
    super(owner, type==null? "Nouveau type" : "Type de ressource", ModalityType.APPLICATION_MODAL);
    this.service = service;
    this.createMode = (type==null || type.getCode()==null || type.getCode().isBlank());
    this.type = type!=null? clone(type) : new ResourceType();
    buildUI();
    bind();
    pack();
    setLocationRelativeTo(owner);
  }

  public boolean isSaved(){ return saved; }

  private void buildUI(){
    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4,4,4,4);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0; gc.gridy = 0; form.add(new JLabel("Code"), gc);
    gc.gridx = 1; form.add(codeField, gc);
    gc.gridx = 0; gc.gridy++; form.add(new JLabel("Libellé"), gc);
    gc.gridx = 1; form.add(labelField, gc);
    gc.gridx = 0; gc.gridy++; form.add(new JLabel("Icône"), gc);
    gc.gridx = 1; form.add(iconPicker, gc);

    JButton save = new JButton("Enregistrer");
    JButton cancel = new JButton("Annuler");
    save.addActionListener(e -> onSave());
    cancel.addActionListener(e -> dispose());

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    south.add(cancel); south.add(save);

    JPanel root = new JPanel(new BorderLayout(8,8));
    root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
    root.add(form, BorderLayout.CENTER);
    root.add(south, BorderLayout.SOUTH);
    setContentPane(root);
  }

  private void bind(){
    if (!createMode && type.getCode()!=null) codeField.setText(type.getCode());
    if (!createMode) codeField.setEnabled(false);
    if (type.getLabel()!=null) labelField.setText(type.getLabel());
    iconPicker.setValue(type.getIcon());
  }

  private void onSave(){
    try {
      if (createMode){
        String code = codeField.getText().trim();
        if (code.isEmpty()){
          JOptionPane.showMessageDialog(this, "Le code est requis.", "Validation", JOptionPane.WARNING_MESSAGE);
          return;
        }
        type.setCode(code);
      }
      type.setLabel(labelField.getText().trim());
      type.setIcon(iconPicker.getValue());
      ResourceType savedType = createMode? service.createResourceType(type) : service.updateResourceType(type);
      type = savedType!=null? savedType : type;
      saved = true;
      dispose();
    } catch(Exception ex){
      JOptionPane.showMessageDialog(this, "Erreur: "+ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private static ResourceType clone(ResourceType src){
    ResourceType copy = new ResourceType();
    copy.setCode(src.getCode());
    copy.setLabel(src.getLabel());
    copy.setIcon(src.getIcon());
    return copy;
  }
}
