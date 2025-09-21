package com.materiel.suite.client.ui.auth;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.AuthService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Fenêtre de connexion simple (sélection d'agence + identifiants). */
public class LoginDialog extends JDialog {
  private final JComboBox<Agency> agencyCombo = new JComboBox<>();
  private final JTextField usernameField = new JTextField();
  private final JPasswordField passwordField = new JPasswordField();
  private final JButton connectButton = new JButton("Se connecter", IconRegistry.small("lock"));

  public LoginDialog(Window owner){
    super(owner, "Connexion", ModalityType.APPLICATION_MODAL);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(10, 10));
    add(buildForm(), BorderLayout.CENTER);
    add(buildActions(), BorderLayout.SOUTH);
    connectButton.addActionListener(e -> doLogin());
    getRootPane().setDefaultButton(connectButton);
    setSize(420, 220);
    setMinimumSize(new Dimension(360, 200));
    setLocationRelativeTo(owner);
    loadAgencies();
  }

  private JComponent buildForm(){
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.WEST;
    gc.weightx = 0;
    int row = 0;

    gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Agence"), gc);
    gc.gridx = 1; gc.weightx = 1; panel.add(agencyCombo, gc);
    gc.weightx = 0; row++;

    gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Utilisateur"), gc);
    gc.gridx = 1; gc.weightx = 1; panel.add(usernameField, gc);
    gc.weightx = 0; row++;

    gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Mot de passe"), gc);
    gc.gridx = 1; gc.weightx = 1; panel.add(passwordField, gc);

    return panel;
  }

  private JComponent buildActions(){
    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(connectButton);
    return south;
  }

  private void loadAgencies(){
    try {
      List<Agency> agencies = ServiceLocator.auth() != null ? ServiceLocator.auth().listAgencies() : List.of();
      DefaultComboBoxModel<Agency> model = new DefaultComboBoxModel<>();
      for (Agency agency : agencies){
        model.addElement(agency);
      }
      agencyCombo.setModel(model);
      if (model.getSize() > 0){
        agencyCombo.setSelectedIndex(0);
      }
    } catch (Exception ex){
      Toasts.error(this, "Impossible de récupérer les agences");
    }
  }

  private Agency resolveAgencyAfterLogin(Agency selected){
    Agency current = AuthContext.get() != null ? AuthContext.get().getAgency() : null;
    if (current != null){
      return cloneAgency(current);
    }
    if (selected == null){
      return null;
    }
    Agency copy = cloneAgency(selected);
    if (AuthContext.get() != null){
      AuthContext.get().setAgency(cloneAgency(selected));
    }
    return copy;
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

  private void doLogin(){
    connectButton.setEnabled(false);
    try {
      AuthService authService = ServiceLocator.auth();
      if (authService == null){
        throw new IllegalStateException("Service d'authentification indisponible");
      }
      Agency agency = (Agency) agencyCombo.getSelectedItem();
      String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
      String password = new String(passwordField.getPassword());
      authService.login(agency != null ? agency.getId() : null, username, password);
      AgencyContext.setAgency(resolveAgencyAfterLogin(agency));
      dispose();
    } catch (Exception ex){
      Toasts.error(this, "Connexion refusée");
      connectButton.setEnabled(true);
      passwordField.setText("");
      passwordField.requestFocusInWindow();
    }
  }

  public static void require(Window owner){
    if (!AuthContext.isLogged()){
      new LoginDialog(owner).setVisible(true);
    }
  }
}
