package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.service.AgencyConfigGateway;
import com.materiel.suite.client.service.PdfService;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplatesGateway;
import com.materiel.suite.client.ui.common.HtmlEditorPanel;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.common.VariablePalettePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panneau d'administration des modèles HTML (devis, factures, emails).
 */
public class TemplatesSettingsPanel extends JPanel {
  private final JComboBox<String> type = new JComboBox<>(new String[]{"QUOTE", "INVOICE", "EMAIL", "PARTIAL"});
  private final DefaultListModel<TemplatesGateway.Template> listModel = new DefaultListModel<>();
  private final JList<TemplatesGateway.Template> list = new JList<>(listModel);
  private final JTextField key = new JTextField(16);
  private final JTextField name = new JTextField(24);
  private final HtmlEditorPanel content = new HtmlEditorPanel();
  private final VariablePalettePanel vars = new VariablePalettePanel("Variables disponibles");
  private final JTextArea emailCss = new JTextArea(6, 50);
  private final HtmlEditorPanel emailSignature = new HtmlEditorPanel();
  private final JButton saveStyles = new JButton("Enregistrer styles agence");
  private final JButton newBtn = new JButton("Nouveau");
  private final JButton saveBtn = new JButton("Enregistrer");
  private final JButton deleteBtn = new JButton("Supprimer");
  private final JButton reloadBtn = new JButton("Recharger");
  private final JButton previewBtn = new JButton("Aperçu HTML");
  private final JButton pdfBtn = new JButton("Exporter PDF");

  public TemplatesSettingsPanel(){
    super(new BorderLayout(8, 8));
    setBorder(new EmptyBorder(8, 8, 8, 8));

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(new ListSelectionListener(){
      @Override public void valueChanged(ListSelectionEvent e){
        if (!e.getValueIsAdjusting()){
          onSelect();
        }
      }
    });

    JScrollPane listScroll = new JScrollPane(list);
    listScroll.setPreferredSize(new Dimension(240, 320));
    JPanel listPanel = new JPanel(new BorderLayout(4, 4));
    listPanel.add(new JLabel("Modèles"), BorderLayout.NORTH);
    listPanel.add(listScroll, BorderLayout.CENTER);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0;
    gc.gridy = 0;

    form.add(new JLabel("Type"), gc);
    gc.gridx = 1;
    form.add(type, gc);

    gc.gridx = 0;
    gc.gridy++;
    form.add(new JLabel("Clé"), gc);
    gc.gridx = 1;
    form.add(key, gc);

    gc.gridx = 0;
    gc.gridy++;
    form.add(new JLabel("Nom"), gc);
    gc.gridx = 1;
    form.add(name, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.gridwidth = 2;
    gc.fill = GridBagConstraints.BOTH;
    gc.weightx = 1.0;
    gc.weighty = 1.0;

    emailCss.setLineWrap(true);
    emailCss.setWrapStyleWord(true);
    emailSignature.setPreferredSize(new Dimension(420, 260));

    JPanel editorPanel = new JPanel(new BorderLayout(6, 6));
    editorPanel.add(content, BorderLayout.CENTER);
    vars.setPreferredSize(new Dimension(220, 360));
    editorPanel.add(vars, BorderLayout.EAST);
    form.add(editorPanel, gc);

    gc.gridy++;
    gc.gridx = 0;
    gc.gridwidth = 2;
    gc.weighty = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    form.add(new JSeparator(), gc);

    gc.gridy++;
    gc.gridwidth = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;
    form.add(new JLabel("CSS des emails (agence)"), gc);
    gc.gridx = 1;
    gc.weightx = 1.0;
    form.add(new JScrollPane(emailCss), gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.weightx = 0;
    form.add(new JLabel("Signature HTML (agence)"), gc);
    gc.gridx = 1;
    gc.fill = GridBagConstraints.BOTH;
    gc.weighty = 0.5;
    form.add(emailSignature, gc);

    gc.gridy++;
    gc.gridx = 1;
    gc.weighty = 0;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.LINE_END;
    form.add(saveStyles, gc);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    actions.add(newBtn);
    actions.add(saveBtn);
    actions.add(deleteBtn);
    actions.add(reloadBtn);
    actions.add(previewBtn);
    actions.add(pdfBtn);

    add(listPanel, BorderLayout.WEST);
    add(form, BorderLayout.CENTER);
    add(actions, BorderLayout.SOUTH);

    vars.setInserter(this::insertVar);

    newBtn.addActionListener(e -> onNew());
    saveBtn.addActionListener(e -> onSave());
    deleteBtn.addActionListener(e -> onDelete());
    reloadBtn.addActionListener(e -> reload(null));
    previewBtn.addActionListener(e -> onPreview());
    pdfBtn.addActionListener(e -> onPdf());
    type.addActionListener(e -> {
      reload(null);
      refreshVars();
    });
    saveStyles.addActionListener(e -> onSaveStyles());

    refreshVars();
    reload(null);
    updateAccess();
    loadAgencyStyles();
  }

  private void onNew(){
    list.clearSelection();
    clearForm();
  }

  private void onSelect(){
    TemplatesGateway.Template template = list.getSelectedValue();
    if (template == null){
      return;
    }
    if (template.type() != null && !template.type().equals(type.getSelectedItem())){
      type.setSelectedItem(template.type());
    }
    key.setText(template.key() == null ? "" : template.key());
    name.setText(template.name() == null ? "" : template.name());
    content.setHtml(template.content());
  }

  private void onSave(){
    if (!AccessControl.canEditSettings()){
      Toasts.error(this, "Vous n'avez pas les droits pour modifier les modèles.");
      return;
    }
    try {
      TemplatesGateway.Template selected = list.getSelectedValue();
      String id = selected == null ? null : selected.id();
      String agency = selected == null ? ServiceLocator.agencyId() : selected.agencyId();
      String selectedType = (String) type.getSelectedItem();
      TemplatesGateway.Template template = new TemplatesGateway.Template(
          id,
          agency,
          selectedType,
          key.getText() == null ? null : key.getText().trim(),
          name.getText() == null ? null : name.getText().trim(),
          content.getHtml()
      );
      TemplatesGateway.Template saved = ServiceLocator.templates().save(template);
      Toasts.success(this, "Modèle enregistré");
      reload(saved == null ? null : saved.id());
    } catch (Exception ex){
      Toasts.error(this, "Enregistrement: " + ex.getMessage());
    }
  }

  private void onDelete(){
    TemplatesGateway.Template template = list.getSelectedValue();
    if (template == null){
      Toasts.info(this, "Sélectionnez un modèle.");
      return;
    }
    if (!AccessControl.canEditSettings()){
      Toasts.error(this, "Vous n'avez pas les droits pour supprimer un modèle.");
      return;
    }
    int confirm = JOptionPane.showConfirmDialog(this,
        "Supprimer le modèle \"" + template.toString() + "\" ?",
        "Confirmation",
        JOptionPane.YES_NO_OPTION);
    if (confirm != JOptionPane.YES_OPTION){
      return;
    }
    try {
      ServiceLocator.templates().delete(template);
      Toasts.success(this, "Modèle supprimé");
      reload(null);
    } catch (Exception ex){
      Toasts.error(this, "Suppression: " + ex.getMessage());
    }
  }

  private void onPreview(){
    try {
      String html = content.getHtml();
      JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Aperçu HTML", Dialog.ModalityType.MODELESS);
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      JEditorPane preview = new JEditorPane();
      preview.setEditable(false);
      preview.setContentType("text/html; charset=UTF-8");
      preview.setText(html == null ? "" : html);
      preview.setCaretPosition(0);
      dialog.add(new JScrollPane(preview), BorderLayout.CENTER);
      dialog.setSize(760, 520);
      dialog.setLocationRelativeTo(this);
      dialog.setVisible(true);
    } catch (Exception ex){
      Toasts.error(this, "Aperçu: " + ex.getMessage());
    }
  }

  private void onPdf(){
    PdfService pdf = ServiceLocator.pdf();
    if (pdf == null){
      Toasts.error(this, "Service PDF indisponible.");
      return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("template.pdf"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
      return;
    }
    try {
      byte[] bytes = pdf.render(content.getHtml(), Map.of(), null);
      Files.write(chooser.getSelectedFile().toPath(), bytes);
      Toasts.success(this, "PDF exporté : " + chooser.getSelectedFile().getName());
    } catch (Exception ex){
      Toasts.error(this, "Export PDF : " + ex.getMessage());
    }
  }

  private void insertVar(String variable){
    if (variable == null || variable.isBlank()){
      return;
    }
    content.insertText(variable);
  }

  private void refreshVars(){
    String selectedType = (String) type.getSelectedItem();
    List<String> varsList = new ArrayList<>();
    varsList.add("agency.name");
    varsList.add("agency.addressHtml");
    varsList.add("agency.vatRate");
    varsList.add("agency.cgvHtml");
    varsList.add("agency.emailCss");
    varsList.add("agency.emailSignatureHtml");
    varsList.add("client.name");
    varsList.add("client.addressHtml");
    varsList.add("logo.cdi");
    varsList.add("lines.rows");
    varsList.add("lines.tableHtml");
    varsList.add("tax.rate");
    varsList.add("amount.netToPay");
    varsList.add(">partial:cgv");
    varsList.add("asset:mon-image");
    varsList.add("qr:https://votre-lien");
    if ("QUOTE".equalsIgnoreCase(selectedType) || "EMAIL".equalsIgnoreCase(selectedType)){
      varsList.addAll(List.of("quote.reference", "quote.date", "quote.totalHt", "quote.totalTtc"));
    }
    if ("INVOICE".equalsIgnoreCase(selectedType) || "EMAIL".equalsIgnoreCase(selectedType)){
      varsList.addAll(List.of("invoice.number", "invoice.date", "invoice.totalHt", "invoice.totalTtc", "invoice.status"));
    }
    vars.setVariables(varsList);
  }

  private void reload(String selectId){
    TemplatesGateway gateway = ServiceLocator.templates();
    listModel.clear();
    String selectedType = (String) type.getSelectedItem();
    List<TemplatesGateway.Template> templates = gateway.list(selectedType);
    for (TemplatesGateway.Template template : templates){
      listModel.addElement(template);
    }
    if (selectId != null){
      for (int i = 0; i < listModel.size(); i++){
        TemplatesGateway.Template template = listModel.get(i);
        if (template != null && selectId.equals(template.id())){
          list.setSelectedIndex(i);
          return;
        }
      }
    }
    if (!listModel.isEmpty()){
      list.setSelectedIndex(0);
    } else {
      list.clearSelection();
      clearForm();
    }
  }

  private void clearForm(){
    key.setText(defaultKey());
    name.setText(defaultName());
    content.setHtml(sampleForType());
  }

  private void updateAccess(){
    boolean canEdit = AccessControl.canEditSettings();
    newBtn.setEnabled(canEdit);
    saveBtn.setEnabled(canEdit);
    deleteBtn.setEnabled(canEdit);
    key.setEditable(canEdit);
    name.setEditable(canEdit);
    content.setEditable(canEdit);
    vars.setEnabled(canEdit);
    emailCss.setEditable(canEdit);
    emailSignature.setEditable(canEdit);
    saveStyles.setEnabled(canEdit);
  }

  private void loadAgencyStyles(){
    AgencyConfigGateway gateway = ServiceLocator.agencyConfig();
    if (gateway == null){
      emailCss.setText("");
      emailSignature.setHtml("<p></p>");
      saveStyles.setEnabled(false);
      return;
    }
    try {
      AgencyConfigGateway.AgencyConfig cfg = gateway.get();
      String css = cfg == null || cfg.emailCss() == null ? "" : cfg.emailCss();
      String signature = cfg == null || cfg.emailSignatureHtml() == null ? "<p></p>" : cfg.emailSignatureHtml();
      emailCss.setText(css);
      emailSignature.setHtml(signature);
    } catch (Exception ex){
      emailCss.setText("");
      emailSignature.setHtml("<p></p>");
    }
  }

  private void onSaveStyles(){
    if (!AccessControl.canEditSettings()){
      Toasts.error(this, "Vous n'avez pas les droits pour modifier les styles d'agence.");
      return;
    }
    AgencyConfigGateway gateway = ServiceLocator.agencyConfig();
    if (gateway == null){
      Toasts.error(this, "Service configuration indisponible.");
      return;
    }
    try {
      AgencyConfigGateway.AgencyConfig current = gateway.get();
      AgencyConfigGateway.AgencyConfig payload = new AgencyConfigGateway.AgencyConfig(
          current == null ? null : current.companyName(),
          current == null ? null : current.companyAddressHtml(),
          current == null ? null : current.vatRate(),
          current == null ? null : current.cgvHtml(),
          emailCss.getText(),
          emailSignature.getHtml()
      );
      AgencyConfigGateway.AgencyConfig saved = gateway.save(payload);
      if (saved != null){
        emailCss.setText(saved.emailCss() == null ? "" : saved.emailCss());
        emailSignature.setHtml(saved.emailSignatureHtml() == null ? "<p></p>" : saved.emailSignatureHtml());
      }
      Toasts.success(this, "Styles d'agence sauvegardés");
    } catch (Exception ex){
      Toasts.error(this, "Erreur styles: " + ex.getMessage());
    }
  }

  private String defaultKey(){
    String selectedType = (String) type.getSelectedItem();
    if (selectedType == null){
      return "default";
    }
    return switch (selectedType.toUpperCase(Locale.ROOT)) {
      case "EMAIL" -> "email";
      case "PARTIAL" -> "partial";
      default -> "default";
    };
  }

  private String defaultName(){
    String selectedType = (String) type.getSelectedItem();
    if (selectedType == null){
      return "Modèle";
    }
    return switch (selectedType.toUpperCase(Locale.ROOT)) {
      case "QUOTE" -> "Modèle devis";
      case "INVOICE" -> "Modèle facture";
      case "EMAIL" -> "Modèle email";
      case "PARTIAL" -> "Bloc partiel";
      default -> "Modèle";
    };
  }

  private String sampleForType(){
    String selectedType = (String) type.getSelectedItem();
    if ("INVOICE".equalsIgnoreCase(selectedType)){
      return """
<!DOCTYPE html><html><body>
<h1>Facture {{invoice.number}}</h1>
<p>Client : {{client.name}}</p>
<table style=\"width:100%;border-collapse:collapse\">
  <thead><tr><th>Désignation</th><th>Qté</th><th>PU HT</th><th>Total HT</th></tr></thead>
  <tbody>
    {{lines.rows}}
  </tbody>
</table>
<p>Total TTC : {{invoice.totalTtc}} €</p>
</body></html>
""";
    }
    if ("EMAIL".equalsIgnoreCase(selectedType)){
      return """
<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>{{agency.emailCss}}</style></head><body>
<p>Bonjour {{client.name}},</p>
<p>Veuillez trouver ci-joint votre document.</p>
{{lines.tableHtml}}
{{agency.emailSignatureHtml}}
</body></html>
""";
    }
    if ("PARTIAL".equalsIgnoreCase(selectedType)){
      return """
<div style=\"font-size:12px;color:#666\">Vos conditions générales ici.</div>
""";
    }
    return """
<!DOCTYPE html><html><body>
<h1>Devis {{quote.reference}}</h1>
<p>Agence : {{agency.name}}</p>
<table style=\"width:100%;border-collapse:collapse\">
  <thead><tr><th>Désignation</th><th>Qté</th><th>PU HT</th><th>Total HT</th></tr></thead>
  <tbody>
    {{lines.rows}}
  </tbody>
</table>
<p>Total HT : {{quote.totalHt}} €</p>
</body></html>
""";
  }
}
