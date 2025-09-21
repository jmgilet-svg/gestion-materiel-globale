package com.materiel.suite.client.ui.settings;

import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.auth.SessionManager;
import com.materiel.suite.client.events.AppEventBus;
import com.materiel.suite.client.events.SettingsEvents;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.GeneralSettings;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.theme.ThemeManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/** Paramètres généraux (durée d'inactivité, autosave, etc.). */
public class GeneralSettingsPanel extends JPanel {
  private final JSpinner timeoutSpinner;
  private final JSpinner autosaveSpinner;
  private final JSpinner defaultVatSpinner;
  private final JComboBox<String> roundingModeCombo;
  private final JSpinner roundingScaleSpinner;
  private final JSpinner uiScaleSpinner;
  private final JCheckBox highContrastCheck;
  private final JCheckBox dyslexiaCheck;
  private final JTextField brandPrimaryField;
  private final JLabel logoPreview;
  private String logoBase64;
  private final JTextField agencyNameField;
  private final JTextField agencyPhoneField;
  private final JTextArea agencyAddressArea;
  private final JLabel cgvPdfPreview;
  private String cgvPdfBase64;
  private final JTextArea cgvTextArea;

  public GeneralSettingsPanel(){
    super(new BorderLayout(8, 8));

    GeneralSettings settings = loadSettings();
    logoBase64 = settings.getAgencyLogoPngBase64();
    cgvPdfBase64 = settings.getCgvPdfBase64();

    agencyNameField = new JTextField(settings.getAgencyName() == null ? "" : settings.getAgencyName());
    agencyPhoneField = new JTextField(settings.getAgencyPhone() == null ? "" : settings.getAgencyPhone());
    agencyAddressArea = new JTextArea(settings.getAgencyAddress() == null ? "" : settings.getAgencyAddress(), 3, 24);
    agencyAddressArea.setLineWrap(true);
    agencyAddressArea.setWrapStyleWord(true);
    cgvPdfPreview = new JLabel();
    cgvTextArea = new JTextArea(settings.getCgvText() == null ? "" : settings.getCgvText(), 6, 24);
    cgvTextArea.setLineWrap(true);
    cgvTextArea.setWrapStyleWord(true);
    int initialScale = Math.max(80, Math.min(130, settings.getUiScalePercent()));
    uiScaleSpinner = new JSpinner(new SpinnerNumberModel(initialScale, 80, 130, 5));
    highContrastCheck = new JCheckBox("Contraste élevé (focus renforcé)");
    highContrastCheck.setSelected(settings.isHighContrast());
    dyslexiaCheck = new JCheckBox("Mode dyslexie (OpenDyslexic)");
    dyslexiaCheck.setSelected(settings.isDyslexiaMode());
    brandPrimaryField = new JTextField(settings.getBrandPrimaryHex() == null ? "" : settings.getBrandPrimaryHex(), 9);
    brandPrimaryField.setColumns(9);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(8, 8, 8, 8);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.WEST;

    int row = 0;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Durée d'inactivité avant déconnexion (minutes)"), gc);
    SpinnerNumberModel timeoutModel = new SpinnerNumberModel(settings.getSessionTimeoutMinutes(), 1, 240, 5);
    timeoutSpinner = new JSpinner(timeoutModel);
    gc.gridx = 1; gc.weightx = 1;
    form.add(timeoutSpinner, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Intervalle d'enregistrement automatique (secondes)"), gc);
    SpinnerNumberModel autosaveModel = new SpinnerNumberModel(Math.max(5, settings.getAutosaveIntervalSeconds()), 5, 600, 5);
    autosaveSpinner = new JSpinner(autosaveModel);
    gc.gridx = 1; gc.weightx = 1;
    form.add(autosaveSpinner, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("TVA par défaut (%)"), gc);
    double vatValue = settings.getDefaultVatPercent() != null ? settings.getDefaultVatPercent() : 20.0;
    SpinnerNumberModel vatModel = new SpinnerNumberModel(vatValue, 0.0, 100.0, 0.5);
    defaultVatSpinner = new JSpinner(vatModel);
    gc.gridx = 1; gc.weightx = 1;
    form.add(defaultVatSpinner, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Mode d'arrondi"), gc);
    roundingModeCombo = new JComboBox<>(new String[]{"HALF_UP", "HALF_DOWN", "HALF_EVEN"});
    String roundingMode = settings.getRoundingMode();
    if (roundingMode != null && !roundingMode.isBlank()){
      roundingModeCombo.setSelectedItem(roundingMode);
    } else {
      roundingModeCombo.setSelectedItem("HALF_UP");
    }
    gc.gridx = 1; gc.weightx = 1;
    form.add(roundingModeCombo, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Décimales d'arrondi"), gc);
    SpinnerNumberModel scaleModel = new SpinnerNumberModel(Math.max(0, settings.getRoundingScale()), 0, 6, 1);
    roundingScaleSpinner = new JSpinner(scaleModel);
    gc.gridx = 1; gc.weightx = 1;
    form.add(roundingScaleSpinner, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Échelle interface (%)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(uiScaleSpinner, gc);

    row++;
    gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
    form.add(highContrastCheck, gc);

    row++;
    gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
    form.add(dyslexiaCheck, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Couleur primaire (branding)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(brandPrimaryField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Logo d'agence (PNG)"), gc);
    JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton chooseLogo = new JButton("Choisir…");
    JButton clearLogo = new JButton("Supprimer");
    logoPreview = new JLabel();
    updateLogoPreview();
    logoRow.add(chooseLogo);
    logoRow.add(clearLogo);
    logoRow.add(logoPreview);
    gc.gridx = 1; gc.weightx = 1;
    form.add(logoRow, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Nom d'agence"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(agencyNameField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Téléphone agence"), gc);
    gc.gridx = 1; gc.weightx = 1;
    form.add(agencyPhoneField, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("Adresse agence"), gc);
    gc.gridx = 1; gc.weightx = 1;
    gc.fill = GridBagConstraints.BOTH;
    form.add(new JScrollPane(agencyAddressArea), gc);
    gc.fill = GridBagConstraints.HORIZONTAL;

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("CGV (PDF)"), gc);
    JPanel cgvRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton chooseCgvPdf = new JButton("Choisir PDF…");
    JButton clearCgvPdf = new JButton("Supprimer");
    updateCgvPdfPreview();
    cgvRow.add(chooseCgvPdf);
    cgvRow.add(clearCgvPdf);
    cgvRow.add(cgvPdfPreview);
    gc.gridx = 1; gc.weightx = 1;
    form.add(cgvRow, gc);

    row++;
    gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
    form.add(new JLabel("CGV (texte fallback)"), gc);
    gc.gridx = 1; gc.weightx = 1;
    gc.fill = GridBagConstraints.BOTH;
    form.add(new JScrollPane(cgvTextArea), gc);
    gc.fill = GridBagConstraints.HORIZONTAL;

    JButton save = new JButton("Enregistrer");
    boolean canEdit = AccessControl.canEditSettings();
    timeoutSpinner.setEnabled(canEdit);
    autosaveSpinner.setEnabled(canEdit);
    defaultVatSpinner.setEnabled(canEdit);
    roundingModeCombo.setEnabled(canEdit);
    roundingScaleSpinner.setEnabled(canEdit);
    uiScaleSpinner.setEnabled(canEdit);
    highContrastCheck.setEnabled(canEdit);
    dyslexiaCheck.setEnabled(canEdit);
    brandPrimaryField.setEnabled(canEdit);
    save.setEnabled(canEdit);
    chooseLogo.setEnabled(canEdit);
    clearLogo.setEnabled(canEdit);
    agencyNameField.setEnabled(canEdit);
    agencyPhoneField.setEnabled(canEdit);
    agencyAddressArea.setEnabled(canEdit);
    agencyAddressArea.setEditable(canEdit);
    chooseCgvPdf.setEnabled(canEdit);
    clearCgvPdf.setEnabled(canEdit);
    cgvTextArea.setEnabled(canEdit);
    cgvTextArea.setEditable(canEdit);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(save);

    add(form, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    save.addActionListener(e -> saveSettings());
    chooseLogo.addActionListener(e -> chooseLogo());
    clearLogo.addActionListener(e -> clearLogo());
    chooseCgvPdf.addActionListener(e -> chooseCgvPdf());
    clearCgvPdf.addActionListener(e -> clearCgvPdf());
  }

  private GeneralSettings loadSettings(){
    try {
      GeneralSettings settings = ServiceLocator.settings().getGeneral();
      if (settings != null){
        return settings;
      }
    } catch (RuntimeException ignore){
    }
    return new GeneralSettings();
  }

  private void saveSettings(){
    int minutes = ((Number) timeoutSpinner.getValue()).intValue();
    int autosave = ((Number) autosaveSpinner.getValue()).intValue();
    double vat = ((Number) defaultVatSpinner.getValue()).doubleValue();
    int scale = ((Number) roundingScaleSpinner.getValue()).intValue();
    int uiScale = ((Number) uiScaleSpinner.getValue()).intValue();
    boolean highContrast = highContrastCheck.isSelected();
    boolean dyslexia = dyslexiaCheck.isSelected();
    String brandPrimary = brandPrimaryField.getText();

    GeneralSettings updated = new GeneralSettings();
    updated.setSessionTimeoutMinutes(minutes);
    updated.setAutosaveIntervalSeconds(autosave);
    updated.setDefaultVatPercent(vat);
    updated.setRoundingMode((String) roundingModeCombo.getSelectedItem());
    updated.setRoundingScale(scale);
    updated.setUiScalePercent(uiScale);
    updated.setHighContrast(highContrast);
    updated.setDyslexiaMode(dyslexia);
    updated.setBrandPrimaryHex(brandPrimary);
    updated.setAgencyLogoPngBase64(logoBase64);
    updated.setAgencyName(agencyNameField.getText());
    updated.setAgencyPhone(agencyPhoneField.getText());
    updated.setAgencyAddress(agencyAddressArea.getText());
    updated.setCgvPdfBase64(cgvPdfBase64);
    updated.setCgvText(cgvTextArea.getText());

    try {
      ServiceLocator.settings().saveGeneral(updated);
      SessionManager.setTimeoutMinutes(updated.getSessionTimeoutMinutes());
      timeoutSpinner.setValue(updated.getSessionTimeoutMinutes());
      autosaveSpinner.setValue(updated.getAutosaveIntervalSeconds());
      defaultVatSpinner.setValue(updated.getDefaultVatPercent() != null ? updated.getDefaultVatPercent() : 20.0);
      roundingModeCombo.setSelectedItem(updated.getRoundingMode());
      roundingScaleSpinner.setValue(updated.getRoundingScale());
      uiScaleSpinner.setValue(updated.getUiScalePercent());
      highContrastCheck.setSelected(updated.isHighContrast());
      dyslexiaCheck.setSelected(updated.isDyslexiaMode());
      brandPrimaryField.setText(updated.getBrandPrimaryHex());
      ThemeManager.applyGeneralSettings(updated);
      ThemeManager.refreshAllFrames();
      AppEventBus.get().publish(new SettingsEvents.GeneralSaved(
          updated.getSessionTimeoutMinutes(),
          updated.getAutosaveIntervalSeconds(),
          updated.getUiScalePercent(),
          updated.isHighContrast(),
          updated.isDyslexiaMode(),
          updated.getBrandPrimaryHex()
      ));
      Toasts.success(this, "Paramètres généraux mis à jour");
    } catch (RuntimeException ex){
      Toasts.error(this, "Échec de l'enregistrement des paramètres");
    }
  }

  private void chooseLogo(){
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(this);
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File file = chooser.getSelectedFile();
    if (file == null){
      return;
    }
    try {
      BufferedImage image = ImageIO.read(file);
      if (image == null){
        throw new IOException("Format non supporté");
      }
      java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
      boolean ok = ImageIO.write(image, "png", buffer);
      if (!ok){
        throw new IOException("Échec de conversion PNG");
      }
      logoBase64 = Base64.getEncoder().encodeToString(buffer.toByteArray());
      logoPreview.setText("Chargé : " + file.getName());
    } catch (IOException ex){
      JOptionPane.showMessageDialog(
          this,
          "Impossible de charger l'image : " + ex.getMessage(),
          "Logo d'agence",
          JOptionPane.ERROR_MESSAGE
      );
    }
  }

  private void clearLogo(){
    logoBase64 = null;
    updateLogoPreview();
  }

  private void chooseCgvPdf(){
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(this);
    if (result != JFileChooser.APPROVE_OPTION){
      return;
    }
    File file = chooser.getSelectedFile();
    if (file == null){
      return;
    }
    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      if (bytes == null || bytes.length == 0){
        throw new IOException("Fichier vide");
      }
      cgvPdfBase64 = Base64.getEncoder().encodeToString(bytes);
      cgvPdfPreview.setText("Chargé : " + file.getName());
    } catch (IOException ex){
      JOptionPane.showMessageDialog(
          this,
          "Impossible de charger le PDF : " + ex.getMessage(),
          "CGV",
          JOptionPane.ERROR_MESSAGE
      );
    }
  }

  private void clearCgvPdf(){
    cgvPdfBase64 = null;
    updateCgvPdfPreview();
  }

  private void updateLogoPreview(){
    if (logoPreview == null){
      return;
    }
    logoPreview.setText(logoBase64 == null || logoBase64.isBlank() ? "(aucun logo)" : "(logo enregistré)");
  }

  private void updateCgvPdfPreview(){
    if (cgvPdfPreview == null){
      return;
    }
    cgvPdfPreview.setText(cgvPdfBase64 == null || cgvPdfBase64.isBlank() ? "(aucun PDF)" : "(PDF CGV enregistré)");
  }
}
