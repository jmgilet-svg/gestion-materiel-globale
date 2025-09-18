package com.materiel.suite.client.ui.sales;

import com.materiel.suite.client.auth.AccessControl;

import javax.swing.*;
import java.awt.*;

/** Boîte de dialogue de base pour les écrans de vente (lecture seule selon les droits). */
public abstract class BaseSalesDialog extends JDialog {
  protected JButton saveButton;

  protected BaseSalesDialog(Window owner, String title){
    super(owner, title, ModalityType.APPLICATION_MODAL);
  }

  protected void enforceSalesPolicy(JComponent root){
    boolean canEdit = AccessControl.canEditSales();
    if (saveButton != null){
      saveButton.setEnabled(canEdit);
    }
    if (!canEdit){
      makeReadOnly(root);
    }
  }

  private void makeReadOnly(Component component){
    if (component == null){
      return;
    }
    if (component instanceof JTextField textField){
      textField.setEditable(false);
    } else if (component instanceof JTextArea textArea){
      textArea.setEditable(false);
    } else if (component instanceof JFormattedTextField formatted){
      formatted.setEditable(false);
    } else if (component instanceof JComboBox<?> comboBox){
      comboBox.setEnabled(false);
    } else if (component instanceof JSpinner spinner){
      spinner.setEnabled(false);
    } else if (component instanceof JButton button){
      String label = button.getText();
      if (label != null){
        String normalized = label.toLowerCase();
        if (normalized.contains("enregistrer") || normalized.contains("supprimer") || normalized.contains("ajouter") || normalized.contains("modifier") || normalized.contains("créer")){
          button.setEnabled(false);
        }
      }
    }
    if (component instanceof Container container){
      for (Component child : container.getComponents()){
        makeReadOnly(child);
      }
    }
  }
}
