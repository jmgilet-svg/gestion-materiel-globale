package com.materiel.suite.client.ui.sales;

import javax.swing.*;
import java.awt.*;

/** Boîte de dialogue minimale pour saisir les informations d'envoi d'email. */
public class EmailPrompt extends JDialog {
  private final JTextField toField = new JTextField(28);
  private final JTextField subjectField = new JTextField(28);
  private final JTextArea bodyArea = new JTextArea(6, 28);
  private boolean confirmed;

  private EmailPrompt(Window owner, String title){
    super(owner, title, ModalityType.APPLICATION_MODAL);
    setLayout(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 6, 6, 6);
    gc.gridx = 0;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("À"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    gc.fill = GridBagConstraints.HORIZONTAL;
    add(toField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("Sujet"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    add(subjectField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Message"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    gc.fill = GridBagConstraints.BOTH;
    JScrollPane scroll = new JScrollPane(bodyArea);
    add(scroll, gc);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton send = new JButton("Envoyer");
    JButton cancel = new JButton("Annuler");
    buttons.add(send);
    buttons.add(cancel);
    gc.gridx = 1;
    gc.gridy++;
    gc.anchor = GridBagConstraints.LINE_END;
    gc.fill = GridBagConstraints.NONE;
    add(buttons, gc);

    send.addActionListener(e -> {
      confirmed = true;
      dispose();
    });
    cancel.addActionListener(e -> {
      confirmed = false;
      dispose();
    });
    getRootPane().setDefaultButton(send);
    setResizable(false);
    pack();
    setLocationRelativeTo(owner);
  }

  public record Result(String to, String subject, String body){}

  public static Result ask(Component parent, String title){
    Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    EmailPrompt dialog = new EmailPrompt(window, title);
    dialog.setVisible(true);
    if (!dialog.confirmed){
      return null;
    }
    return new Result(dialog.toField.getText().trim(), dialog.subjectField.getText().trim(), dialog.bodyArea.getText());
  }
}
