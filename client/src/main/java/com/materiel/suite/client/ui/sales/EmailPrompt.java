package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplatesGateway;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Boîte de dialogue pour saisir les informations d'envoi d'email. */
public class EmailPrompt extends JDialog {
  private final JTextField toField = new JTextField(28);
  private final JTextField ccField = new JTextField(28);
  private final JTextField bccField = new JTextField(28);
  private final JComboBox<TemplatesGateway.Template> templateBox = new JComboBox<>();
  private final JTextField subjectField = new JTextField(28);
  private final JTextArea bodyArea = new JTextArea(6, 28);
  private boolean confirmed;
  private Map<String, String> contextVars = Map.of();

  private EmailPrompt(Window owner, String title, Map<String, String> vars){
    super(owner, title, ModalityType.APPLICATION_MODAL);
    contextVars = vars == null ? Map.of() : vars;
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
    add(new JLabel("Modèle (EMAIL)"), gc);
    gc.gridx = 1;
    gc.anchor = GridBagConstraints.LINE_START;
    add(templateBox, gc);

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

    loadEmailTemplates();
    templateBox.addActionListener(e -> applyTemplateSelected());
    applyTemplateSelected();

    pack();
    setLocationRelativeTo(owner);
  }

  public record Result(List<String> to, List<String> cc, List<String> bcc, String subject, String body){}

  public static Result ask(Component parent, String title){
    return askWithTemplates(parent, title, Map.of());
  }

  public static Result askWithTemplates(Component parent, String title, Map<String, String> vars){
    Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    EmailPrompt dialog = new EmailPrompt(window, title, vars);
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

  private void loadEmailTemplates(){
    try {
      templateBox.removeAllItems();
      List<TemplatesGateway.Template> templates = ServiceLocator.templates().list("EMAIL");
      for (TemplatesGateway.Template template : templates){
        templateBox.addItem(template);
      }
    } catch (Exception ignore){
      // Pas de templates disponibles
    }
  }

  private void applyTemplateSelected(){
    Object selected = templateBox.getSelectedItem();
    if (!(selected instanceof TemplatesGateway.Template template)){
      return;
    }
    String merged = merge(template.content(), contextVars);
    if (merged == null){
      merged = "";
    }
    String[] lines = merged.split("\\R", 2);
    if (lines.length > 0){
      String first = lines[0].trim();
      if (first.regionMatches(true, 0, "subject:", 0, 8)){
        subjectField.setText(first.substring(8).trim());
        bodyArea.setText(lines.length > 1 ? lines[1] : "");
        return;
      }
    }
    bodyArea.setText(merged);
  }

  private static String merge(String template, Map<String, String> vars){
    if (template == null){
      return "";
    }
    String out = template;
    if (vars != null){
      for (Map.Entry<String, String> entry : vars.entrySet()){
        String value = entry.getValue();
        out = out.replace("{{" + entry.getKey() + "}}", value == null ? "" : value);
      }
    }
    return out;
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
