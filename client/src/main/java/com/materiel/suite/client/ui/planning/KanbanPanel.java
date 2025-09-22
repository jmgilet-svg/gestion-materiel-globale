package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.ui.common.EmptyState;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.security.Security;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Kanban pipeline Intervention → Devis → Facturation. */
public class KanbanPanel extends JPanel {
  public interface Listener {
    void onOpen(Intervention intervention);
    void onGenerateQuote(Intervention intervention);
    void onCreateInvoice(Intervention intervention);
    /** Notifié après changement de statut et sauvegarde. */
    default void onStatusChanged(Intervention intervention){}
  }

  public enum Stage {
    TODO("À planifier"),
    TO_QUOTE("À deviser"),
    QUOTED("Devisé"),
    INVOICED("Facturé");

    final String label;

    Stage(String label){
      this.label = label;
    }
  }

  private static final DataFlavor KANBAN_FLAVOR = createFlavor();

  private final KanbanColumn todo = new KanbanColumn(Stage.TODO);
  private final KanbanColumn toQuote = new KanbanColumn(Stage.TO_QUOTE);
  private final KanbanColumn quoted = new KanbanColumn(Stage.QUOTED);
  private final KanbanColumn invoiced = new KanbanColumn(Stage.INVOICED);
  private final List<KanbanColumn> columns;
  private final JPanel board = new JPanel(new GridLayout(1, 0, 12, 0));
  private final JPanel emptyContainer = new JPanel(new BorderLayout());
  private final CardLayout layout;
  private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM");
  private final DragMouseAdapter dragAdapter = new DragMouseAdapter();

  private Listener listener;
  private List<Intervention> source = List.of();
  private boolean showingError;
  private String filter = "";

  public KanbanPanel(){
    super(new CardLayout());
    layout = (CardLayout) getLayout();
    setOpaque(true);
    setBackground(new Color(0xF5F5F5));

    board.setOpaque(false);
    board.setBorder(new EmptyBorder(12, 12, 12, 12));
    board.add(todo.wrap());
    board.add(toQuote.wrap());
    board.add(quoted.wrap());
    board.add(invoiced.wrap());

    columns = List.of(todo, toQuote, quoted, invoiced);

    emptyContainer.setOpaque(false);

    add(board, "board");
    add(emptyContainer, "empty");

    showPlaceholder();
  }

  public void setListener(Listener listener){
    this.listener = listener;
  }

  public void setFilter(String query){
    String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    if (Objects.equals(filter, normalized)){
      return;
    }
    filter = normalized;
    render();
  }

  public void setData(List<Intervention> interventions){
    if (interventions == null || interventions.isEmpty()){
      source = List.of();
    } else {
      List<Intervention> safe = new ArrayList<>();
      for (Intervention intervention : interventions){
        if (intervention != null){
          if (AgencyContext.matchesCurrentAgency(intervention)){
            safe.add(intervention);
          }
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
    for (KanbanColumn column : columns){
      column.reset();
    }
    if (source.isEmpty()){
      showPlaceholder();
      return;
    }
    boolean hasCards = false;
    for (Intervention intervention : source){
      if (intervention == null || !matchesFilter(intervention)){
        continue;
      }
      Stage stage = resolveStage(intervention);
      columnFor(stage).addCard(createCard(intervention, stage));
      hasCards = true;
    }
    for (KanbanColumn column : columns){
      column.finish();
    }
    if (!hasCards && filter.isBlank()){
      showPlaceholder();
      return;
    }
    layout.show(this, "board");
    revalidate();
    repaint();
  }

  private Stage resolveStage(Intervention intervention){
    Stage workflow = stageFromWorkflow(intervention);
    if (workflow != null){
      return workflow;
    }
    if (isInvoiced(intervention)){
      return Stage.INVOICED;
    }
    if (intervention.hasQuote()){
      return Stage.QUOTED;
    }
    if (hasScheduledDates(intervention)){
      return Stage.TO_QUOTE;
    }
    return Stage.TODO;
  }

  private Stage stageFromWorkflow(Intervention intervention){
    if (intervention == null){
      return null;
    }
    String value = clean(intervention.getWorkflowStage());
    if (value.isBlank()){
      return null;
    }
    String normalized = value.toUpperCase(Locale.ROOT);
    try {
      return Stage.valueOf(normalized);
    } catch (IllegalArgumentException ex){
      return null;
    }
  }

  private KanbanColumn columnFor(Stage stage){
    return switch (stage){
      case TODO -> todo;
      case TO_QUOTE -> toQuote;
      case QUOTED -> quoted;
      case INVOICED -> invoiced;
    };
  }

  private boolean hasScheduledDates(Intervention intervention){
    return intervention.getDateHeureDebut() != null
        || intervention.getDateHeureFin() != null
        || intervention.getDateDebut() != null
        || intervention.getDateFin() != null;
  }

  private KanbanCard createCard(Intervention intervention, Stage stage){
    return new KanbanCard(intervention, stage);
  }

  private boolean matchesFilter(Intervention intervention){
    if (filter.isBlank()){
      return true;
    }
    String haystack = (clean(intervention.getClientName()) + " " + clean(intervention.getLabel()))
        .toLowerCase(Locale.ROOT);
    return haystack.contains(filter);
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

  private void applyStageChange(Intervention intervention, Stage from, Stage to){
    if (intervention == null || from == to){
      return;
    }
    String previous = intervention.getWorkflowStage();
    try {
      intervention.setWorkflowStage(to.name());
      if (listener != null){
        listener.onStatusChanged(intervention);
      }
      Toasts.success(this, "Statut mis à jour : " + to.label);
    } catch (Exception ex){
      intervention.setWorkflowStage(previous);
      String message = ex.getMessage();
      Toasts.error(this, message == null || message.isBlank()
          ? "Impossible de mettre à jour le statut." : message);
    }
    render();
  }

  private void denyDropFeedback(Stage target){
    if (target == Stage.INVOICED){
      Toasts.error(this, "Droit insuffisant : seuls ADMIN/SALES peuvent passer en \"Facturé\".");
    } else {
      Toasts.error(this, "Action non autorisée.");
    }
  }

  private static DataFlavor createFlavor(){
    try {
      return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + KanbanTransferData.class.getName());
    } catch (ClassNotFoundException e){
      throw new IllegalStateException(e);
    }
  }

  private void registerDragSource(JComponent component, TransferHandler handler){
    if (component instanceof JButton){
      return;
    }
    component.setTransferHandler(handler);
    component.addMouseListener(dragAdapter);
    component.addMouseMotionListener(dragAdapter);
    if (component instanceof Container){
      for (Component child : component.getComponents()){
        if (child instanceof JComponent jComponent){
          registerDragSource(jComponent, handler);
        }
      }
    }
  }

  private final class KanbanCard extends JPanel {
    private final Intervention intervention;

    KanbanCard(Intervention intervention, Stage stage){
      super(new BorderLayout());
      this.intervention = intervention;
      setOpaque(true);
      setBackground(Color.WHITE);
      setBorder(BorderFactory.createCompoundBorder(
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
      add(title, BorderLayout.NORTH);

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

      add(center, BorderLayout.CENTER);

      JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
      actions.setOpaque(false);
      actions.setBorder(new EmptyBorder(8, 0, 0, 0));

      JButton open = new JButton("Ouvrir");
      open.addActionListener(e -> {
        if (listener != null){
          listener.onOpen(intervention);
        } else {
          Toasts.info(KanbanPanel.this, "Action Ouvrir non disponible");
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
          Toasts.info(KanbanPanel.this, "Action Devis non disponible");
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
          Toasts.info(KanbanPanel.this, "Action Facture non disponible");
        }
      });

      actions.add(open);
      actions.add(quote);
      actions.add(invoice);
      add(actions, BorderLayout.SOUTH);

      CardTransferHandler handler = new CardTransferHandler(intervention, stage);
      registerDragSource(this, handler);
    }
  }

  private final class KanbanColumn {
    private final Stage stage;
    private final JPanel container;
    private final JLabel header;
    private final JPanel wrapper;
    private int cardCount;

    KanbanColumn(Stage stage){
      this.stage = stage;
      container = new JPanel();
      container.setOpaque(true);
      container.setBackground(Color.WHITE);
      container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
      container.setBorder(new EmptyBorder(8, 8, 8, 8));
      ColumnTransferHandler handler = new ColumnTransferHandler(stage);
      container.setTransferHandler(handler);

      header = new JLabel(stage.label, SwingConstants.CENTER);
      header.setBorder(new EmptyBorder(4, 4, 8, 4));

      wrapper = new JPanel(new BorderLayout());
      wrapper.setOpaque(false);
      wrapper.add(header, BorderLayout.NORTH);

      JScrollPane scroll = new JScrollPane(container);
      scroll.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(new Color(0xD0D0D0)),
          new EmptyBorder(4, 4, 4, 4)));
      scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      scroll.getViewport().setBackground(Color.WHITE);
      scroll.setTransferHandler(handler);
      scroll.getViewport().setTransferHandler(handler);
      wrapper.add(scroll, BorderLayout.CENTER);
    }

    JPanel wrap(){
      return wrapper;
    }

    void reset(){
      container.removeAll();
      cardCount = 0;
      updateHeader();
    }

    void addCard(KanbanCard card){
      if (card == null){
        return;
      }
      card.setAlignmentX(Component.LEFT_ALIGNMENT);
      if (container.getComponentCount() > 0){
        container.add(Box.createVerticalStrut(8));
      }
      container.add(card);
      cardCount++;
    }

    void finish(){
      if (cardCount == 0){
        JLabel placeholder = new JLabel("—", SwingConstants.CENTER);
        placeholder.setForeground(new Color(0x9E9E9E));
        placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(placeholder);
      }
      container.add(Box.createVerticalGlue());
      updateHeader();
    }

    private void updateHeader(){
      String text = stage.label + " · " + cardCount;
      if (stage == Stage.INVOICED && !Security.canDropTo(stage)){
        text += "  🔒";
        header.setToolTipText("Accès restreint : seuls ADMIN/SALES peuvent marquer \"Facturé\".");
      } else {
        header.setToolTipText(null);
      }
      header.setText(text);
    }
  }

  private final class ColumnTransferHandler extends TransferHandler {
    private final Stage targetStage;

    ColumnTransferHandler(Stage targetStage){
      this.targetStage = targetStage;
    }

    @Override public boolean canImport(TransferSupport support){
      if (!support.isDataFlavorSupported(KANBAN_FLAVOR)){
        return false;
      }
      if (!Security.canDropTo(targetStage)){
        support.setShowDropLocation(false);
        return false;
      }
      return true;
    }

    @Override public boolean importData(TransferSupport support){
      if (!support.isDrop()){
        return false;
      }
      if (!Security.canDropTo(targetStage)){
        denyDropFeedback(targetStage);
        return false;
      }
      if (!support.isDataFlavorSupported(KANBAN_FLAVOR)){
        return false;
      }
      try {
        KanbanTransferData data = (KanbanTransferData) support.getTransferable().getTransferData(KANBAN_FLAVOR);
        applyStageChange(data.intervention(), data.stage(), targetStage);
        return true;
      } catch (Exception ex){
        return false;
      }
    }
  }

  private final class CardTransferHandler extends TransferHandler {
    private final KanbanTransferData payload;

    CardTransferHandler(Intervention intervention, Stage stage){
      this.payload = new KanbanTransferData(intervention, stage);
    }

    @Override public int getSourceActions(JComponent c){
      return MOVE;
    }

    @Override protected Transferable createTransferable(JComponent c){
      return new KanbanTransferable(payload);
    }
  }

  private record KanbanTransferData(Intervention intervention, Stage stage) {
  }

  private static final class KanbanTransferable implements Transferable {
    private final KanbanTransferData data;

    KanbanTransferable(KanbanTransferData data){
      this.data = data;
    }

    @Override public DataFlavor[] getTransferDataFlavors(){
      return new DataFlavor[]{KANBAN_FLAVOR};
    }

    @Override public boolean isDataFlavorSupported(DataFlavor flavor){
      return KANBAN_FLAVOR.equals(flavor);
    }

    @Override public Object getTransferData(DataFlavor flavor){
      if (!isDataFlavorSupported(flavor)){
        throw new IllegalArgumentException("Unsupported flavor");
      }
      return data;
    }
  }

  private static final class DragMouseAdapter extends MouseAdapter {
    @Override public void mousePressed(MouseEvent e){
      if (!SwingUtilities.isLeftMouseButton(e)){
        return;
      }
      if (e.getComponent() instanceof JComponent component){
        TransferHandler handler = component.getTransferHandler();
        if (handler != null){
          handler.exportAsDrag(component, e, TransferHandler.MOVE);
        }
      }
    }
  }
}
