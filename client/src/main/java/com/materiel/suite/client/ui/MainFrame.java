package com.materiel.suite.client.ui;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.agency.AgencyPickerDialog;
import com.materiel.suite.client.auth.AccessControl;
import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.auth.AuthContext;
import com.materiel.suite.client.auth.SessionManager;
import com.materiel.suite.client.auth.User;
import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.service.SyncService;
import com.materiel.suite.client.ui.commands.CommandBus;
import com.materiel.suite.client.ui.crm.ClientsPanel;
import com.materiel.suite.client.ui.delivery.DeliveryNotesPanel;
import com.materiel.suite.client.ui.invoices.InvoicesPanel;
import com.materiel.suite.client.ui.auth.LoginDialog;
import com.materiel.suite.client.ui.common.AutosaveIndicator;
import com.materiel.suite.client.ui.common.CommandPalette;
import com.materiel.suite.client.ui.common.KeymapUtil;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.ui.orders.OrdersPanel;
import com.materiel.suite.client.ui.planning.PlanningPanel;
import com.materiel.suite.client.ui.planning.agenda.AgendaPanel;
import com.materiel.suite.client.ui.quotes.QuotesPanel;
import com.materiel.suite.client.ui.resources.ResourcesPanel;
import com.materiel.suite.client.ui.settings.SettingsPanel;
import com.materiel.suite.client.ui.shell.CollapsibleSidebar;
import com.materiel.suite.client.ui.shell.SidebarButton;
import com.materiel.suite.client.ui.icons.IconRegistry;
import com.materiel.suite.client.ui.users.ChangePasswordDialog;
import com.materiel.suite.client.ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MainFrame extends JFrame implements SessionManager.SessionAware {
  private final CardLayout cards = new CardLayout();
  private final JPanel center = new JPanel(cards);
  private final JLabel status = new JLabel("Prêt.");
  private javax.swing.Timer syncTimer;
  private final Map<String, SidebarButton> navButtons = new LinkedHashMap<>();
  private CollapsibleSidebar sidebar;
  private final CommandPalette commandPalette;
  private final AutosaveIndicator autosaveIndicator = new AutosaveIndicator();
  private String currentCard;
  private JLabel userLabel;
  private JLabel agencyBadge;
  private JButton changePasswordButton;
  private JButton logoutButton;
  private JButton changeAgencyButton;

  public MainFrame(AppConfig cfg) {
    super("Gestion Matériel — Suite");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    commandPalette = new CommandPalette(this);
    ThemeManager.applyInitial();
    setSize(1080, 720);

    setLayout(new BorderLayout());
    add(buildHeader(cfg), BorderLayout.NORTH);
    add(buildSidebar(), BorderLayout.WEST);
    add(center, BorderLayout.CENTER);
    add(status, BorderLayout.SOUTH);
    setJMenuBar(buildMenuBar());

    center.add(new PlanningPanel(), "planning");
    center.add(new AgendaPanel(), "agenda");
    center.add(new QuotesPanel(), "quotes");
    center.add(new OrdersPanel(), "orders");
    center.add(new DeliveryNotesPanel(), "delivery");
    center.add(new InvoicesPanel(), "invoices");
    center.add(new ResourcesPanel(), "resources");
    center.add(new ClientsPanel(), "clients");
    center.add(new SettingsPanel(), "settings");

    applyNavigationPolicy();

    if (ServiceFactory.http()!=null){
      SyncService sync = new SyncService(ServiceFactory.http());
      syncTimer = new javax.swing.Timer(4000, e -> {
        var events = sync.pull();
        if (!events.isEmpty()){
          status.setText("Sync: "+events.size()+" événement(s)");
        }
      });
      syncTimer.start();
    }

    SessionManager.install(this);
    updateSessionInfo();
    initCommandPalette();
    updateAgencyBadge();
  }

  private JMenuBar buildMenuBar(){
    JMenuBar mb = new JMenuBar();
    JMenu app = new JMenu("Application");
    JMenuItem theme = new JMenuItem("Basculer thème clair/sombre");
    theme.addActionListener(e -> ThemeManager.toggleDark());
    app.add(theme);

    JMenu edit = new JMenu("Édition");
    JMenuItem undo = new JMenuItem("Annuler (Ctrl+Z)");
    JMenuItem redo = new JMenuItem("Rétablir (Ctrl+Y)");
    undo.addActionListener(e -> CommandBus.get().undo());
    redo.addActionListener(e -> CommandBus.get().redo());
    edit.add(undo); edit.add(redo);

    mb.add(app);
    mb.add(edit);
    return mb;
  }

  private JComponent buildHeader(AppConfig cfg) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(8,8,8,8));
    JLabel title = new JLabel("Gestion Matériel — Mode " + (cfg.getMode()==null?"(non défini)":cfg.getMode()));
    title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    left.setOpaque(false);
    left.add(title);
    agencyBadge = new JLabel("Agence : —");
    left.add(agencyBadge);
    changeAgencyButton = new JButton("Changer d'agence");
    changeAgencyButton.addActionListener(e -> openAgencyPicker());
    left.add(changeAgencyButton);
    panel.add(left, BorderLayout.WEST);

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    userLabel = new JLabel();
    userLabel.setIcon(IconRegistry.small("user"));
    changePasswordButton = new JButton("Mot de passe…", IconRegistry.small("lock"));
    changePasswordButton.addActionListener(e -> {
      if (AccessControl.canChangeOwnPassword()){
        new ChangePasswordDialog(this).setVisible(true);
      }
    });
    logoutButton = new JButton("Déconnexion", IconRegistry.small("lock"));
    logoutButton.addActionListener(e -> doLogout());
    right.add(autosaveIndicator);
    right.add(userLabel);
    right.add(changePasswordButton);
    right.add(logoutButton);
    panel.add(right, BorderLayout.EAST);
    return panel;
  }

  private void openAgencyPicker(){
    List<Agency> agencies = discoverAgencies();
    if (agencies.isEmpty()){
      Toasts.info(this, "Aucune agence disponible");
      return;
    }
    AgencyPickerDialog dialog = new AgencyPickerDialog(this, agencies);
    dialog.setVisible(true);
    Agency selected = dialog.getSelectedAgency();
    if (selected != null){
      updateAgencyBadge();
      applyNavigationPolicy();
      reloadAfterAgencyChange();
      status.setText("Agence active : " + agencyBadge.getText().replace("Agence : ", ""));
    }
  }

  private List<Agency> discoverAgencies(){
    List<Agency> agencies = new ArrayList<>();
    try {
      if (ServiceLocator.auth() != null){
        List<Agency> fetched = ServiceLocator.auth().listAgencies();
        if (fetched != null){
          agencies.addAll(fetched);
        }
      }
    } catch (Exception ignore){
    }
    if (!agencies.isEmpty()){
      return agencies;
    }
    for (String id : AgencyContext.knownAgencyIds()){
      Agency agency = new Agency();
      agency.setId(id);
      agency.setName(id);
      agencies.add(agency);
    }
    if (agencies.isEmpty()){
      String currentId = AgencyContext.agencyId();
      String currentLabel = AgencyContext.agencyLabel();
      if (currentId != null || currentLabel != null){
        Agency agency = new Agency();
        agency.setId(currentId);
        agency.setName(currentLabel != null ? currentLabel : currentId);
        agencies.add(agency);
      }
    }
    return agencies;
  }

  private void updateAgencyBadge(){
    if (agencyBadge == null){
      return;
    }
    String id = AgencyContext.agencyId();
    String label = AgencyContext.agencyLabel();
    StringBuilder text = new StringBuilder("Agence : ");
    if (label != null && !label.isBlank()){
      text.append(label);
      if (id != null && !id.isBlank() && !label.equalsIgnoreCase(id)){
        text.append(" (").append(id).append(')');
      }
    } else if (id != null && !id.isBlank()){
      text.append(id);
    } else {
      text.append('—');
    }
    agencyBadge.setText(text.toString());
    if (changeAgencyButton != null){
      boolean logged = AuthContext.isLogged();
      changeAgencyButton.setEnabled(logged);
      changeAgencyButton.setVisible(logged);
    }
  }

  private void reloadAfterAgencyChange(){
    PlanningPanel planning = visiblePlanningPanel();
    if (planning != null){
      planning.actionReload();
    }
    center.revalidate();
    center.repaint();
  }

  private JComponent buildSidebar() {
    sidebar = new CollapsibleSidebar();
    addSidebarItem(sidebar, "planning", "calendar", "Planning");
    addSidebarItem(sidebar, "agenda", "calendar", "Agenda");
    addSidebarItem(sidebar, "quotes", "file", "Devis");
    addSidebarItem(sidebar, "orders", "pallet", "Commandes");
    addSidebarItem(sidebar, "delivery", "truck", "Bons de livraison");
    addSidebarItem(sidebar, "invoices", "invoice", "Factures");
    addSidebarItem(sidebar, "clients", "user", "Clients");
    addSidebarItem(sidebar, "resources", "wrench", "Ressources");
    addSidebarItem(sidebar, "settings", "settings", "Paramètres");
    return sidebar;
  }

  private void addSidebarItem(CollapsibleSidebar side, String card, String iconKey, String label) {
    SidebarButton button = side.addItemSvg(iconKey, label, () -> openCard(card));
    navButtons.put(card, button);
  }

  /** Navigation inter-panneaux depuis menus contextuels */
  public void openCard(String key){
    SidebarButton targetBtn = navButtons.get(key);
    if (targetBtn != null && !targetBtn.isVisible()){
      return;
    }
    cards.show(center, key);
    navButtons.forEach((card, btn) -> btn.setActive(card.equals(key)));
    currentCard = key;
  }

  private void initCommandPalette(){
    Supplier<List<CommandPalette.Command>> provider = () -> {
      List<CommandPalette.Command> commands = new ArrayList<>();
      commands.add(new CommandPalette.Command("Aller au planning", "Navigation", "", () -> openCard("planning")));
      commands.add(new CommandPalette.Command("Aller à l'agenda", "Navigation", "", () -> openCard("agenda")));
      commands.add(new CommandPalette.Command("Aller aux devis", "Navigation", "", () -> openCard("quotes")));
      commands.add(new CommandPalette.Command("Aller aux commandes", "Navigation", "", () -> openCard("orders")));
      commands.add(new CommandPalette.Command("Aller aux bons de livraison", "Navigation", "", () -> openCard("delivery")));
      commands.add(new CommandPalette.Command("Aller aux factures", "Navigation", "", () -> openCard("invoices")));
      commands.add(new CommandPalette.Command("Aller aux clients", "Navigation", "", () -> openCard("clients")));
      commands.add(new CommandPalette.Command("Aller aux ressources", "Navigation", "", () -> openCard("resources")));
      commands.add(new CommandPalette.Command("Ouvrir les paramètres", "Navigation", "", () -> openCard("settings")));

      PlanningPanel panel = visiblePlanningPanel();
      if (panel != null){
        int selected = Math.max(0, panel.getSelectedCount());
        String selectionLabel = selected == 0
            ? "Aucune intervention sélectionnée"
            : selected + " intervention(s) sélectionnée(s)";
        commands.add(new CommandPalette.Command(
            "Planning : prévisualiser les devis",
            selectionLabel,
            "P",
            panel::actionDryRun));
        commands.add(new CommandPalette.Command(
            "Planning : générer les devis",
            selectionLabel,
            "D",
            panel::actionGenerateQuotes));
        commands.add(new CommandPalette.Command(
            "Planning : basculer le filtre",
            "Filtre actuel : " + panel.getCurrentFilterLabel(),
            "F",
            panel::actionFilterCycle));
        commands.add(new CommandPalette.Command(
            "Planning : recharger",
            "Actualiser les données",
            "R",
            panel::actionReload));
      }
      return commands;
    };

    Supplier<JComponent> help = CommandPalette::defaultHelp;

    KeymapUtil.bindGlobal(getRootPane(), "open-command-palette", KeymapUtil.ctrlK(),
        () -> commandPalette.open(provider, help, ok -> {}));
  }

  public void setSaving(boolean saving){
    autosaveIndicator.setSaving(saving);
  }

  public void markSavedNow(){
    autosaveIndicator.markSavedNow();
  }

  private PlanningPanel visiblePlanningPanel(){
    for (Component component : center.getComponents()){
      if (component instanceof PlanningPanel panel && component.isVisible()){
        return panel;
      }
    }
    return null;
  }

  private void applyNavigationPolicy(){
    boolean planning = AccessControl.canViewPlanning() && AgencyContext.hasFeature("PLANNING");
    setNavVisible("planning", planning);
    setNavVisible("agenda", planning);
    boolean sales = AccessControl.canViewSales() && AgencyContext.hasFeature("SALES");
    setNavVisible("quotes", sales);
    setNavVisible("orders", sales);
    setNavVisible("delivery", sales);
    setNavVisible("invoices", sales);
    setNavVisible("clients", AgencyContext.hasFeature("CLIENTS"));
    setNavVisible("resources", AccessControl.canViewResources() && AgencyContext.hasFeature("RESOURCES"));
    setNavVisible("settings", AccessControl.canViewSettings() && AgencyContext.hasFeature("SETTINGS"));
    if (sidebar != null){
      sidebar.revalidate();
      sidebar.repaint();
    }
    ensureActiveCardVisible();
  }

  private void updateSessionInfo(){
    if (userLabel == null || changePasswordButton == null || logoutButton == null){
      return;
    }
    User current = AuthContext.get();
    if (current == null){
      userLabel.setText("Non connecté");
      changePasswordButton.setVisible(false);
      changePasswordButton.setEnabled(false);
      logoutButton.setEnabled(false);
    } else {
      String role = current.getRole() != null ? current.getRole().name() : "";
      String display = current.getDisplayName() != null ? current.getDisplayName() : current.getUsername();
      userLabel.setText(display + (role.isBlank() ? "" : " (" + role + ")"));
      changePasswordButton.setVisible(AccessControl.canChangeOwnPassword());
      changePasswordButton.setEnabled(AccessControl.canChangeOwnPassword());
      logoutButton.setEnabled(true);
    }
    userLabel.setVisible(true);
    updateAgencyBadge();
  }

  private void doLogout(){
    try {
      if (ServiceLocator.auth() != null){
        ServiceLocator.auth().logout();
      }
    } catch (Exception ignore){
    }
    Toasts.info(this, "Déconnecté");
    LoginDialog.require(this);
    SessionManager.install(this);
    applyNavigationPolicy();
    updateSessionInfo();
  }

  private void setNavVisible(String key, boolean visible){
    SidebarButton button = navButtons.get(key);
    if (button != null){
      button.setVisible(visible);
    }
  }

  private void ensureActiveCardVisible(){
    if (currentCard != null){
      SidebarButton current = navButtons.get(currentCard);
      if (current != null && current.isVisible()){
        return;
      }
    }
    for (Map.Entry<String, SidebarButton> entry : navButtons.entrySet()){
      if (entry.getValue().isVisible()){
        openCard(entry.getKey());
        return;
      }
    }
  }

  @Override
  public void onSessionRefreshed(){
    applyNavigationPolicy();
    updateSessionInfo();
  }
}

