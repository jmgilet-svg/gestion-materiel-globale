package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.agency.AgencyContext;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.service.PlanningService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Compact read-only planning view showing interventions for a single day.
 */
public class MiniPlanningPanel extends JPanel {
  private static final DateTimeFormatter DAY_FORMAT =
      DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm");

  private final PlanningService planningService;
  private final DefaultListModel<Entry> model = new DefaultListModel<>();
  private final JList<Entry> list = new JList<>(model);
  private final JLabel title = new JLabel();
  private UUID highlightedId;
  private LocalDate referenceDay;

  public MiniPlanningPanel(PlanningService planningService){
    super(new BorderLayout(8, 8));
    this.planningService = planningService;
    title.setBorder(new EmptyBorder(4, 4, 4, 4));
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    list.setFocusable(false);
    list.setCellRenderer(new CellRenderer());
    list.setVisibleRowCount(18);
    add(title, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
    showForDay(LocalDate.now(), null);
  }

  public void showForIntervention(Intervention intervention){
    LocalDate day = intervention != null && intervention.getDateHeureDebut() != null
        ? intervention.getDateHeureDebut().toLocalDate()
        : LocalDate.now();
    showForDay(day, intervention);
  }

  public void showForDay(LocalDate day, Intervention highlight){
    referenceDay = day != null ? day : LocalDate.now();
    highlightedId = highlight != null ? highlight.getId() : null;
    title.setText("Planning du " + DAY_FORMAT.format(referenceDay));
    model.clear();
    if (planningService == null){
      model.addElement(new Entry("Service planning indisponible", false));
      return;
    }
    try {
      List<Intervention> interventions = planningService.listInterventions(referenceDay, referenceDay);
      if (interventions != null){
        List<Intervention> filtered = filterByAgency(interventions);
        filtered.sort(Comparator.comparing(Intervention::getDateHeureDebut,
            Comparator.nullsLast(Comparator.naturalOrder())));
        for (Intervention it : filtered){
          model.addElement(new Entry(formatLine(it), isHighlighted(it)));
        }
      }
      if (model.isEmpty()){
        model.addElement(new Entry("Aucune intervention pour ce jour.", false));
      }
    } catch (Exception ex){
      model.addElement(new Entry("Impossible de charger le planning : " + ex.getMessage(), false));
    }
  }

  private List<Intervention> filterByAgency(List<Intervention> interventions){
    if (interventions == null || interventions.isEmpty()){
      return List.of();
    }
    List<Intervention> filtered = new ArrayList<>();
    for (Intervention intervention : interventions){
      if (intervention == null){
        continue;
      }
      if (AgencyContext.matchesCurrentAgency(intervention)){
        filtered.add(intervention);
      }
    }
    return filtered.isEmpty() ? List.of() : List.copyOf(filtered);
  }

  private boolean isHighlighted(Intervention intervention){
    if (intervention == null){
      return false;
    }
    UUID id = intervention.getId();
    return id != null && id.equals(highlightedId);
  }

  private String formatLine(Intervention intervention){
    if (intervention == null){
      return "Intervention";
    }
    LocalDateTime start = intervention.getDateHeureDebut();
    LocalDateTime end = intervention.getDateHeureFin();
    String startStr = start != null ? TIME_FORMAT.format(start) : "--";
    String endStr = end != null ? TIME_FORMAT.format(end) : "--";
    String client = intervention.getClientName() != null ? intervention.getClientName() : "";
    String label = intervention.getLabel() != null ? intervention.getLabel() : "";
    StringBuilder sb = new StringBuilder();
    sb.append(startStr).append(" - ").append(endStr).append("  |");
    if (!client.isBlank()){
      sb.append(' ').append(client);
    }
    if (!label.isBlank()){
      if (!client.isBlank()){
        sb.append(" — ");
      } else {
        sb.append(' ');
      }
      sb.append(label);
    }
    return sb.toString();
  }

  private static final class Entry {
    final String text;
    final boolean highlight;

    Entry(String text, boolean highlight){
      this.text = text;
      this.highlight = highlight;
    }
  }

  private static final class CellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus){
      JLabel label = (JLabel) super.getListCellRendererComponent(list,
          value instanceof Entry entry ? entry.text : String.valueOf(value),
          index,
          isSelected,
          cellHasFocus);
      if (!isSelected && value instanceof Entry entry && entry.highlight){
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(new Color(0x0D47A1));
      }
      return label;
    }
  }
}
