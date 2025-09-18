package com.materiel.suite.client.ui.users;

import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.User;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;

/** Boîte de dialogue permettant à l'utilisateur courant de modifier son mot de passe. */
public class ChangePasswordDialog extends JDialog {
  public ChangePasswordDialog(Window owner){
    super(owner, "Changer mon mot de passe", ModalityType.APPLICATION_MODAL);
    User current = AuthContext.get();
    if (current == null){
      dispose();
      return;
    }
    String username = current.getUsername() != null ? current.getUsername() : current.getDisplayName();
    if (username != null && !username.isBlank()){
      setTitle("Changer mon mot de passe — " + username);
    }
    setLayout(new BorderLayout(8, 8));

    JPasswordField password = new JPasswordField();
    JPasswordField confirm = new JPasswordField();
    JButton save = new JButton("Enregistrer", IconRegistry.small("success"));

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 6, 6, 6);
    gc.anchor = GridBagConstraints.WEST;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
    form.add(new JLabel("Nouveau mot de passe"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(password, gc);
    gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
    form.add(new JLabel("Confirmer"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(confirm, gc);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(save);

    add(form, BorderLayout.CENTER);
    add(actions, BorderLayout.SOUTH);

    save.addActionListener(e -> {
      String first = new String(password.getPassword());
      String second = new String(confirm.getPassword());
      if (first == null || first.isBlank() || !first.equals(second)){
        Toasts.error(this, "Mot de passe invalide");
        return;
      }
      try {
        User fresh = AuthContext.get();
        if (fresh == null || ServiceLocator.users() == null){
          throw new IllegalStateException("Utilisateur absent");
        }
        ServiceLocator.users().updatePassword(fresh.getId(), first);
        Toasts.success(this, "Mot de passe mis à jour");
        dispose();
      } catch (Exception ex){
        Toasts.error(this, "Impossible de mettre à jour le mot de passe");
      }
    });

    getRootPane().setDefaultButton(save);
    setSize(420, 180);
    setLocationRelativeTo(owner);
  }
}
