package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.ResourceRef;
import org.apache.commons.text.StringEscapeUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight panel used in the planning module to display an intervention as a "card".
 * It exposes a listener for common actions (open, edit, mark done, adjust time).
 */
public class InterventionTilePanel extends JPanel {
  public interface Listener {
    void onOpen(Intervention intervention);
    void onEdit(Intervention intervention);
    void onMarkDone(Intervention intervention);
    void onTimeAdjust(Intervention intervention, boolean start, int minutesDelta);
  }

  private static final DateTimeFormatter DAY_FORMAT =
      DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
  private static final DateTimeFormatter DATE_SHORT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.FRENCH);

  private static final Color STATUS_DEFAULT = new Color(0x1F4ED8);
  private static final Color STATUS_DONE = new Color(0x2E7D32);
  private static final Color STATUS_IN_PROGRESS = new Color(0xF59E0B);
  private static final Color STATUS_CANCELLED = new Color(0xB91C1C);

  private final Intervention intervention;
  private final Listener listener;
  private boolean compact;

  public InterventionTilePanel(Intervention intervention, Listener listener){
    this.intervention = intervention;
    this.listener = listener;
    setOpaque(true);
    setBackground(Color.WHITE);
    setLayout(new BorderLayout(0, 8));
    setToolTipText("Ctrl + molette : ajuster début · Maj + molette : ajuster fin");
    buildUI();
    installInteractions();
  }

  private void installInteractions(){
    addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        if (listener != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){
          listener.onOpen(intervention);
        }
      }
    });
    addMouseWheelListener(this::onMouseWheel);
  }

  private void onMouseWheel(MouseWheelEvent event){
    if (listener == null){
      return;
    }
    boolean ctrl = event.isControlDown();
    boolean shift = event.isShiftDown();
    if (!ctrl && !shift){
      return;
    }
    int rotation = event.getWheelRotation();
    if (rotation == 0){
      return;
    }
    int minutes = (rotation > 0 ? 1 : -1) * 15;
    if (ctrl){
      listener.onTimeAdjust(intervention, true, minutes);
      event.consume();
    } else if (shift){
      listener.onTimeAdjust(intervention, false, minutes);
      event.consume();
    }
  }

  private void buildUI(){
    removeAll();
    setBorder(new EmptyBorder(compact ? 6 : 10, compact ? 8 : 12, compact ? 6 : 10, compact ? 8 : 12));
    add(buildHeader(), BorderLayout.NORTH);
    add(buildBody(), BorderLayout.CENTER);
    add(buildFooter(), BorderLayout.SOUTH);
    revalidate();
    repaint();
  }

  private JComponent buildHeader(){
    JPanel header = new JPanel(new BorderLayout(8, 0));
    header.setOpaque(false);

    String client = escape(nonBlank(intervention != null ? intervention.getClientName() : null, "Client"));
    String title = escape(nonBlank(intervention != null ? intervention.getLabel() : null, "Intervention"));
    JLabel label = new JLabel("<html><b>" + client + "</b> — " + title + "</html>");
    label.setFont(label.getFont().deriveFont(Font.PLAIN, adjustFont(label.getFont().getSize2D(), compact ? -0.5f : 0f)));
    header.add(label, BorderLayout.CENTER);

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    right.setOpaque(false);

    JLabel dot = new JLabel("●");
    dot.setForeground(colorForStatus(intervention != null ? intervention.getStatus() : null));
    right.add(dot);

    JLabel status = new JLabel(escape(statusLabel()));
    Font statusFont = status.getFont();
    if (statusFont != null){
      status.setFont(statusFont.deriveFont(Font.BOLD, adjustFont(statusFont.getSize2D(), compact ? -1f : 0f)));
    }
    right.add(status);

    String badgeText = badgeText();
    if (!badgeText.isEmpty()){
      JLabel badge = new JLabel(badgeText);
      badge.setOpaque(true);
      badge.setForeground(new Color(0x0F172A));
      badge.setBackground(new Color(0xE2E8F0));
      badge.setBorder(new EmptyBorder(1, 6, 1, 6));
      right.add(badge);
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
    gc.insets = new Insets(compact ? 2 : 4, 0, compact ? 2 : 4, 0);

    body.add(row("Date", formatDateRange()), gc);
    gc.gridy++;
    body.add(row("Horaire", formatTimeRange()), gc);
    gc.gridy++;
    body.add(row("Ressources", formatResources()), gc);
    gc.gridy++;
    body.add(row("Infos", formatInfos()), gc);

    return body;
  }

  private JPanel buildFooter(){
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    footer.setOpaque(false);

    JButton open = new JButton("Ouvrir");
    JButton edit = new JButton("Éditer");
    JButton done = new JButton("Terminer");

    if (compact){
      Dimension dim = new Dimension(80, 24);
      open.setPreferredSize(dim);
      edit.setPreferredSize(dim);
      done.setPreferredSize(new Dimension(90, 24));
      float size = adjustFont(open.getFont().getSize2D(), -1f);
      open.setFont(open.getFont().deriveFont(size));
      edit.setFont(edit.getFont().deriveFont(size));
      done.setFont(done.getFont().deriveFont(size));
    }

    open.addActionListener(e -> {
      if (listener != null){
        listener.onOpen(intervention);
      }
    });
    edit.addActionListener(e -> {
      if (listener != null){
        listener.onEdit(intervention);
      }
    });
    done.addActionListener(e -> {
      if (listener != null){
        listener.onMarkDone(intervention);
      }
    });

    footer.add(open);
    footer.add(edit);
    footer.add(done);
    return footer;
  }

  private JPanel row(String label, String valueHtml){
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    row.setOpaque(false);
    String escapedLabel = escape(label);
    JLabel l = new JLabel("<html><span style='color:#666666'>" + escapedLabel + "</span>: " + valueHtml + "</html>");
    if (compact){
      l.setFont(l.getFont().deriveFont(adjustFont(l.getFont().getSize2D(), -1f)));
    }
    row.add(l);
    return row;
  }

  private String formatDateRange(){
    LocalDate start = toLocalDate(intervention != null ? intervention.getDateHeureDebut() : null);
    LocalDate end = toLocalDate(intervention != null ? intervention.getDateHeureFin() : null);
    if (start == null && end == null){
      return "<i>—</i>";
    }
    if (start != null && end != null && start.equals(end)){
      return escape(DAY_FORMAT.format(start));
    }
    String from = start != null ? escape(DATE_SHORT.format(start)) : "—";
    String to = end != null ? escape(DATE_SHORT.format(end)) : "—";
    return from + " → " + to;
  }

  private String formatTimeRange(){
    LocalDateTime start = intervention != null ? intervention.getDateHeureDebut() : null;
    LocalDateTime end = intervention != null ? intervention.getDateHeureFin() : null;
    if (start == null && end == null){
      return "<i>—</i>";
    }
    if (start != null && end != null){
      return escape(start.format(DATE_TIME)) + " → " + escape(end.format(DATE_TIME));
    }
    if (start != null){
      return escape(start.format(DATE_TIME));
    }
    return escape(end.format(DATE_TIME));
  }

  private String formatResources(){
    if (intervention == null){
      return "<i>Aucune</i>";
    }
    List<ResourceRef> refs = intervention.getResources();
    if (refs == null || refs.isEmpty()){
      return "<i>Aucune</i>";
    }
    List<String> parts = new ArrayList<>();
    for (ResourceRef ref : refs){
      if (ref == null){
        continue;
      }
      String label = escape(nonBlank(ref.getName(), "Ressource"));
      Icon icon = IconUtil.colored(nonBlank(ref.getIcon(), label), compact ? 14 : 16);
      if (icon != null){
        parts.add(img(icon) + " " + label);
      } else {
        parts.add("🔧 " + label);
      }
    }
    return String.join("  •  ", parts);
  }

  private String formatInfos(){
    List<String> parts = new ArrayList<>();
    parts.add("⏱ " + escape(formatDuration()));
    InterventionType type = intervention != null ? intervention.getType() : null;
    if (type != null){
      String label = nonBlank(type.getLabel(), type.getCode());
      if (!label.isEmpty()){
        parts.add("🧾 " + escape(label));
      }
    }
    String agency = intervention != null ? nonBlank(intervention.getAgency(), intervention.getAgencyId()) : "";
    if (!agency.isEmpty()){
      parts.add("🏢 " + escape(agency));
    }
    return String.join("   •   ", parts);
  }

  private String formatDuration(){
    LocalDateTime start = intervention != null ? intervention.getDateHeureDebut() : null;
    LocalDateTime end = intervention != null ? intervention.getDateHeureFin() : null;
    if (start == null || end == null){
      return "Durée inconnue";
    }
    long minutes = Duration.between(start, end).toMinutes();
    if (minutes <= 0){
      return "< 15 min";
    }
    long hours = minutes / 60;
    long rest = minutes % 60;
    if (hours > 0 && rest > 0){
      return hours + "h" + String.format(Locale.FRENCH, "%02d", rest);
    }
    if (hours > 0){
      return hours + "h";
    }
    return minutes + " min";
  }

  private String statusLabel(){
    if (intervention == null){
      return "Planifiée";
    }
    String status = intervention.getStatus();
    if (status != null && !status.isBlank()){
      return status;
    }
    return "Planifiée";
  }

  private Color colorForStatus(String status){
    if (status == null){
      return STATUS_DEFAULT;
    }
    String value = status.toLowerCase(Locale.ROOT);
    if (value.contains("done") || value.contains("term")){
      return STATUS_DONE;
    }
    if (value.contains("cours") || value.contains("progress")){
      return STATUS_IN_PROGRESS;
    }
    if (value.contains("annul") || value.contains("cancel")){
      return STATUS_CANCELLED;
    }
    return STATUS_DEFAULT;
  }

  private String badgeText(){
    if (intervention == null){
      return "";
    }
    StringBuilder sb = new StringBuilder();
    if (intervention.isFavorite()){
      sb.append('★');
    }
    if (intervention.isLocked()){
      if (sb.length() > 0){
        sb.append(' ');
      }
      sb.append("🔒");
    }
    return sb.toString();
  }

  private String img(Icon icon){
    if (icon == null){
      return "";
    }
    int width = Math.max(1, icon.getIconWidth());
    int height = Math.max(1, icon.getIconHeight());
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      icon.paintIcon(null, g2, 0, 0);
    } finally {
      g2.dispose();
    }
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
      return "<img src='data:image/png;base64," + b64 + "' height='" + height + "' valign='middle'/>";
    } catch (Exception ex){
      return "";
    }
  }

  private static String escape(String text){
    return StringEscapeUtils.escapeHtml4(text == null ? "" : text);
  }

  private static String nonBlank(String value, String fallback){
    if (value != null && !value.isBlank()){
      return value;
    }
    return fallback != null ? fallback : "";
  }

  private static float adjustFont(float base, float delta){
    float size = base + delta;
    return Math.max(10f, size);
  }

  private static LocalDate toLocalDate(LocalDateTime dateTime){
    return dateTime != null ? dateTime.toLocalDate() : null;
  }

  public void setCompact(boolean compact){
    if (this.compact == compact){
      return;
    }
    this.compact = compact;
    buildUI();

  }
}
