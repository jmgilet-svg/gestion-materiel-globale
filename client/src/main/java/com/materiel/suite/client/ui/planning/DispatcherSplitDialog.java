package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.service.ClientService;
import com.materiel.suite.client.service.InterventionTypeService;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.TemplateService;
import com.materiel.suite.client.ui.interventions.InterventionDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Dispatcher mode showing the intervention editor alongside a compact planning view.
 */
public class DispatcherSplitDialog extends JDialog {
  private final InterventionDialog editor;
  private final MiniPlanningPanel planningPanel;

  public DispatcherSplitDialog(Window owner,
                               PlanningService planningService,
                               ClientService clientService,
                               InterventionTypeService typeService,
                               TemplateService templateService,
                               Intervention intervention){
    super(owner, "Mode Dispatcher", ModalityType.APPLICATION_MODAL);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.editor = new InterventionDialog(owner, planningService, clientService, typeService, templateService);
    this.editor.setParentComponent(this);
    this.editor.setCloseHandler(this::dispose);
    this.editor.setFullscreenButtonVisible(false);
    JComponent editorPanel = this.editor.detachContentPanel();
    this.editor.installShortcutsOn(editorPanel);
    this.planningPanel = new MiniPlanningPanel(planningService);

    Intervention target = intervention != null ? intervention : new Intervention();
    this.editor.edit(target);
    this.planningPanel.showForIntervention(target);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        wrap(editorPanel),
        wrap(planningPanel));
    split.setResizeWeight(0.7);
    split.setBorder(BorderFactory.createEmptyBorder());
    setContentPane(split);
    setMinimumSize(new Dimension(1180, 760));
    setSize(new Dimension(1280, 780));
    setLocationRelativeTo(owner);
  }

  public void setOnSave(java.util.function.Consumer<Intervention> onSave){
    editor.setOnSave(onSave);
  }

  private static JComponent wrap(Component component){
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    return panel;
  }
}
