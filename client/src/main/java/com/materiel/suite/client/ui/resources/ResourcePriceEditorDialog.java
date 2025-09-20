package com.materiel.suite.client.ui.resources;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Boîte de dialogue légère pour modifier le prix unitaire HT d'une ressource.
 */
public class ResourcePriceEditorDialog extends JDialog {
  private final Resource resource;
  private final JFormattedTextField priceField;

  public ResourcePriceEditorDialog(Window owner, Resource resource){
    super(owner, "Tarif ressource", ModalityType.APPLICATION_MODAL);
    if (resource == null){
      throw new IllegalArgumentException("resource is required");
    }
    if (!AccessControl.canEditResources()){
      throw new IllegalStateException("Accès refusé : édition des ressources");
    }
    this.resource = resource;

    setLayout(new BorderLayout(8, 8));
    add(buildForm(), BorderLayout.CENTER);
    add(buildActions(), BorderLayout.SOUTH);

    pack();
    setMinimumSize(new Dimension(380, getPreferredSize().height));
    setLocationRelativeTo(owner);
  }

  private JPanel buildForm(){
    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 6, 6, 6);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 0;

    JTextField nameField = new JTextField(resource.getName() != null ? resource.getName() : "");
    nameField.setEditable(false);

    JTextField typeField = new JTextField(resource.getType() != null ? resource.getType().toString() : "");
    typeField.setEditable(false);

    NumberFormat priceFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
    priceFormat.setMaximumFractionDigits(2);
    priceFormat.setMinimumFractionDigits(0);
    priceField = new JFormattedTextField(priceFormat);
    priceField.setColumns(8);
    priceField.setValue(resource.getUnitPriceHt());

    int row = 0;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Nom"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(nameField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Type"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(typeField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("PU HT (€)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(priceField, gc);

    return form;
  }

  private JPanel buildActions(){
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    JButton cancel = new JButton("Annuler");
    JButton save = new JButton("Enregistrer", IconRegistry.small("success"));
    cancel.addActionListener(e -> dispose());
    save.addActionListener(e -> onSave());
    actions.add(cancel);
    actions.add(save);
    return actions;
  }

  private void onSave(){
    BigDecimal value = parsePrice();
    if (value == null){
      Toasts.error(this, "Montant invalide");
      return;
    }
    BigDecimal previous = resource.getUnitPriceHt();
    resource.setUnitPriceHt(value);
    try {
      Resource saved = ServiceLocator.resources().save(resource);
      if (saved != null){
        applyFrom(saved);
      }
      Toasts.success(this, "Tarif mis à jour");
      dispose();
    } catch (Exception ex){
      resource.setUnitPriceHt(previous);
      Toasts.error(this, "Impossible d'enregistrer le tarif");
    }
  }

  private BigDecimal parsePrice(){
    Object value = priceField.getValue();
    if (value instanceof BigDecimal bd){
      return bd;
    }
    if (value instanceof Number number){
      return BigDecimal.valueOf(number.doubleValue());
    }
    String text = priceField.getText();
    if (text == null){
      return null;
    }
    String normalized = text.trim().replace(" ", "").replace(',', '.');
    if (normalized.isEmpty()){
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException ex){
      return null;
    }
  }

  private void applyFrom(Resource source){
    if (source == null || source == resource){
      return;
    }
    resource.setId(source.getId());
    resource.setName(source.getName());
    resource.setType(source.getType());
    resource.setUnitPriceHt(source.getUnitPriceHt());
    resource.setColor(source.getColor());
    resource.setNotes(source.getNotes());
    resource.setState(source.getState());
    resource.setEmail(source.getEmail());
    resource.setCapacity(source.getCapacity());
    resource.setTags(source.getTags());
    resource.setWeeklyUnavailability(source.getWeeklyUnavailability());
    resource.setUnavailabilities(source.getUnavailabilities());
  }
}
