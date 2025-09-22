package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.service.AgencyConfigGateway;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplatesGateway;
import com.materiel.suite.client.ui.common.HtmlValidation;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Boîte de dialogue pour saisir les informations d'envoi d'email. */
public class EmailPrompt extends JDialog {
  private final JTextField toField = new JTextField(28);
  private final JTextField ccField = new JTextField(28);
  private final JTextField bccField = new JTextField(28);
  private final JComboBox<TemplatesGateway.Template> templateBox = new JComboBox<>();
  private final JTextField subjectField = new JTextField(28);
  private final JTextArea bodyArea = new JTextArea(6, 28);
  private final JEditorPane preview = new JEditorPane("text/html", "");
  private final JCheckBox livePreview = new JCheckBox("Prévisualiser le rendu HTML");
  private final JCheckBox mobileToggle = new JCheckBox("Aperçu mobile (375px)");
  private final JPanel previewViewport = new JPanel(new BorderLayout());
  private final JLabel validationLabel = new JLabel(" ");
  private final JButton validateBtn = new JButton("Valider HTML");
  private String agencyCss = "";
  private String agencySignature = "";
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
    preview.setEditable(false);
    preview.setContentType("text/html; charset=UTF-8");
    previewViewport.add(preview, BorderLayout.CENTER);
    JScrollPane right = new JScrollPane(previewViewport);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        new JScrollPane(bodyArea), right);
    split.setResizeWeight(0.5);
    add(split, gc);

    gc.gridx = 1;
    gc.gridy++;
    gc.anchor = GridBagConstraints.LINE_START;
    gc.fill = GridBagConstraints.NONE;
    JPanel previewOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    previewOptions.add(livePreview);
    previewOptions.add(mobileToggle);
    previewOptions.add(validateBtn);
    validationLabel.setForeground(new java.awt.Color(0x33, 0x66, 0x33));
    previewOptions.add(validationLabel);
    add(previewOptions, gc);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton send = new JButton("Envoyer");
    JButton cancel = new JButton("Annuler");
    buttons.add(send);
    buttons.add(cancel);
    gc.gridx = 1;
    gc.gridy++;
    gc.anchor = GridBagConstraints.LINE_END;
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
    setResizable(true);

    loadEmailTemplates();
    templateBox.addActionListener(e -> applyTemplateSelected());

    AgencyConfigGateway config = ServiceLocator.agencyConfig();
    if (config != null){
      try {
        AgencyConfigGateway.AgencyConfig cfg = config.get();
        agencyCss = cfg == null || cfg.emailCss() == null ? "" : cfg.emailCss();
        agencySignature = cfg == null || cfg.emailSignatureHtml() == null ? "" : cfg.emailSignatureHtml();
      } catch (Exception ignore){
        agencyCss = "";
        agencySignature = "";
      }
    }

    livePreview.setSelected(true);
    livePreview.addActionListener(e -> refreshPreview());
    mobileToggle.addActionListener(e -> refreshPreview());
    validateBtn.addActionListener(e -> runValidation());
    bodyArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e){ refreshPreview(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e){ refreshPreview(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e){ refreshPreview(); }
    });

    applyTemplateSelected();
    refreshPreview();

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
        dialog.buildFinalHtml());
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
      refreshPreview();
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
        refreshPreview();
        return;
      }
    }
    bodyArea.setText(merged);
    refreshPreview();
  }

  private void refreshPreview(){
    if (!livePreview.isSelected()){
      preview.setText("");
      previewViewport.setPreferredSize(null);
      previewViewport.revalidate();
      previewViewport.repaint();
      return;
    }
    String headCss = agencyCss == null || agencyCss.isBlank() ? "" : "<style>" + agencyCss + "</style>";
    String body = bodyArea.getText();
    if (body == null){
      body = "";
    }
    if (agencySignature != null && !agencySignature.isBlank()){
      body = body + "\n" + agencySignature;
    }
    String html = "<!doctype html><html><head><meta charset='UTF-8'>"
        + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        + headCss + "</head><body>" + body + "</body></html>";
    preview.setText(html);
    preview.setCaretPosition(0);
    if (mobileToggle.isSelected()){
      previewViewport.setPreferredSize(new Dimension(375, 600));
    } else {
      previewViewport.setPreferredSize(null);
    }
    previewViewport.revalidate();
    previewViewport.repaint();
  }

  private String buildFinalHtml(){
    String html = bodyArea.getText();
    if (html == null){
      return "";
    }
    String trimmed = html.trim();
    if (trimmed.isEmpty()){
      return "";
    }
    if (!trimmed.toLowerCase(Locale.ROOT).contains("<html")){
      String body = trimmed;
      if (shouldAppendSignature(body)){
        body = body + System.lineSeparator() + agencySignature;
      }
      String headCss = agencyCss == null || agencyCss.isBlank() ? "" : "<style>" + agencyCss + "</style>";
      return "<!doctype html><html><head><meta charset='UTF-8'>"
          + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
          + headCss + "</head><body>" + body + "</body></html>";
    }
    return trimmed;
  }

  private boolean shouldAppendSignature(String content){
    if (agencySignature == null || agencySignature.isBlank()){
      return false;
    }
    String normalizedSignature = agencySignature.replaceAll("\\s+", "");
    if (normalizedSignature.isEmpty()){
      return false;
    }
    String normalizedContent = content == null ? "" : content.replaceAll("\\s+", "");
    return !normalizedContent.contains(normalizedSignature);
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

  private void runValidation(){
    HtmlValidation.Result result = HtmlValidation.validate(bodyArea.getText());
    if (result.ok){
      validationLabel.setForeground(new java.awt.Color(0x2e, 0x7d, 0x32));
      validationLabel.setText("HTML valide");
      validationLabel.setToolTipText(null);
    } else {
      validationLabel.setForeground(new java.awt.Color(0xc6, 0x28, 0x28));
      validationLabel.setText("⚠ " + result.message);
      String details = result.details == null ? "" : result.details.replace("\n", "<br/>");
      validationLabel.setToolTipText(details.isEmpty() ? null : "<html>" + details + "</html>");
    }
  }
}
