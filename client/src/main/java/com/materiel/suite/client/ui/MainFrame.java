package com.materiel.suite.client.ui;

import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.net.ServiceFactory;
import com.materiel.suite.client.service.SyncService;
import com.materiel.suite.client.ui.delivery.DeliveryNotesPanel;
import com.materiel.suite.client.ui.invoices.InvoicesPanel;
import com.materiel.suite.client.ui.orders.OrdersPanel;
import com.materiel.suite.client.ui.quotes.QuotesPanel;
import com.materiel.suite.client.ui.planning.PlanningPanel;
import com.materiel.suite.client.ui.planning.agenda.AgendaPanel;
import com.materiel.suite.client.ui.resources.ResourcesPanel;
import com.materiel.suite.client.ui.crm.ClientsPanel;
import com.materiel.suite.client.ui.theme.ThemeManager;
import com.materiel.suite.client.ui.commands.CommandBus;
import com.materiel.suite.client.ui.shell.CollapsibleSidebar;
import com.materiel.suite.client.ui.shell.SidebarButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
  private final CardLayout cards = new CardLayout();
  private final JPanel center = new JPanel(cards);
  private final JLabel status = new JLabel("Prêt.");
  private javax.swing.Timer syncTimer;
  private final Map<String, SidebarButton> navButtons = new LinkedHashMap<>();

  public MainFrame(AppConfig cfg) {
    super("Gestion Matériel — Suite");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

    openCard("quotes");

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
    JPanel p = new JPanel(new BorderLayout());
    p.setBorder(new EmptyBorder(8,8,8,8));
    JLabel title = new JLabel("Gestion Matériel — Mode " + (cfg.getMode()==null?"(non défini)":cfg.getMode()));
    title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
    p.add(title, BorderLayout.WEST);
    return p;
  }

  private JComponent buildSidebar() {
    CollapsibleSidebar side = new CollapsibleSidebar();
    addSidebarItem(side, "planning", "🗓", "Planning");
    addSidebarItem(side, "agenda", "📒", "Agenda");
    addSidebarItem(side, "quotes", "🧾", "Devis");
    addSidebarItem(side, "orders", "📦", "Commandes");
    addSidebarItem(side, "delivery", "🚚", "Bons de livraison");
    addSidebarItem(side, "invoices", "💶", "Factures");
    addSidebarItem(side, "clients", "👥", "Clients");
    addSidebarItem(side, "resources", "🏷️", "Ressources");
    return side;
  }

  private void addSidebarItem(CollapsibleSidebar side, String card, String icon, String label) {
    SidebarButton button = side.addItem(icon, label, () -> openCard(card));
    navButtons.put(card, button);
  }

  /** Navigation inter-panneaux depuis menus contextuels */
  public void openCard(String key){
    cards.show(center, key);
    navButtons.forEach((card, button) -> button.setActive(card.equals(key)));
  }
}

