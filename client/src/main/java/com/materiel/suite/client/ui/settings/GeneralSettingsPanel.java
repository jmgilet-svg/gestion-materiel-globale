package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.auth.SessionManager;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.util.Prefs;

import javax.swing.*;
import java.awt.*;

/** Paramètres généraux (durée d'inactivité notamment). */
public class GeneralSettingsPanel extends JPanel {

  public GeneralSettingsPanel(){
    super(new BorderLayout(8, 8));

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.WEST;

    int row = 0;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Durée d'inactivité avant déconnexion (minutes)"), gc);
    int initial = Prefs.getSessionTimeoutMinutes();
    SpinnerNumberModel model = new SpinnerNumberModel(initial, 1, 240, 5);
    JSpinner timeoutSpinner = new JSpinner(model);
    gc.gridx = 1; gc.weightx = 1;
    form.add(timeoutSpinner, gc);

    JButton save = new JButton("Enregistrer");
    boolean canEdit = AccessControl.canEditSettings();
    timeoutSpinner.setEnabled(canEdit);
    save.setEnabled(canEdit);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(save);

    add(form, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    save.addActionListener(e -> {
      int minutes = (int) timeoutSpinner.getValue();
      Prefs.setSessionTimeoutMinutes(minutes);
      SessionManager.setTimeoutMinutes(minutes);
      Toasts.success(this, "Durée de session mise à jour (" + minutes + " min)");
    });
  }
}
