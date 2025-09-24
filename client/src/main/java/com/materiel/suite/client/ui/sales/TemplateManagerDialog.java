package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.TemplatesGateway;
import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Dialog simple pour gérer les modèles HTML (devis, factures, emails) par agence.
 */
public class TemplateManagerDialog extends JDialog {
  private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"QUOTE", "INVOICE", "EMAIL", "PARTIAL"});
  private final DefaultListModel<TemplatesGateway.Template> listModel = new DefaultListModel<>();
  private final JList<TemplatesGateway.Template> list = new JList<>(listModel);
  private final JTextField keyField = new JTextField();
  private final JTextField nameField = new JTextField();
  private final JTextArea contentArea = new JTextArea(24, 80);
  private final JButton newBtn = new JButton("Nouveau");
  private final JButton saveBtn = new JButton("Enregistrer");
  private final JButton deleteBtn = new JButton("Supprimer");
  private final JButton previewBtn = new JButton("Prévisualiser PDF");

  public TemplateManagerDialog(Window owner){
    super(owner, "Modèles (templates) par agence", ModalityType.APPLICATION_MODAL);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    top.add(new JLabel("Type"));
    top.add(typeCombo);
    add(top, BorderLayout.NORTH);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane listScroll = new JScrollPane(list);
    listScroll.setPreferredSize(new Dimension(240, 360));

    JPanel editor = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 6, 6, 6);
    gc.gridx = 0;
    gc.gridy = 0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    editor.add(new JLabel("Clé"), gc);
    gc.gridx = 1;
    gc.weightx = 1.0;
    editor.add(keyField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.weightx = 0;
    editor.add(new JLabel("Nom"), gc);
    gc.gridx = 1;
    gc.weightx = 1.0;
    editor.add(nameField, gc);

    gc.gridx = 0;
    gc.gridy++;
    gc.gridwidth = 2;
    gc.weightx = 1.0;
    gc.weighty = 1.0;
    gc.fill = GridBagConstraints.BOTH;
    contentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    contentArea.setLineWrap(false);
    contentArea.setWrapStyleWord(false);
    editor.add(new JScrollPane(contentArea), gc);

    gc.gridy++;
    gc.gridwidth = 2;
    gc.weighty = 0;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.LINE_END;
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    buttons.add(newBtn);
    buttons.add(saveBtn);
    buttons.add(deleteBtn);
    buttons.add(previewBtn);
    editor.add(buttons, gc);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, editor);
    split.setResizeWeight(0.25);
    split.setContinuousLayout(true);
    add(split, BorderLayout.CENTER);

    typeCombo.addActionListener(e -> reload(null));
    list.addListSelectionListener(this::onSelect);
    newBtn.addActionListener(e -> onNew());
    saveBtn.addActionListener(e -> onSave());
    deleteBtn.addActionListener(e -> onDelete());
    previewBtn.addActionListener(e -> onPreview());

    setSize(1100, 700);
    setLocationRelativeTo(owner);

    reload(null);
  }

  private void onSelect(ListSelectionEvent event){
    if (event != null && event.getValueIsAdjusting()){
      return;
    }
    applyTemplate(list.getSelectedValue());
  }

  private void onNew(){
    list.clearSelection();
    keyField.setText(defaultKey());
    nameField.setText(defaultName());
    contentArea.setText("");
    contentArea.setCaretPosition(0);
    nameField.requestFocusInWindow();
  }

  private void onSave(){
    String selectedType = (String) typeCombo.getSelectedItem();
    TemplatesGateway.Template selected = list.getSelectedValue();
    String id = selected == null ? null : selected.id();
    String agencyId = selected == null ? ServiceLocator.agencyId() : selected.agencyId();
    TemplatesGateway.Template payload = new TemplatesGateway.Template(
        id,
        agencyId,
        selectedType,
        normalize(keyField.getText()),
        normalize(nameField.getText()),
        contentArea.getText()
    );
    try {
      TemplatesGateway.Template saved = ServiceLocator.templates().save(payload);
      Toasts.success(this, "Modèle enregistré");
      if (saved != null && saved.id() != null){
        reload(saved.id());
      } else {
        reload(null);
        if (saved != null){
          list.setSelectedValue(saved, true);
          if (list.isSelectionEmpty()){
            applyTemplate(saved);
          }
        }
      }
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
    try {
      ServiceLocator.templates().delete(template);
      Toasts.success(this, "Modèle supprimé");
      reload(null);
    } catch (Exception ex){
      Toasts.error(this, "Suppression: " + ex.getMessage());
    }
  }

  private void onPreview(){
    String html = contentArea.getText();
    if (html == null || html.isBlank()){
      Toasts.info(this, "Contenu vide");
      return;
    }
    try {
      byte[] pdf = PdfTemplateEngine.renderHtmlForPreview(html, null);
      JFileChooser chooser = new JFileChooser();
      chooser.setSelectedFile(new File("template-preview.pdf"));
      if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
        return;
      }
      Files.write(chooser.getSelectedFile().toPath(), pdf);
      Toasts.success(this, "PDF exporté : " + chooser.getSelectedFile().getName());
    } catch (Exception ex){
      Toasts.error(this, "Preview: " + ex.getMessage());
    }
  }

  private void reload(String selectId){
    listModel.clear();
    TemplatesGateway gateway = ServiceLocator.templates();
    String selectedType = (String) typeCombo.getSelectedItem();
    List<TemplatesGateway.Template> templates = gateway == null ? List.of() : gateway.list(selectedType);
    TemplatesGateway.Template toSelect = null;
    if (templates != null){
      for (TemplatesGateway.Template template : templates){
        listModel.addElement(template);
        if (selectId != null && template != null && selectId.equals(template.id())){
          toSelect = template;
        }
      }
    }
    if (toSelect != null){
      list.setSelectedValue(toSelect, true);
      return;
    }
    if (!listModel.isEmpty()){
      list.setSelectedIndex(0);
    } else {
      list.clearSelection();
      applyTemplate(null);
    }
  }

  private void applyTemplate(TemplatesGateway.Template template){
    if (template == null){
      keyField.setText(defaultKey());
      nameField.setText(defaultName());
      contentArea.setText("");
    } else {
      keyField.setText(template.key() == null ? "" : template.key());
      nameField.setText(template.name() == null ? "" : template.name());
      contentArea.setText(template.content() == null ? "" : template.content());
    }
    contentArea.setCaretPosition(0);
  }

  private String normalize(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String defaultKey(){
    String selectedType = (String) typeCombo.getSelectedItem();
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
    String selectedType = (String) typeCombo.getSelectedItem();
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
}
