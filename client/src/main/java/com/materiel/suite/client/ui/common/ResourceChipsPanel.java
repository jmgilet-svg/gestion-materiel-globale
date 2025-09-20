package com.materiel.suite.client.ui.common;

import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.service.ServiceLocator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Sélecteur compact affichant les ressources sous forme de "chips" cliquables. */
public class ResourceChipsPanel extends JPanel {
  public interface Listener {
    void onPick(Resource resource);
  }

  private final JTextField searchField = new JTextField();
  private final JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
  private final List<Resource> resources;
  private Listener listener;

  public ResourceChipsPanel(){
    super(new BorderLayout(6, 6));
    List<Resource> list = ServiceLocator.resources().listAll();
    resources = list != null ? List.copyOf(list) : List.of();

    searchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e){ refresh(); }
      @Override public void removeUpdate(DocumentEvent e){ refresh(); }
      @Override public void changedUpdate(DocumentEvent e){ refresh(); }
    });

    chipsPanel.setOpaque(false);

    JScrollPane scroll = new JScrollPane(chipsPanel);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    add(searchField, BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);

    refresh();
  }

  public void setListener(Listener listener){
    this.listener = listener;
  }

  @Override
  public void setEnabled(boolean enabled){
    super.setEnabled(enabled);
    searchField.setEnabled(enabled);
    searchField.setEditable(enabled);
    for (Component component : chipsPanel.getComponents()){
      component.setEnabled(enabled);
    }
  }

  private void refresh(){
    String query = searchField.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<Resource> filtered = resources.stream()
        .filter(resource -> matches(resource, normalized))
        .limit(200)
        .collect(Collectors.toList());
    chipsPanel.removeAll();
    for (Resource resource : filtered){
      chipsPanel.add(createChip(resource));
    }
    chipsPanel.revalidate();
    chipsPanel.repaint();
  }

  private boolean matches(Resource resource, String query){
    if (query.isBlank()){
      return true;
    }
    if (resource == null){
      return false;
    }
    String name = resource.getName() != null ? resource.getName() : "";
    String type = resource.getTypeName() != null ? resource.getTypeName() : "";
    String haystack = (name + " " + type).toLowerCase(Locale.ROOT);
    return haystack.contains(query);
  }

  private JComponent createChip(Resource resource){
    String label = resource != null && resource.getName() != null && !resource.getName().isBlank()
        ? resource.getName()
        : "Ressource";
    JButton button = new JButton(label);
    button.setFocusable(false);
    button.setMargin(new Insets(2, 8, 2, 8));
    button.putClientProperty("JButton.buttonType", "roundRect");
    button.setEnabled(isEnabled());
    button.addActionListener(e -> {
      if (listener != null){
        listener.onPick(resource);
      }
    });
    return button;
  }
}
