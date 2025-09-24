package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.ResourceRef;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tuile d'intervention compacte : résumé + actions rapides. */
public class InterventionTilePanel extends JPanel {
  public interface Listener {
    void onOpen(Intervention intervention);
    void onEdit(Intervention intervention);
    void onMarkDone(Intervention intervention);
  }

  private static final DateTimeFormatter START_FORMAT =
      DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", Locale.FRENCH);
  private static final DateTimeFormatter END_TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

  private final Intervention intervention;
  private final Listener listener;
  private final JLabel statusDot = new JLabel("●");
  private final JLabel statusLabel = new JLabel();
  private final JLabel alertBadge = new JLabel();

  public InterventionTilePanel(Intervention intervention, Listener listener){
    this.intervention = intervention;
    this.listener = listener;
    setLayout(new BorderLayout());
    setOpaque(true);
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(230, 230, 230)),
        new EmptyBorder(12, 14, 12, 14)));

    add(buildHeader(), BorderLayout.NORTH);
    add(buildBody(), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);
  }

  private JComponent buildHeader(){
    JPanel header = new JPanel(new BorderLayout(8, 8));
    header.setOpaque(false);

    String client = nonBlank(intervention != null ? intervention.getClientName() : null, "—");
    String title = nonBlank(intervention != null ? intervention.getLabel() : null, "Intervention");
    JLabel left = new JLabel("<html><b>" + escape(client) + "</b> — " + escape(title) + "</html>");
    left.setOpaque(false);
    header.add(left, BorderLayout.WEST);

    JLabel center = new JLabel(rangeText(intervention));
    center.setOpaque(false);
    header.add(center, BorderLayout.CENTER);

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    right.setOpaque(false);
    Status status = Status.from(intervention != null ? intervention.getStatus() : null);
    statusDot.setForeground(status.color);
    statusLabel.setText(status.icon + " " + status.label);
    statusLabel.setForeground(status.color);
    right.add(statusDot);
    right.add(statusLabel);

    boolean late = isLate(intervention);
    boolean missing = missingResources(intervention);
    if (late || missing){
      alertBadge.setText("  !  ");
      alertBadge.setOpaque(true);
      alertBadge.setBackground(new Color(220, 53, 69));
      alertBadge.setForeground(Color.WHITE);
      alertBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
      if (late && missing){
        alertBadge.setToolTipText("Retard · Ressources manquantes");
      } else if (late){
        alertBadge.setToolTipText("Retard");
      } else {
        alertBadge.setToolTipText("Ressources manquantes");
      }
      right.add(alertBadge);
    }

    header.add(right, BorderLayout.EAST);
    return header;
  }

  private JComponent buildBody(){
    JPanel body = new JPanel(new GridBagLayout());
    body.setOpaque(false);
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets(4, 0, 4, 0);
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1.0;

    body.add(row("Ressources", resourcesLine(intervention)), gc);
    gc.gridy++;
    body.add(row("Adresse", addressLine(intervention)), gc);
    gc.gridy++;
    body.add(row("Infos", infoLine(intervention)), gc);

    return body;
  }

  private JComponent buildFooter(){
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    footer.setOpaque(false);
    JButton open = new JButton("Ouvrir");
    JButton edit = new JButton("Éditer");
    JButton done = new JButton("Terminer");

    open.addActionListener(e -> { if (listener != null){ listener.onOpen(intervention); } });
    edit.addActionListener(e -> { if (listener != null){ listener.onEdit(intervention); } });
    done.addActionListener(e -> { if (listener != null){ listener.onMarkDone(intervention); } });

    Status status = Status.from(intervention != null ? intervention.getStatus() : null);
    if (status == Status.TERMINE || status == Status.ANNULE){
      done.setEnabled(false);
    }

    footer.add(open);
    footer.add(edit);
    footer.add(done);
    return footer;
  }

  /* ---------- helpers ---------- */

  private JPanel row(String label, String valueHtml){
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    panel.setOpaque(false);
    JLabel l = new JLabel("<html><span style='color:#666666'>" + escape(label) + "</span>: " + valueHtml + "</html>");
    panel.add(l);
    return panel;
  }

  private String resourcesLine(Intervention it){
    if (it == null){
      return "<i>Aucune</i>";
    }
    List<ResourceRef> list = it.getResources();
    if (list == null || list.isEmpty()){
      return "<i>Aucune</i>";
    }
    List<String> parts = new ArrayList<>();
    for (ResourceRef ref : list){
      if (ref == null){
        continue;
      }
      String name = nonBlank(ref.getName(), "Ressource");
      String icon = iconFor(ref);
      parts.add(icon + " " + escape(name));
    }
    return String.join("  •  ", parts);
  }

  private String addressLine(Intervention it){
    String address = it != null ? it.getAddress() : null;
    if (address == null || address.isBlank()){
      address = it != null ? it.getSiteLabel() : null;
    }
    return "📍 " + escape(nonBlank(address, "—"));
  }

  private String infoLine(Intervention it){
    String duration = plannedDuration(it);
    String type = "—";
    if (it != null){
      InterventionType interventionType = it.getType();
      if (interventionType != null){
        type = nonBlank(interventionType.getLabel(), interventionType.getCode());
      }
    }
    String responsible = it != null ? nonBlank(it.getDriverName(), it.getCraneName(), it.getTruckName(), "—") : "—";
    return "⏱ " + escape(duration)
        + "   •   🧾 " + escape(type)
        + "   •   👤 " + escape(responsible);
  }

  private String rangeText(Intervention it){
    if (it == null){
      return "?";
    }
    LocalDateTime start = effectiveStart(it);
    LocalDateTime end = effectiveEnd(it);
    if (start == null){
      return "?";
    }
    String startText = START_FORMAT.format(start);
    if (end == null){
      return startText;
    }
    if (end.toLocalDate().equals(start.toLocalDate())){
      return startText + " → " + END_TIME_FORMAT.format(end);
    }
    return startText + " → " + START_FORMAT.format(end);
  }

  private String plannedDuration(Intervention it){
    LocalDateTime start = effectiveStart(it);
    LocalDateTime end = effectiveEnd(it);
    if (start == null || end == null){
      return "—";
    }
    long minutes = Duration.between(start, end).toMinutes();
    if (minutes <= 0){
      return "—";
    }
    long hours = minutes / 60;
    long mins = minutes % 60;
    if (hours > 0){
      return hours + "h" + String.format(Locale.ROOT, "%02d", mins);
    }
    return mins + " min";
  }

  private static LocalDateTime effectiveStart(Intervention it){
    if (it == null){
      return null;
    }
    LocalDateTime value = it.getDateHeureDebut();
    return value != null ? value : it.getStartDateTime();
  }

  private static LocalDateTime effectiveEnd(Intervention it){
    if (it == null){
      return null;
    }
    LocalDateTime value = it.getDateHeureFin();
    return value != null ? value : it.getEndDateTime();
  }

  private static boolean missingResources(Intervention it){
    if (it == null){
      return true;
    }
    List<ResourceRef> list = it.getResources();
    return list == null || list.isEmpty();
  }

  private static boolean isLate(Intervention it){
    if (it == null){
      return false;
    }
    Status status = Status.from(it.getStatus());
    if (status == Status.TERMINE || status == Status.ANNULE){
      return false;
    }
    LocalDateTime end = effectiveEnd(it);
    if (end == null){
      return false;
    }
    return end.isBefore(LocalDateTime.now());
  }

  private static String iconFor(ResourceRef ref){
    if (ref == null){
      return "🔧";
    }
    String iconKey = nonBlank(ref.getIcon(), null);
    if (iconKey != null){
      return iconForKey(iconKey);
    }
    String name = nonBlank(ref.getName(), null);
    if (name != null){
      return iconForKey(name);
    }
    return "🔧";
  }

  private static String iconForKey(String value){
    if (value == null){
      return "🔧";
    }
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.contains("grue") || lower.contains("crane")){
      return "🏗️";
    }
    if (lower.contains("truck") || lower.contains("camion")){
      return "🚚";
    }
    if (lower.contains("driver") || lower.contains("chauffeur")
        || lower.contains("tech") || lower.contains("operator")
        || lower.contains("manut") || lower.contains("crew")){
      return "👷";
    }
    if (lower.contains("nacelle") || lower.contains("lift")){
      return "🛠️";
    }
    return "🔧";
  }

  private static String escape(String text){
    if (text == null){
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static String nonBlank(String value, String fallback){
    if (value == null || value.isBlank()){
      return fallback;
    }
    return value;
  }

  private static String nonBlank(String first, String second, String third, String fallback){
    String value = nonBlank(first, null);
    if (value != null){
      return value;
    }
    value = nonBlank(second, null);
    if (value != null){
      return value;
    }
    value = nonBlank(third, null);
    return value != null ? value : fallback;
  }

  private enum Status {
    PLANIFIE("Planifié", "🔵", new Color(30, 144, 255)),
    BROUILLON("Brouillon", "🟡", new Color(255, 165, 0)),
    TERMINE("Terminé", "🟢", new Color(40, 167, 69)),
    ANNULE("Annulé", "🔴", new Color(220, 53, 69)),
    ENCOURS("En cours", "🔵", new Color(0, 123, 255));

    final String label;
    final String icon;
    final Color color;

    Status(String label, String icon, Color color){
      this.label = label;
      this.icon = icon;
      this.color = color;
    }

    static Status from(String value){
      if (value == null){
        return PLANIFIE;
      }
      String normalized = value.trim().toUpperCase(Locale.ROOT)
          .replace('-', '_')
          .replace('É', 'E')
          .replace('È', 'E')
          .replace('Ê', 'E');
      if (normalized.contains("TERMINE") || normalized.contains("DONE")
          || normalized.contains("COMPLET")){
        return TERMINE;
      }
      if (normalized.contains("ANNUL") || normalized.contains("CANCEL")){
        return ANNULE;
      }
      if (normalized.contains("BROU") || normalized.contains("DRAFT")){
        return BROUILLON;
      }
      if (normalized.contains("COURS") || normalized.contains("PROGRESS")
          || normalized.contains("RUN")){
        return ENCOURS;
      }
      return PLANIFIE;
    }
  }
}
