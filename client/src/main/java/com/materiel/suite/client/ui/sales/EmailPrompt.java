package com.materiel.suite.client.ui.sales;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Boîte de dialogue pour saisir les informations d'envoi d'email. */
public class EmailPrompt extends JDialog {
  private final JTextField toField = new JTextField(28);
  private final JTextField ccField = new JTextField(28);
  private final JTextField bccField = new JTextField(28);
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
    add(new JLabel("À (séparés par ,)"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    gc.fill = GridBagConstraints.HORIZONTAL;
    add(toField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("CC"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    add(ccField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("BCC"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    add(bccField, gc);

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

  public record Result(List<String> to, List<String> cc, List<String> bcc, String subject, String body){}

  public static Result ask(Component parent, String title){
    Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    EmailPrompt dialog = new EmailPrompt(window, title);
    dialog.setVisible(true);
    if (!dialog.confirmed){
      return null;
    }
    return new Result(split(dialog.toField.getText()),
        split(dialog.ccField.getText()),
        split(dialog.bccField.getText()),
        dialog.subjectField.getText().trim(),
        dialog.bodyArea.getText());
  }

  private static List<String> split(String value){
    List<String> out = new ArrayList<>();
    if (value == null){
      return out;
    }
    for (String part : value.split(",")){
      String trimmed = part.trim();
      if (!trimmed.isEmpty()){
        out.add(trimmed);
      }
    }
    return out;
  }
}
