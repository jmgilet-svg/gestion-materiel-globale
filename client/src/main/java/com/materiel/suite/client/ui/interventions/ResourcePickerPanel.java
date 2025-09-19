package com.materiel.suite.client.ui.interventions;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

/**
 * Sélecteur 3-pans : filtres (gauche) → résultats (centre) → sélection courante (droite).
 * Affiche une mini timeline "conflits" selon le créneau de l'intervention.
 */
public class ResourcePickerPanel extends JPanel {
  private final PlanningService planningService;
  private final JTextField search = new JTextField();
  private final JComboBox<String> typeFilter = new JComboBox<>();
  private final JComboBox<String> stateFilter = new JComboBox<>(new String[]{"(tous)", "DISPONIBLE", "OCCUPEE", "EN_MAINTENANCE"});
  private final JCheckBox onlyAvailable = new JCheckBox("Disponibles sur le créneau");
  private final DefaultListModel<Resource> resultsModel = new DefaultListModel<>();
  private final JList<Resource> resultsList = new JList<>(resultsModel);
  private final DefaultListModel<Resource> selectedModel = new DefaultListModel<>();
  private final JList<Resource> selectedList = new JList<>(selectedModel);
  private final JButton addButton = new JButton("Ajouter", IconRegistry.small("plus"));
  private final JButton removeButton = new JButton("Retirer", IconRegistry.small("trash"));

  private final List<Resource> allResources = new ArrayList<>();
  private final Map<String, Resource> resourceIndex = new LinkedHashMap<>();
  private Map<String, List<BusySlot>> busyByResource = new HashMap<>();
  private LocalDateTime plannedStart;
  private LocalDateTime plannedEnd;
  private Intervention context;
  private Runnable selectionListener;
  private boolean readOnly;

  public ResourcePickerPanel(){
    this(ServiceFactory.planning());
  }

  public ResourcePickerPanel(PlanningService planningService){
    super(new BorderLayout(8, 8));
    this.planningService = planningService;
    buildUI();
    wireEvents();
    resultsList.setCellRenderer(new ResourceCellRenderer());
    selectedList.setCellRenderer(new ResourceCellRenderer());
    resultsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    selectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    typeFilter.addItem("(tous)");
  }

  private void buildUI(){
    JPanel filters = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(4, 4, 4, 4);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.NORTHWEST;
    int y = 0;
    gc.gridx = 0; gc.gridy = y; filters.add(new JLabel("Recherche"), gc);
    gc.gridx = 0; gc.gridy = ++y; filters.add(search, gc);
    gc.gridx = 0; gc.gridy = ++y; filters.add(new JLabel("Type"), gc);
    gc.gridx = 0; gc.gridy = ++y; filters.add(typeFilter, gc);
    gc.gridx = 0; gc.gridy = ++y; filters.add(new JLabel("État"), gc);
    gc.gridx = 0; gc.gridy = ++y; filters.add(stateFilter, gc);
    gc.gridx = 0; gc.gridy = ++y; filters.add(onlyAvailable, gc);
    gc.weighty = 1; gc.gridy = ++y; filters.add(Box.createVerticalGlue(), gc);

    JScrollPane center = new JScrollPane(resultsList);

    JPanel right = new JPanel(new BorderLayout(4, 4));
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(addButton);
    toolbar.add(removeButton);
    right.add(toolbar, BorderLayout.NORTH);
    right.add(new JScrollPane(selectedList), BorderLayout.CENTER);

    JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filters, center);
    splitLeft.setResizeWeight(0.18);
    JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitLeft, right);
    splitMain.setResizeWeight(0.66);
    add(splitMain, BorderLayout.CENTER);

    addButton.addActionListener(e -> moveSelected(resultsList, resultsModel, selectedModel));
    removeButton.addActionListener(e -> moveSelected(selectedList, selectedModel, resultsModel));
    resultsList.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          moveSelected(resultsList, resultsModel, selectedModel);
        }
      }
    });
    selectedList.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (e.getClickCount() == 2){
          moveSelected(selectedList, selectedModel, resultsModel);
        }
      }
    });
  }

  private void wireEvents(){
    ActionListener refresh = e -> refreshResults();
    typeFilter.addActionListener(refresh);
    stateFilter.addActionListener(refresh);
    onlyAvailable.addActionListener(refresh);
    search.addActionListener(refresh);
    search.getDocument().addDocumentListener(new SimpleDocListener(this::refreshResults));
  }

  public void setSelectionListener(Runnable listener){
    this.selectionListener = listener;
  }

  public void setReadOnly(boolean readOnly){
    this.readOnly = readOnly;
    search.setEnabled(!readOnly);
    search.setEditable(!readOnly);
    typeFilter.setEnabled(!readOnly);
    stateFilter.setEnabled(!readOnly);
    onlyAvailable.setEnabled(!readOnly);
    addButton.setEnabled(!readOnly);
    removeButton.setEnabled(!readOnly);
    resultsList.setEnabled(!readOnly);
    selectedList.setEnabled(!readOnly);
  }

  public void setContext(Intervention context){
    this.context = context;
    recomputeBusyMap();
  }

  public void setPlannedWindow(LocalDateTime start, LocalDateTime end){
    this.plannedStart = start;
    this.plannedEnd = end;
    recomputeBusyMap();
    refreshResults();
  }

  public void setResources(List<Resource> resources){
    allResources.clear();
    resourceIndex.clear();
    if (resources != null){
      for (Resource resource : resources){
        if (resource == null){
          continue;
        }
        allResources.add(resource);
        String key = keyOf(resource);
        if (!key.isEmpty()){
          resourceIndex.put(key, resource);
        }
      }
    }
    alignSelectionWithIndex();
    rebuildTypeFilter();
    recomputeBusyMap();
    refreshResults();
  }

  public void setSelectedResources(List<ResourceRef> refs){
    selectedModel.clear();
    if (refs != null){
      for (ResourceRef ref : refs){
        if (ref == null){
          continue;
        }
        Resource resource = fromRef(ref);
        if (resource != null && !contains(selectedModel, resource)){
          selectedModel.addElement(resource);
        }
      }
    }
    if (context != null){
      context.setResources(getSelectedResourceRefs());
    }
    refreshResults();
  }

  public List<Resource> getSelectedResources(){
    List<Resource> list = new ArrayList<>();
    for (int i = 0; i < selectedModel.size(); i++){
      Resource resource = selectedModel.get(i);
      if (resource == null){
        continue;
      }
      Resource resolved = resolve(resource);
      if (resolved != null){
        list.add(resolved);
      }
    }
    return list;
  }

  public List<ResourceRef> getSelectedResourceRefs(){
    List<ResourceRef> refs = new ArrayList<>();
    for (int i = 0; i < selectedModel.size(); i++){
      Resource resource = selectedModel.get(i);
      if (resource == null){
        continue;
      }
      refs.add(toRef(resource));
    }
    return refs;
  }

  private void alignSelectionWithIndex(){
    for (int i = 0; i < selectedModel.size(); i++){
      Resource current = selectedModel.get(i);
      Resource resolved = resolve(current);
      if (resolved != null && resolved != current){
        selectedModel.set(i, resolved);
      }
    }
  }

  private Resource resolve(Resource resource){
    if (resource == null){
      return null;
    }
    String key = keyOf(resource);
    Resource resolved = resourceIndex.get(key);
    return resolved != null ? resolved : resource;
  }

  private Resource fromRef(ResourceRef ref){
    String key = keyOf(ref);
    Resource resource = key.isEmpty() ? null : resourceIndex.get(key);
    if (resource != null){
      return resource;
    }
    if (ref.getId() != null){
      resource = new Resource(ref.getId(), ref.getName());
    } else {
      resource = new Resource(null, ref.getName());
    }
    if (ref.getIcon() != null && !ref.getIcon().isBlank()){
      ResourceType type = new ResourceType();
      type.setIcon(ref.getIcon());
      type.setName(ref.getName());
      resource.setType(type);
    }
    return resource;
  }

  private void notifySelectionChanged(){
    if (context != null){
      context.setResources(getSelectedResourceRefs());
    }
    if (selectionListener != null){
      selectionListener.run();
    }
  }

  private void moveSelected(JList<Resource> fromList, DefaultListModel<Resource> from, DefaultListModel<Resource> to){
    if (readOnly){
      return;
    }
    List<Resource> picked = fromList.getSelectedValuesList();
    if (picked == null || picked.isEmpty()){
      return;
    }
    boolean changed = false;
    for (Resource resource : picked){
      from.removeElement(resource);
      if (to == selectedModel){
        if (!contains(selectedModel, resource)){
          selectedModel.addElement(resource);
          changed = true;
        }
      } else {
        changed = true;
      }
    }
    if (changed){
      notifySelectionChanged();
      refreshResults();
    }
  }

  private boolean contains(DefaultListModel<Resource> model, Resource resource){
    String key = keyOf(resource);
    for (int i = 0; i < model.size(); i++){
      if (Objects.equals(key, keyOf(model.get(i)))){
        return true;
      }
    }
    return false;
  }

  private void rebuildTypeFilter(){
    Object previous = typeFilter.getSelectedItem();
    typeFilter.removeAllItems();
    typeFilter.addItem("(tous)");
    Map<String, Boolean> seen = new LinkedHashMap<>();
    for (Resource resource : allResources){
      ResourceType type = resource != null ? resource.getType() : null;
      String name = type != null ? type.getName() : null;
      if (name != null && !name.isBlank() && seen.putIfAbsent(name, Boolean.TRUE) == null){
        typeFilter.addItem(name);
      }
    }
    if (previous != null){
      typeFilter.setSelectedItem(previous);
    }
  }

  private void refreshResults(){
    Set<String> selectedKeys = toIdSet(selectedModel);
    String query = search.getText();
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    String type = (String) typeFilter.getSelectedItem();
    String state = (String) stateFilter.getSelectedItem();
    boolean availOnly = onlyAvailable.isSelected();

    List<Resource> filtered = new ArrayList<>();
    for (Resource resource : allResources){
      if (resource == null){
        continue;
      }
      String key = keyOf(resource);
      if (selectedKeys.contains(key)){
        continue;
      }
      if (type != null && !"(tous)".equals(type)){
        ResourceType rt = resource.getType();
        String name = rt != null ? rt.getName() : null;
        if (name == null || !name.equals(type)){
          continue;
        }
      }
      if (state != null && !"(tous)".equals(state)){
        String current = resource.getState();
        if (current == null || !current.equals(state)){
          continue;
        }
      }
      if (!normalized.isEmpty() && !matches(resource, normalized)){
        continue;
      }
      if (availOnly && hasOverlap(busyByResource.get(key))){
        continue;
      }
      filtered.add(resource);
    }
    filtered.sort(Comparator.comparing(r -> Optional.ofNullable(r.getName()).orElse("").toLowerCase(Locale.ROOT)));
    resultsModel.clear();
    for (Resource resource : filtered){
      resultsModel.addElement(resource);
    }
  }

  private boolean matches(Resource resource, String query){
    if (resource == null){
      return false;
    }
    String name = resource.getName();
    if (name != null && name.toLowerCase(Locale.ROOT).contains(query)){
      return true;
    }
    ResourceType type = resource.getType();
    String typeName = type != null ? type.getName() : null;
    return typeName != null && typeName.toLowerCase(Locale.ROOT).contains(query);
  }
  private boolean hasOverlap(List<BusySlot> slots){
    if (slots == null || slots.isEmpty()){
      return false;
    }
    if (plannedStart == null || plannedEnd == null){
      return false;
    }
    for (BusySlot slot : slots){
      if (slot == null || slot.start == null || slot.end == null){
        continue;
      }
      if (plannedStart.isBefore(slot.end) && slot.start.isBefore(plannedEnd)){
        return true;
      }
    }
    return false;
  }

  private void recomputeBusyMap(){
    busyByResource = computeBusyMap(plannedStart, plannedEnd);
  }

  private Map<String, List<BusySlot>> computeBusyMap(LocalDateTime start, LocalDateTime end){
    Map<String, List<BusySlot>> map = new HashMap<>();
    if (planningService == null || start == null || end == null){
      return map;
    }
    LocalDate from = start.toLocalDate().minusDays(1);
    LocalDate to = end.toLocalDate().plusDays(1);
    List<Intervention> interventions;
    try {
      interventions = planningService.listInterventions(from, to);
    } catch (Exception ex){
      return map;
    }
    UUID currentId = context != null ? context.getId() : null;
    if (interventions != null){
      for (Intervention intervention : interventions){
        if (intervention == null){
          continue;
        }
        if (currentId != null && currentId.equals(intervention.getId())){
          continue;
        }
        LocalDateTime s = intervention.getDateHeureDebut();
        LocalDateTime e = intervention.getDateHeureFin();
        if (s == null || e == null){
          continue;
        }
        for (ResourceRef ref : intervention.getResources()){
          String key = keyOf(ref);
          if (key.isEmpty()){
            continue;
          }
          map.computeIfAbsent(key, k -> new ArrayList<>())
             .add(new BusySlot(s, e, intervention.getLabel()));
        }
      }
    }
    for (Resource resource : allResources){
      if (resource == null){
        continue;
      }
      String key = keyOf(resource);
      if (key.isEmpty()){
        continue;
      }
      List<Unavailability> unavailabilities = resource.getUnavailabilities();
      if (unavailabilities == null){
        continue;
      }
      for (Unavailability unavailability : unavailabilities){
        LocalDateTime s = unavailability.getStart();
        LocalDateTime e = unavailability.getEnd();
        if (s == null || e == null){
          continue;
        }
        map.computeIfAbsent(key, k -> new ArrayList<>())
           .add(new BusySlot(s, e, unavailability.getReason()));
      }
    }
    return map;
  }

  private String keyOf(Resource resource){
    if (resource == null){
      return "";
    }
    UUID id = resource.getId();
    if (id != null){
      return id.toString();
    }
    String name = resource.getName();
    return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  private String keyOf(ResourceRef ref){
    if (ref == null){
      return "";
    }
    UUID id = ref.getId();
    if (id != null){
      return id.toString();
    }
    String name = ref.getName();
    return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  private ResourceRef toRef(Resource resource){
    if (resource == null){
      return new ResourceRef();
    }
    ResourceType type = resource.getType();
    return new ResourceRef(resource.getId(), resource.getName(), type != null ? type.getIconKey() : null);
  }

  private Set<String> toIdSet(DefaultListModel<Resource> model){
    Set<String> set = new java.util.HashSet<>();
    for (int i = 0; i < model.size(); i++){
      set.add(keyOf(model.get(i)));
    }
    return set;
  }

  private static String escapeHtml(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private class ResourceCellRenderer extends JPanel implements ListCellRenderer<Resource> {
    private final JLabel left = new JLabel();
    private final JProgressBar bar = new JProgressBar(0, 100);
    private final JLabel right = new JLabel();

    ResourceCellRenderer(){
      super(new BorderLayout(6, 0));
      bar.setPreferredSize(new Dimension(80, 8));
      bar.setBorderPainted(false);
      bar.setStringPainted(false);
      add(left, BorderLayout.WEST);
      add(bar, BorderLayout.CENTER);
      add(right, BorderLayout.EAST);
      setBorder(new EmptyBorder(3, 6, 3, 6));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Resource> list, Resource value, int index, boolean isSelected, boolean cellHasFocus){
      setOpaque(true);
      setBackground(isSelected ? new Color(0xDCEAFB) : Color.WHITE);
      ResourceType type = value != null ? value.getType() : null;
      left.setIcon(type != null ? IconRegistry.small(type.getIconKey()) : null);
      String name = value != null ? value.getName() : "";
      String typeName = type != null ? Optional.ofNullable(type.getName()).orElse("") : "";
      left.setText("<html>" + escapeHtml(name) + "<br/><span style='color:#607D8B;font-size:10px;'>" + escapeHtml(typeName) + "</span></html>");
      BigDecimal price = value != null ? value.getUnitPriceHt() : null;
      right.setText(price != null ? price.stripTrailingZeros().toPlainString() + " € HT" : "");
      List<BusySlot> slots = busyByResource.get(keyOf(value));
      boolean conflict = hasOverlap(slots);
      bar.setValue(conflict ? 100 : 0);
      bar.setForeground(conflict ? new Color(0xE53935) : new Color(0x43A047));
      return this;
    }
  }

  private static class BusySlot {
    final LocalDateTime start;
    final LocalDateTime end;
    final String label;

    BusySlot(LocalDateTime start, LocalDateTime end, String label){
      this.start = start;
      this.end = end;
      this.label = label;
    }
  }

  private static class SimpleDocListener implements DocumentListener {
    private final Runnable runnable;

    SimpleDocListener(Runnable runnable){
      this.runnable = runnable;
    }

    @Override public void insertUpdate(DocumentEvent e){ runnable.run(); }
    @Override public void removeUpdate(DocumentEvent e){ runnable.run(); }
    @Override public void changedUpdate(DocumentEvent e){ runnable.run(); }
  }
}
