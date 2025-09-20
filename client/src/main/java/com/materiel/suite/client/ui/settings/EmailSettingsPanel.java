package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.EmailSettings;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.util.MailSender;

import javax.swing.*;
import java.awt.*;

/** Paramètres SMTP (ordre de mission par email). */
public class EmailSettingsPanel extends JPanel {
  private final JTextField hostField = new JTextField();
  private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(587, 1, 65535, 1));
  private final JCheckBox starttlsCheck = new JCheckBox("Activer STARTTLS");
  private final JCheckBox authCheck = new JCheckBox("Authentification requise");
  private final JTextField usernameField = new JTextField();
  private final JPasswordField passwordField = new JPasswordField();
  private final JTextField fromAddressField = new JTextField();
  private final JTextField fromNameField = new JTextField();
  private final JTextField ccField = new JTextField();
  private final JTextField subjectField = new JTextField();
  private final JTextArea bodyArea = new JTextArea(8, 32);
  private final JCheckBox htmlCheck = new JCheckBox("Envoyer en HTML (multipart)");
  private final JTextArea htmlArea = new JTextArea(10, 32);
  private final JCheckBox trackingCheck = new JCheckBox("Activer le tracking d'ouverture (pixel 1×1)");
  private final JTextField trackingBaseField = new JTextField();
  private final JButton saveButton = new JButton("Enregistrer");
  private final JButton testButton = new JButton("Envoyer un email de test");

  public EmailSettingsPanel(){
    super(new BorderLayout(8, 8));

    bodyArea.setLineWrap(true);
    bodyArea.setWrapStyleWord(true);
    htmlArea.setLineWrap(true);
    htmlArea.setWrapStyleWord(true);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.WEST;
    gc.weightx = 0;

    int row = 0;
    gc.gridx = 0; gc.gridy = row;
    form.add(new JLabel("Serveur SMTP"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(hostField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Port"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(portSpinner, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Sécurité"), gc);
    JPanel securityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    securityPanel.add(starttlsCheck);
    securityPanel.add(authCheck);
    gc.gridx = 1; gc.weightx = 1;
    form.add(securityPanel, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Utilisateur"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(usernameField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Mot de passe"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(passwordField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Adresse d'expédition"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(fromAddressField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Nom expéditeur"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(fromNameField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("CC (optionnel)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(ccField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Sujet (template)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(subjectField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    gc.anchor = GridBagConstraints.NORTHWEST;
    form.add(new JLabel("Corps TEXTE (template)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(new JScrollPane(bodyArea), gc);
    gc.anchor = GridBagConstraints.WEST;

    row++;
    gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
    form.add(htmlCheck, gc);
    gc.gridwidth = 1;

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    gc.anchor = GridBagConstraints.NORTHWEST;
    form.add(new JLabel("Corps HTML (template)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(new JScrollPane(htmlArea), gc);
    gc.anchor = GridBagConstraints.WEST;

    row++;
    gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
    form.add(trackingCheck, gc);
    gc.gridwidth = 1;

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Tracking base URL"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(trackingBaseField, gc);

    add(form, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    actions.add(testButton);
    actions.add(saveButton);
    add(actions, BorderLayout.SOUTH);

    loadSettings();
    configureAccess();

    saveButton.addActionListener(e -> onSave());
    testButton.addActionListener(e -> onTest());
  }

  private void loadSettings(){
    EmailSettings settings = ServiceLocator.emailSettings();
    hostField.setText(settings.getSmtpHost() == null ? "" : settings.getSmtpHost());
    portSpinner.setValue(settings.getSmtpPort());
    starttlsCheck.setSelected(settings.isStarttls());
    authCheck.setSelected(settings.isAuth());
    usernameField.setText(settings.getUsername() == null ? "" : settings.getUsername());
    passwordField.setText(settings.getPassword() == null ? "" : settings.getPassword());
    fromAddressField.setText(settings.getFromAddress() == null ? "" : settings.getFromAddress());
    fromNameField.setText(settings.getFromName() == null ? "" : settings.getFromName());
    ccField.setText(settings.getCcAddress() == null ? "" : settings.getCcAddress());
    subjectField.setText(settings.getSubjectTemplate());
    bodyArea.setText(settings.getBodyTemplate());
    htmlCheck.setSelected(settings.isEnableHtml());
    htmlArea.setText(settings.getHtmlTemplate());
    trackingCheck.setSelected(settings.isEnableOpenTracking());
    trackingBaseField.setText(settings.getTrackingBaseUrl() == null ? "" : settings.getTrackingBaseUrl());
  }

  private void configureAccess(){
    boolean canEdit = AccessControl.canEditSettings();
    hostField.setEnabled(canEdit);
    portSpinner.setEnabled(canEdit);
    starttlsCheck.setEnabled(canEdit);
    authCheck.setEnabled(canEdit);
    usernameField.setEnabled(canEdit);
    passwordField.setEnabled(canEdit);
    fromAddressField.setEnabled(canEdit);
    fromNameField.setEnabled(canEdit);
    ccField.setEnabled(canEdit);
    subjectField.setEnabled(canEdit);
    bodyArea.setEnabled(canEdit);
    bodyArea.setEditable(canEdit);
    htmlCheck.setEnabled(canEdit);
    htmlArea.setEnabled(canEdit);
    htmlArea.setEditable(canEdit);
    trackingCheck.setEnabled(canEdit);
    trackingBaseField.setEnabled(canEdit);
    saveButton.setEnabled(canEdit);
    testButton.setEnabled(canEdit);
  }

  private void onSave(){
    EmailSettings settings = new EmailSettings();
    settings.setSmtpHost(hostField.getText());
    settings.setSmtpPort(((Number) portSpinner.getValue()).intValue());
    settings.setStarttls(starttlsCheck.isSelected());
    settings.setAuth(authCheck.isSelected());
    settings.setUsername(usernameField.getText());
    settings.setPassword(new String(passwordField.getPassword()));
    settings.setFromAddress(fromAddressField.getText());
    settings.setFromName(fromNameField.getText());
    settings.setCcAddress(ccField.getText());
    settings.setSubjectTemplate(subjectField.getText());
    settings.setBodyTemplate(bodyArea.getText());
    settings.setEnableHtml(htmlCheck.isSelected());
    settings.setHtmlTemplate(htmlArea.getText());
    settings.setEnableOpenTracking(trackingCheck.isSelected());
    settings.setTrackingBaseUrl(trackingBaseField.getText());
    try {
      ServiceLocator.saveEmailSettings(settings);
      Toasts.success(this, "Paramètres email enregistrés");
    } catch (RuntimeException ex){
      Toasts.error(this, "Impossible d'enregistrer les paramètres email");
    }
  }

  private void onTest(){
    onSave();
    String address = JOptionPane.showInputDialog(this, "Adresse de test :", "test@example.com");
    if (address == null || address.isBlank()){
      return;
    }
    try {
      MailSender.testSend(address.trim());
      Toasts.success(this, "Email de test envoyé");
    } catch (Exception ex){
      Toasts.error(this, "Échec de l'envoi : " + ex.getMessage());
    }
  }
}
