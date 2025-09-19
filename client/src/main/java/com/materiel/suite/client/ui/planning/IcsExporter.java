package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.ResourceRef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Génère des exports iCalendar (.ics) à partir d'interventions sélectionnées. */
final class IcsExporter {
  private static final DateTimeFormatter UTC_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
  private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

  private IcsExporter(){
  }

  /** Un seul .ics contenant toutes les interventions. */
  static void exportSingle(File file, List<Intervention> interventions) throws IOException {
    List<Intervention> safe = interventions == null ? List.of() : interventions;
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)){
      writer.write(buildCalendar(safe));
    }
  }

  /** Un .zip avec un .ics par ressource (nom_fichier = <nom_ressource>.ics). */
  static void exportPerResourceZip(File zipFile, List<Intervention> interventions) throws IOException {
    Map<String, List<Intervention>> byResource = groupByResource(interventions);
    try (ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(zipFile), StandardCharsets.UTF_8)){
      for (Map.Entry<String, List<Intervention>> entry : byResource.entrySet()){
        String resourceName = entry.getKey();
        String calendar = buildCalendar(entry.getValue());
        ZipEntry zipEntry = new ZipEntry(safe(resourceName) + ".ics");
        stream.putNextEntry(zipEntry);
        stream.write(calendar.getBytes(StandardCharsets.UTF_8));
        stream.closeEntry();
      }
    }
  }

  private static Map<String, List<Intervention>> groupByResource(List<Intervention> interventions){
    Map<String, List<Intervention>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    if (interventions == null){
      return map;
    }
    for (Intervention intervention : interventions){
      if (intervention == null){
        continue;
      }
      List<ResourceRef> resources = intervention.getResources();
      if (resources == null || resources.isEmpty()){
        map.computeIfAbsent("Sans ressource", key -> new ArrayList<>()).add(intervention);
      } else {
        for (ResourceRef ref : resources){
          String name = ref != null && ref.getName() != null && !ref.getName().isBlank()
              ? ref.getName()
              : "Ressource";
          map.computeIfAbsent(name, key -> new ArrayList<>()).add(intervention);
        }
      }
    }
    return map;
  }

  private static String buildCalendar(List<Intervention> interventions){
    StringBuilder builder = new StringBuilder();
    builder.append("BEGIN:VCALENDAR\r\n")
        .append("PRODID:-//Gestion Materiel//Planning//FR\r\n")
        .append("VERSION:2.0\r\n")
        .append("CALSCALE:GREGORIAN\r\n")
        .append("METHOD:PUBLISH\r\n");
    if (interventions != null){
      for (Intervention intervention : interventions){
        if (intervention != null){
          appendEvent(builder, intervention);
        }
      }
    }
    builder.append("END:VCALENDAR\r\n");
    return builder.toString();
  }

  private static void appendEvent(StringBuilder builder, Intervention intervention){
    Instant now = Instant.now();
    builder.append("BEGIN:VEVENT\r\n");
    String uidBase = intervention.getId() != null ? intervention.getId().toString() : UUID.randomUUID().toString();
    builder.append("UID:").append(escape(uidBase + "@gestion-materiel")).append("\r\n");
    builder.append("DTSTAMP:").append(formatUtc(now)).append("\r\n");

    Instant start = resolveStart(intervention);
    Instant end = resolveEnd(intervention);
    if (start != null){
      builder.append("DTSTART:").append(formatUtc(start)).append("\r\n");
    }
    if (end != null){
      builder.append("DTEND:").append(formatUtc(end)).append("\r\n");
    }

    builder.append("SUMMARY:").append(fold(buildSummary(intervention))).append("\r\n");

    String location = intervention.getAddress();
    if (location != null && !location.isBlank()){
      builder.append("LOCATION:").append(fold(location)).append("\r\n");
    }

    String description = buildDescription(intervention);
    if (!description.isBlank()){
      builder.append("DESCRIPTION:").append(fold(description)).append("\r\n");
    }

    builder.append("END:VEVENT\r\n");
  }

  private static Instant resolveStart(Intervention intervention){
    LocalDateTime dateTime = intervention.getDateHeureDebut();
    if (dateTime != null){
      return dateTime.atZone(DEFAULT_ZONE).toInstant();
    }
    LocalDate date = intervention.getDateDebut();
    if (date != null){
      return date.atStartOfDay(DEFAULT_ZONE).toInstant();
    }
    return null;
  }

  private static Instant resolveEnd(Intervention intervention){
    LocalDateTime dateTime = intervention.getDateHeureFin();
    if (dateTime != null){
      return dateTime.atZone(DEFAULT_ZONE).toInstant();
    }
    LocalDate date = intervention.getDateFin();
    if (date != null){
      return date.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
    }
    return null;
  }

  private static String buildSummary(Intervention intervention){
    String label = intervention.getLabel();
    String client = intervention.getClientName();
    boolean hasLabel = label != null && !label.isBlank();
    boolean hasClient = client != null && !client.isBlank();
    if (hasLabel && hasClient){
      return label + " — " + client;
    }
    if (hasLabel){
      return label;
    }
    if (hasClient){
      return client;
    }
    return "Intervention";
  }

  private static String buildDescription(Intervention intervention){
    StringBuilder description = new StringBuilder();
    String details = intervention.getDescription();
    if (details != null && !details.isBlank()){
      description.append(details.trim());
    }
    String note = intervention.getInternalNote();
    if (note != null && !note.isBlank()){
      if (description.length() > 0){
        description.append('\n');
      }
      description.append("Note interne: ").append(note.trim());
    }
    List<ResourceRef> resources = intervention.getResources();
    if (resources != null && !resources.isEmpty()){
      if (description.length() > 0){
        description.append('\n');
      }
      description.append("Ressources: ");
      for (int i = 0; i < resources.size(); i++){
        ResourceRef ref = resources.get(i);
        String name = ref != null && ref.getName() != null && !ref.getName().isBlank()
            ? ref.getName()
            : "Ressource";
        if (i > 0){
          description.append(", ");
        }
        description.append(name);
      }
    }
    String quoteRef = intervention.getQuoteReference();
    if (quoteRef != null && !quoteRef.isBlank()){
      if (description.length() > 0){
        description.append('\n');
      }
      description.append("Devis lié: ").append(quoteRef.trim());
    }
    return description.toString();
  }

  /** Formats RFC5545 UTC (Z). */
  private static String formatUtc(Instant instant){
    return UTC_FORMAT.format(instant);
  }

  /** Escapes ICS text (\,; , commas, and newlines). */
  private static String escape(String value){
    if (value == null){
      return "";
    }
    String normalized = value.replace("\r\n", "\n").replace("\r", "\n");
    normalized = normalized.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,");
    return normalized.replace("\n", "\\n");
  }

  /** Folds long lines at 75 octets max with CRLF + space (RFC5545). */
  private static String fold(String value){
    String escaped = escape(value);
    StringBuilder folded = new StringBuilder();
    int lineLength = 0;
    for (int i = 0; i < escaped.length(); ){
      int codePoint = escaped.codePointAt(i);
      String cp = new String(Character.toChars(codePoint));
      int bytes = cp.getBytes(StandardCharsets.UTF_8).length;
      if (lineLength + bytes > 75){
        folded.append("\r\n ");
        lineLength = 1; // leading space counts toward the next line length
      }
      folded.append(cp);
      lineLength += bytes;
      i += Character.charCount(codePoint);
    }
    return folded.toString();
  }

  private static String safe(String name){
    if (name == null || name.isBlank()){
      return "ressource";
    }
    return name.replaceAll("[^a-zA-Z0-9-_\\.]+", "_");
  }
}
