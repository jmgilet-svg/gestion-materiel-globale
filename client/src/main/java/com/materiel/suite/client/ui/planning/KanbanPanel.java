package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.ui.common.EmptyState;
import com.materiel.suite.client.ui.common.Toasts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Kanban pipeline Intervention → Devis → Facturation. */
public class KanbanPanel extends JPanel {
  public interface Listener {
    void onOpen(Intervention intervention);
    void onGenerateQuote(Intervention intervention);
    void onCreateInvoice(Intervention intervention);
  }

  private final Column todo = new Column("À planifier");
  private final Column toQuote = new Column("À deviser");
  private final Column quoted = new Column("Devisé");
  private final Column invoiced = new Column("Facturé");
  private final JPanel board = new JPanel(new GridLayout(1, 0, 12, 0));
  private final JPanel emptyContainer = new JPanel(new BorderLayout());
  private final CardLayout layout;
  private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM");

  private Listener listener;
  private List<Intervention> source = List.of();
  private boolean showingError;

  public KanbanPanel(){
    super(new CardLayout());
    layout = (CardLayout) getLayout();
    setOpaque(true);
    setBackground(new Color(0xF5F5F5));

    board.setOpaque(false);
    board.setBorder(new EmptyBorder(12, 12, 12, 12));
    board.add(wrap(todo));
    board.add(wrap(toQuote));
    board.add(wrap(quoted));
    board.add(wrap(invoiced));

    emptyContainer.setOpaque(false);

    add(board, "board");
    add(emptyContainer, "empty");

    showPlaceholder();
  }

  public void setListener(Listener listener){
    this.listener = listener;
  }

  public void setData(List<Intervention> interventions){
    if (interventions == null || interventions.isEmpty()){
      source = List.of();
    } else {
      List<Intervention> safe = new ArrayList<>();
      for (Intervention intervention : interventions){
        if (intervention != null){
          safe.add(intervention);
        }
      }
      source = safe.isEmpty() ? List.of() : List.copyOf(safe);
    }
    showingError = false;
    render();
  }

  public void showError(String title, String subtitle, Runnable retry){
    source = List.of();
    showingError = true;
    String cta = retry == null ? null : "Réessayer";
    showEmptyState(title == null || title.isBlank() ? "Erreur" : title,
        subtitle == null || subtitle.isBlank() ? "Une erreur est survenue." : subtitle,
        cta, retry);
  }

  private void render(){
    for (Column column : List.of(todo, toQuote, quoted, invoiced)){
      column.removeAll();
    }
    if (source.isEmpty()){
      showPlaceholder();
      return;
    }
    for (Intervention intervention : source){
      if (intervention == null){
        continue;
      }
      Column target = resolveColumn(intervention);
      target.addCard(makeCard(intervention));
    }
    for (Column column : List.of(todo, toQuote, quoted, invoiced)){
      if (column.getComponentCount() == 0){
        column.addPlaceholder();
      }
      column.add(Box.createVerticalGlue());
    }
    layout.show(this, "board");
    revalidate();
    repaint();
  }

  private Column resolveColumn(Intervention intervention){
    if (intervention == null){
      return todo;
    }
    if (isInvoiced(intervention)){
      return invoiced;
    }
    if (intervention.hasQuote()){
      return quoted;
    }
    if (hasScheduledDates(intervention)){
      return toQuote;
    }
    return todo;
  }

  private boolean hasScheduledDates(Intervention intervention){
    return intervention.getDateHeureDebut() != null
        || intervention.getDateHeureFin() != null
        || intervention.getDateDebut() != null
        || intervention.getDateFin() != null;
  }

  private JPanel makeCard(Intervention intervention){
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(true);
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0xE0E0E0)),
        new EmptyBorder(8, 8, 8, 8)));

    String client = clean(intervention.getClientName());
    String label = clean(intervention.getLabel());
    if (client.isBlank() && !label.isBlank()){
      client = label;
      label = "";
    }
    if (client.isBlank()){
      client = "Intervention";
    }
    StringBuilder header = new StringBuilder("<html><b>")
        .append(escape(client))
        .append("</b>");
    if (!label.isBlank()){
      header.append("<br/>").append(escape(label));
    }
    header.append("</html>");
    JLabel title = new JLabel(header.toString());
    panel.add(title, BorderLayout.NORTH);

    JPanel center = new JPanel();
    center.setOpaque(false);
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

    JLabel timing = new JLabel(periodLabel(intervention));
    timing.setForeground(new Color(0x616161));
    timing.setAlignmentX(Component.LEFT_ALIGNMENT);
    center.add(timing);

    String meta = metadata(intervention);
    if (!meta.isBlank()){
      JLabel metaLabel = new JLabel(meta);
      metaLabel.setForeground(new Color(0x424242));
      metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      center.add(Box.createVerticalStrut(4));
      center.add(metaLabel);
    }

    panel.add(center, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    actions.setOpaque(false);
    actions.setBorder(new EmptyBorder(8, 0, 0, 0));

    JButton open = new JButton("Ouvrir");
    open.addActionListener(e -> {
      if (listener != null){
        listener.onOpen(intervention);
      } else {
        Toasts.info(this, "Action Ouvrir non disponible");
      }
    });

    JButton quote = new JButton("Devis");
    quote.setEnabled(!intervention.hasQuote());
    if (intervention.hasQuote()){
      quote.setToolTipText("Un devis est déjà associé à cette intervention.");
    }
    quote.addActionListener(e -> {
      if (listener != null){
        listener.onGenerateQuote(intervention);
      } else {
        Toasts.info(this, "Action Devis non disponible");
      }
    });

    JButton invoice = new JButton("Facture");
    boolean canInvoice = intervention.hasQuote() && !isInvoiced(intervention);
    invoice.setEnabled(canInvoice);
    if (!intervention.hasQuote()){
      invoice.setToolTipText("Générez un devis avant de créer la facture.");
    } else if (isInvoiced(intervention)){
      invoice.setToolTipText("Une facture est déjà associée à cette intervention.");
    }
    invoice.addActionListener(e -> {
      if (listener != null){
        listener.onCreateInvoice(intervention);
      } else {
        Toasts.info(this, "Action Facture non disponible");
      }
    });

    actions.add(open);
    actions.add(quote);
    actions.add(invoice);
    panel.add(actions, BorderLayout.SOUTH);

    return panel;
  }

  private String metadata(Intervention intervention){
    List<String> parts = new ArrayList<>();
    String quote = clean(intervention.getQuoteReference());
    if (quote.isBlank()){
      quote = clean(intervention.getQuoteNumber());
    }
    if (!quote.isBlank()){
      parts.add("Devis : " + quote);
    }
    String invoice = clean(intervention.getInvoiceNumber());
    if (!invoice.isBlank()){
      parts.add("Facture : " + invoice);
    }
    return String.join(" • ", parts);
  }

  private boolean isInvoiced(Intervention intervention){
    String invoiceNumber = intervention.getInvoiceNumber();
    return invoiceNumber != null && !invoiceNumber.isBlank();
  }

  private String periodLabel(Intervention intervention){
    LocalDateTime start = intervention.getDateHeureDebut();
    LocalDateTime end = intervention.getDateHeureFin();
    if (start != null || end != null){
      return formatDateTime(start) + " → " + formatDateTime(end);
    }
    LocalDate startDay = intervention.getDateDebut();
    LocalDate endDay = intervention.getDateFin();
    if (startDay != null || endDay != null){
      return formatDay(startDay) + " → " + formatDay(endDay);
    }
    return "Période non planifiée";
  }

  private String formatDateTime(LocalDateTime value){
    return value == null ? "--" : dateTimeFormat.format(value);
  }

  private String formatDay(LocalDate value){
    return value == null ? "--" : dayFormat.format(value);
  }

  private void showPlaceholder(){
    if (showingError){
      return;
    }
    showEmptyState("Aucune intervention sur la période",
        "Ajustez les filtres de dates pour en afficher davantage.",
        null, null);
  }

  private void showEmptyState(String title, String subtitle, String ctaLabel, Runnable action){
    emptyContainer.removeAll();
    emptyContainer.add(new EmptyState(title, subtitle, ctaLabel, action), BorderLayout.CENTER);
    layout.show(this, "empty");
    revalidate();
    repaint();
  }

  private JPanel wrap(Column column){
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(false);

    JLabel header = new JLabel(column.title, SwingConstants.CENTER);
    header.setBorder(new EmptyBorder(4, 4, 8, 4));
    wrapper.add(header, BorderLayout.NORTH);

    JScrollPane scroll = new JScrollPane(column);
    scroll.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0xD0D0D0)),
        new EmptyBorder(4, 4, 4, 4)));
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getViewport().setBackground(Color.WHITE);
    wrapper.add(scroll, BorderLayout.CENTER);

    return wrapper;
  }

  private static String clean(String value){
    return value == null ? "" : value.strip();
  }

  private static String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private static class Column extends JPanel {
    final String title;

    Column(String title){
      super();
      this.title = title;
      setOpaque(true);
      setBackground(Color.WHITE);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    void addCard(JPanel card){
      if (card == null){
        return;
      }
      card.setAlignmentX(Component.LEFT_ALIGNMENT);
      if (getComponentCount() > 0){
        add(Box.createVerticalStrut(8));
      }
      add(card);
    }

    void addPlaceholder(){
      JLabel label = new JLabel("—", SwingConstants.CENTER);
      label.setForeground(new Color(0x9E9E9E));
      label.setAlignmentX(Component.CENTER_ALIGNMENT);
      add(label);
    }
  }
}
