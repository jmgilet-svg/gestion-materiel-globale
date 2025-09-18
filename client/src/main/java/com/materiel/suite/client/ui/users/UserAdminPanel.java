package com.materiel.suite.client.ui.users;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.Role;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.users.UserAccount;
import com.materiel.suite.client.users.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Interface d'administration des comptes utilisateurs. */
public class UserAdminPanel extends JPanel {
  private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Utilisateur", "Nom affiché", "Rôle", "Agence", "__ID"}, 0){
    @Override public boolean isCellEditable(int row, int column){
      return false;
    }
  };
  private final JTable table = new JTable(model);
  private final JButton addButton = new JButton("Ajouter", IconRegistry.small("plus"));
  private final JButton editButton = new JButton("Modifier", IconRegistry.small("edit"));
  private final JButton deleteButton = new JButton("Supprimer", IconRegistry.small("trash"));
  private final JButton passwordButton = new JButton("Définir mot de passe…", IconRegistry.small("lock"));
  private final JButton refreshButton = new JButton("Recharger", IconRegistry.small("refresh"));
  private List<UserAccount> cache = new ArrayList<>();

  public UserAdminPanel(){
    super(new BorderLayout(8, 8));
    table.setRowHeight(24);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    hideIdColumn();
    add(new JScrollPane(table), BorderLayout.CENTER);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(addButton);
    toolbar.add(editButton);
    toolbar.add(deleteButton);
    toolbar.addSeparator();
    toolbar.add(passwordButton);
    toolbar.addSeparator();
    toolbar.add(refreshButton);
    add(toolbar, BorderLayout.NORTH);

    addButton.addActionListener(e -> openEditor(null));
    editButton.addActionListener(e -> {
      UserAccount current = current();
      if (current != null){
        openEditor(current);
      }
    });
    deleteButton.addActionListener(e -> deleteCurrent());
    passwordButton.addActionListener(e -> {
      UserAccount current = current();
      if (current != null){
        new SetPasswordDialog(SwingUtilities.getWindowAncestor(this), current, this::reload).setVisible(true);
      }
    });
    refreshButton.addActionListener(e -> reload());

    table.getSelectionModel().addListSelectionListener(e -> updateActions());
    updateActions();

    reload();
  }

  private void hideIdColumn(){
    var column = table.getColumnModel().getColumn(4);
    column.setMinWidth(0);
    column.setMaxWidth(0);
    column.setPreferredWidth(0);
  }

  private void updateActions(){
    boolean hasSelection = table.getSelectedRow() >= 0;
    editButton.setEnabled(hasSelection);
    deleteButton.setEnabled(hasSelection);
    passwordButton.setEnabled(hasSelection);
  }

  private UserAccount current(){
    int viewRow = table.getSelectedRow();
    if (viewRow < 0){
      return null;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    String id = Objects.toString(model.getValueAt(modelRow, 4), null);
    return cache.stream().filter(user -> Objects.equals(user.getId(), id)).findFirst().orElse(null);
  }

  private void reload(){
    UserService svc = ServiceLocator.users();
    if (svc == null){
      Toasts.error(this, "Service utilisateur indisponible");
      return;
    }
    try {
      cache = svc.list();
      model.setRowCount(0);
      for (UserAccount account : cache){
        model.addRow(new Object[]{
            account.getUsername(),
            account.getDisplayName(),
            account.getRole(),
            account.getAgency() != null ? account.getAgency().getName() : "",
            account.getId()
        });
      }
      updateActions();
    } catch (Exception ex){
      Toasts.error(this, "Impossible de charger les comptes");
    }
  }

  private void deleteCurrent(){
    UserAccount current = current();
    if (current == null){
      return;
    }
    int confirm = JOptionPane.showConfirmDialog(this,
        "Supprimer l'utilisateur " + current.getUsername() + " ?",
        "Confirmation",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);
    if (confirm != JOptionPane.YES_OPTION){
      return;
    }
    try {
      ServiceLocator.users().delete(current.getId());
      Toasts.success(this, "Compte supprimé");
      reload();
    } catch (Exception ex){
      Toasts.error(this, "Impossible de supprimer le compte");
    }
  }

  private void openEditor(UserAccount initial){
    new UserEditorDialog(SwingUtilities.getWindowAncestor(this), initial, this::reload).setVisible(true);
  }

  private static Agency cloneAgency(Agency source){
    if (source == null){
      return null;
    }
    Agency copy = new Agency();
    copy.setId(source.getId());
    copy.setName(source.getName());
    return copy;
  }

  private static class UserEditorDialog extends JDialog {
    private final JTextField usernameField = new JTextField();
    private final JTextField displayNameField = new JTextField();
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
    private final JComboBox<Agency> agencyCombo = new JComboBox<>();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton saveButton = new JButton("Enregistrer", IconRegistry.small("success"));
    private final UserAccount workingCopy;

    UserEditorDialog(Window owner, UserAccount initial, Runnable onSaved){
      super(owner, initial == null ? "Créer un utilisateur" : "Modifier un utilisateur", ModalityType.APPLICATION_MODAL);
      setLayout(new BorderLayout(8, 8));
      workingCopy = initial != null ? copyOf(initial) : new UserAccount();

      DefaultComboBoxModel<Agency> agenciesModel = new DefaultComboBoxModel<>();
      try {
        List<Agency> agencies = ServiceLocator.auth() != null ? ServiceLocator.auth().listAgencies() : List.of();
        for (Agency agency : agencies){
          agenciesModel.addElement(cloneAgency(agency));
        }
      } catch (Exception ex){
        // ignore, combo vide
      }
      agencyCombo.setModel(agenciesModel);

      JPanel form = new JPanel(new GridBagLayout());
      GridBagConstraints gc = new GridBagConstraints();
      gc.insets = new Insets(6, 6, 6, 6);
      gc.anchor = GridBagConstraints.WEST;
      gc.fill = GridBagConstraints.HORIZONTAL;
      gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
      form.add(new JLabel("Utilisateur"), gc);
      gc.gridx = 1; gc.weightx = 1;
      form.add(usernameField, gc);

      gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
      form.add(new JLabel("Nom affiché"), gc);
      gc.gridx = 1; gc.weightx = 1;
      form.add(displayNameField, gc);

      gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
      form.add(new JLabel("Rôle"), gc);
      gc.gridx = 1; gc.weightx = 1;
      form.add(roleCombo, gc);

      gc.gridx = 0; gc.gridy = 3; gc.weightx = 0;
      form.add(new JLabel("Agence"), gc);
      gc.gridx = 1; gc.weightx = 1;
      form.add(agencyCombo, gc);

      if (initial == null){
        gc.gridx = 0; gc.gridy = 4; gc.weightx = 0;
        form.add(new JLabel("Mot de passe"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(passwordField, gc);
      }

      add(form, BorderLayout.CENTER);

      JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      actions.add(saveButton);
      add(actions, BorderLayout.SOUTH);

      if (initial != null){
        usernameField.setText(initial.getUsername());
        usernameField.setEditable(false);
        displayNameField.setText(initial.getDisplayName());
        roleCombo.setSelectedItem(initial.getRole());
        selectAgency(initial.getAgency());
      }

      saveButton.addActionListener(e -> {
        if (usernameField.getText() == null || usernameField.getText().isBlank()){
          Toasts.error(this, "Identifiant requis");
          return;
        }
        workingCopy.setUsername(usernameField.getText().trim());
        workingCopy.setDisplayName(displayNameField.getText());
        workingCopy.setRole((Role) roleCombo.getSelectedItem());
        workingCopy.setAgency((Agency) agencyCombo.getSelectedItem());
        try {
          UserService svc = ServiceLocator.users();
          if (svc == null){
            throw new IllegalStateException("Service indisponible");
          }
          if (initial == null){
            String pwd = new String(passwordField.getPassword());
            if (pwd.isBlank()){
              Toasts.error(this, "Mot de passe requis");
              return;
            }
            svc.create(workingCopy, pwd);
          } else {
            svc.update(workingCopy);
          }
          Toasts.success(this, "Compte enregistré");
          dispose();
          if (onSaved != null){
            onSaved.run();
          }
        } catch (Exception ex){
          Toasts.error(this, "Impossible d'enregistrer le compte");
        }
      });

      getRootPane().setDefaultButton(saveButton);
      setSize(520, 280);
      setLocationRelativeTo(owner);
    }

    private void selectAgency(Agency agency){
      if (agency == null){
        return;
      }
      for (int i = 0; i < agencyCombo.getItemCount(); i++){
        Agency item = agencyCombo.getItemAt(i);
        if (item != null && Objects.equals(item.getId(), agency.getId())){
          agencyCombo.setSelectedIndex(i);
          return;
        }
      }
    }

    private UserAccount copyOf(UserAccount source){
      UserAccount copy = new UserAccount();
      copy.setId(source.getId());
      copy.setUsername(source.getUsername());
      copy.setDisplayName(source.getDisplayName());
      copy.setRole(source.getRole());
      copy.setAgency(cloneAgency(source.getAgency()));
      return copy;
    }
  }

  private static class SetPasswordDialog extends JDialog {
    SetPasswordDialog(Window owner, UserAccount user, Runnable onSaved){
      super(owner, "Définir mot de passe — " + user.getUsername(), ModalityType.APPLICATION_MODAL);
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
          UserService svc = ServiceLocator.users();
          if (svc == null){
            throw new IllegalStateException("Service indisponible");
          }
          svc.updatePassword(user.getId(), first);
          Toasts.success(this, "Mot de passe mis à jour");
          dispose();
          if (onSaved != null){
            onSaved.run();
          }
        } catch (Exception ex){
          Toasts.error(this, "Impossible de modifier le mot de passe");
        }
      });

      getRootPane().setDefaultButton(save);
      setSize(420, 200);
      setLocationRelativeTo(owner);
    }
  }
}
