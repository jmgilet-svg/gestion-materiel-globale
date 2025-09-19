package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.auth.SessionManager;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.GeneralSettings;
import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.*;
import java.awt.*;

/** Paramètres généraux (durée d'inactivité, autosave, etc.). */
public class GeneralSettingsPanel extends JPanel {
  private final JSpinner timeoutSpinner;
  private final JSpinner autosaveSpinner;

  public GeneralSettingsPanel(){
    super(new BorderLayout(8, 8));

    GeneralSettings settings = loadSettings();

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.WEST;

    int row = 0;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Durée d'inactivité avant déconnexion (minutes)"), gc);
    SpinnerNumberModel timeoutModel = new SpinnerNumberModel(settings.getSessionTimeoutMinutes(), 1, 240, 5);
    timeoutSpinner = new JSpinner(timeoutModel);
    gc.gridx = 1; gc.weightx = 1;
    form.add(timeoutSpinner, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Intervalle d'enregistrement automatique (secondes)"), gc);
    SpinnerNumberModel autosaveModel = new SpinnerNumberModel(Math.max(5, settings.getAutosaveIntervalSeconds()), 5, 600, 5);
    autosaveSpinner = new JSpinner(autosaveModel);
    gc.gridx = 1; gc.weightx = 1;
    form.add(autosaveSpinner, gc);

    JButton save = new JButton("Enregistrer");
    boolean canEdit = AccessControl.canEditSettings();
    timeoutSpinner.setEnabled(canEdit);
    autosaveSpinner.setEnabled(canEdit);
    save.setEnabled(canEdit);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(save);

    add(form, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    save.addActionListener(e -> saveSettings());
  }

  private GeneralSettings loadSettings(){
    try {
      GeneralSettings settings = ServiceLocator.settings().getGeneral();
      if (settings != null){
        return settings;
      }
    } catch (RuntimeException ignore){
    }
    return new GeneralSettings();
  }

  private void saveSettings(){
    int minutes = ((Number) timeoutSpinner.getValue()).intValue();
    int autosave = ((Number) autosaveSpinner.getValue()).intValue();

    GeneralSettings updated = new GeneralSettings();
    updated.setSessionTimeoutMinutes(minutes);
    updated.setAutosaveIntervalSeconds(autosave);

    try {
      ServiceLocator.settings().saveGeneral(updated);
      SessionManager.setTimeoutMinutes(updated.getSessionTimeoutMinutes());
      timeoutSpinner.setValue(updated.getSessionTimeoutMinutes());
      autosaveSpinner.setValue(updated.getAutosaveIntervalSeconds());
      Toasts.success(this, "Paramètres généraux mis à jour");
    } catch (RuntimeException ex){
      Toasts.error(this, "Échec de l'enregistrement des paramètres");
    }
  }
}
