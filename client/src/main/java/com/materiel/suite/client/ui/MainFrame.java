package com.materiel.suite.client.ui;

import com.materiel.suite.client.config.AppConfig;
import com.materiel.suite.client.ui.delivery.DeliveryNotesPanel;
import com.materiel.suite.client.ui.invoices.InvoicesPanel;
import com.materiel.suite.client.ui.orders.OrdersPanel;
import com.materiel.suite.client.ui.quotes.QuotesPanel;
import com.materiel.suite.client.ui.planning.PlanningPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {
  private final CardLayout cards = new CardLayout();
  private final JPanel center = new JPanel(cards);

  public MainFrame(AppConfig cfg) {
    super("Gestion Matériel — Suite");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(1080, 720);

    setLayout(new BorderLayout());
    add(buildHeader(cfg), BorderLayout.NORTH);
    add(buildSidebar(), BorderLayout.WEST);
    add(center, BorderLayout.CENTER);

    center.add(new PlanningPanel(), "planning");
    center.add(new QuotesPanel(), "quotes");
    center.add(new OrdersPanel(), "orders");
    center.add(new DeliveryNotesPanel(), "delivery");
    center.add(new InvoicesPanel(), "invoices");
    center.add(new JLabel("Clients (à implémenter)"), "customers");
    center.add(new JLabel("Ressources (à implémenter)"), "resources");

    cards.show(center, "quotes");
  }

  private JComponent buildHeader(AppConfig cfg) {
    JPanel p = new JPanel(new BorderLayout());
    p.setBorder(new EmptyBorder(8,8,8,8));
    JLabel title = new JLabel("Gestion Matériel — Mode " + (cfg.getMode()==null?"(non défini)":cfg.getMode().name()));
    title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
    p.add(title, BorderLayout.WEST);
    return p;
  }

  private JComponent buildSidebar() {
    JPanel side = new JPanel();
    side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
    side.setBorder(new EmptyBorder(8,8,8,8));
    side.setPreferredSize(new Dimension(220, 0));
    side.add(navButton("Planning", "planning"));
    side.add(Box.createVerticalStrut(6));
    side.add(navButton("Devis", "quotes"));
    side.add(Box.createVerticalStrut(6));
    side.add(navButton("Commandes", "orders"));
    side.add(Box.createVerticalStrut(6));
    side.add(navButton("Bons de livraison", "delivery"));
    side.add(Box.createVerticalStrut(6));
    side.add(navButton("Factures", "invoices"));
    side.add(Box.createVerticalStrut(6));
    side.add(navButton("Clients", "customers"));
    side.add(Box.createVerticalStrut(6));
    side.add(navButton("Ressources", "resources"));
    side.add(Box.createVerticalGlue());
    return side;
  }

  private JButton navButton(String label, String card) {
    JButton b = new JButton(label);
    b.putClientProperty("JButton.buttonType", "roundRect");
    b.addActionListener(e -> cards.show(center, card));
    return b;
  }
}
