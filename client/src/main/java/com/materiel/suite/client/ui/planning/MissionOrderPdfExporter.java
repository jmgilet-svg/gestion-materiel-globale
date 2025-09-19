package com.materiel.suite.client.ui.planning;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.GeneralSettings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Génère un PDF "Ordre de mission" (une page par intervention). */
final class MissionOrderPdfExporter {
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private MissionOrderPdfExporter(){
  }

  public static void export(File file, List<Intervention> interventions) throws IOException {
    try (PDDocument doc = new PDDocument()){
      if (interventions != null){
        for (Intervention intervention : interventions){
          if (intervention == null){
            continue;
          }
          PDPage page = new PDPage(PDRectangle.A4);
          doc.addPage(page);
          renderIntervention(doc, page, intervention);
        }
      }
      doc.save(file);
    }
  }

  public static void exportPerResourceZip(File file, List<Intervention> interventions) throws IOException {
    Map<String, List<Intervention>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    if (interventions != null){
      for (Intervention intervention : interventions){
        if (intervention == null){
          continue;
        }
        List<ResourceRef> resources = intervention.getResources();
        if (resources == null || resources.isEmpty()){
          grouped.computeIfAbsent("Sans ressource", key -> new ArrayList<>()).add(intervention);
        } else {
          for (ResourceRef resource : resources){
            if (resource == null){
              continue;
            }
            String name = safe(resource.getName(), "Ressource");
            grouped.computeIfAbsent(name, key -> new ArrayList<>()).add(intervention);
          }
        }
      }
    }
    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))){
      for (Map.Entry<String, List<Intervention>> entry : grouped.entrySet()){
        String resourceName = sanitizeFileName(entry.getKey());
        File temp = File.createTempFile("mission-" + resourceName + "-", ".pdf");
        try {
          export(temp, entry.getValue());
          zip.putNextEntry(new ZipEntry(resourceName + ".pdf"));
          try (FileInputStream in = new FileInputStream(temp)){
            in.transferTo(zip);
          }
          zip.closeEntry();
        } finally {
          if (!temp.delete()){
            temp.deleteOnExit();
          }
        }
      }
    }
  }

  /* ---------- Rendering ---------- */
  private static void renderIntervention(PDDocument doc, PDPage page, Intervention intervention) throws IOException {
    PDRectangle box = page.getMediaBox();
    float margin = 36f;
    float x = margin;
    float y = box.getHeight() - margin;
    try (PDPageContentStream stream = new PDPageContentStream(doc, page)){
      byte[] logo = tryGetAgencyLogo();
      if (logo != null && logo.length > 0){
        try {
          PDImageXObject image = PDImageXObject.createFromByteArray(doc, logo, "logo");
          stream.drawImage(image, x, y - 40f, 120f, 36f);
        } catch (Exception ignore){
        }
      }
      stream.setFont(PDType1Font.HELVETICA_BOLD, 18);
      String title = "ORDRE DE MISSION";
      float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000f * 18f;
      stream.beginText();
      stream.newLineAtOffset((box.getWidth() - titleWidth) / 2f, y);
      stream.showText(title);
      stream.endText();
      y -= 42f;

      stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
      y = text(stream, x, y, "Intervention :", 12f, true);
      stream.setFont(PDType1Font.HELVETICA, 12);
      y = text(stream, x, y, safe(intervention.getLabel(), "(sans titre)"), 14f, false);

      stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
      y = text(stream, x, y, "Client :", 12f, true);
      stream.setFont(PDType1Font.HELVETICA, 12);
      y = text(stream, x, y, safe(intervention.getClientName(), "—"), 14f, false);

      stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
      y = text(stream, x, y, "Adresse / Chantier :", 12f, true);
      stream.setFont(PDType1Font.HELVETICA, 12);
      y = paragraph(stream, x, y, safe(intervention.getAddress(), ""), 450f, 12f, 4);

      String start = formatDateTime(intervention.getDateHeureDebut());
      String end = formatDateTime(intervention.getDateHeureFin());
      stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
      y = text(stream, x, y, "Créneau :", 12f, true);
      stream.setFont(PDType1Font.HELVETICA, 12);
      y = text(stream, x, y, start + "  →  " + end, 16f, false);

      stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
      y = text(stream, x, y, "Ressources :", 12f, true);
      stream.setFont(PDType1Font.HELVETICA, 12);
      List<ResourceRef> resources = intervention.getResources();
      if (resources != null && !resources.isEmpty()){
        for (ResourceRef resource : resources){
          if (resource == null){
            continue;
          }
          y = bullet(stream, x, y, safe(resource.getName(), "Ressource"));
        }
      } else {
        y = text(stream, x, y, "—", 12f, false);
      }
      y -= 6f;

      stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
      y = text(stream, x, y, "Contacts client :", 12f, true);
      stream.setFont(PDType1Font.HELVETICA, 12);
      List<Contact> contacts = intervention.getContacts();
      if (contacts != null && !contacts.isEmpty()){
        for (Contact contact : contacts){
          if (contact == null){
            continue;
          }
          StringBuilder line = new StringBuilder();
          String first = safe(contact.getFirstName(), "");
          String last = safe(contact.getLastName(), "");
          if (!first.isEmpty()){
            line.append(first);
          }
          if (!last.isEmpty()){
            if (line.length() > 0){
              line.append(' ');
            }
            line.append(last);
          }
          if (line.length() == 0){
            line.append("Contact");
          }
          if (contact.getPhone() != null && !contact.getPhone().isBlank()){
            line.append(" — ").append(contact.getPhone().trim());
          }
          if (contact.getEmail() != null && !contact.getEmail().isBlank()){
            line.append(" — ").append(contact.getEmail().trim());
          }
          y = bullet(stream, x, y, line.toString());
        }
      } else {
        y = text(stream, x, y, "—", 12f, false);
      }
      y -= 6f;

      if (intervention.getInternalNote() != null && !intervention.getInternalNote().isBlank()){
        stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        y = text(stream, x, y, "Note interne :", 12f, true);
        stream.setFont(PDType1Font.HELVETICA, 12);
        y = paragraph(stream, x, y, intervention.getInternalNote(), 450f, 12f, 4);
      }

      String safety = tryGetSafety(intervention);
      if (safety != null && !safety.isBlank()){
        stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        y = text(stream, x, y, "Consignes & EPI requis :", 12f, true);
        stream.setFont(PDType1Font.HELVETICA, 12);
        y = paragraph(stream, x, y, safety, 450f, 12f, 6);
      }

      String equipment = tryGetEquipment(intervention);
      if (equipment != null && !equipment.isBlank()){
        stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        y = text(stream, x, y, "Matériel embarqué :", 12f, true);
        stream.setFont(PDType1Font.HELVETICA, 12);
        y = paragraph(stream, x, y, equipment, 450f, 12f, 6);
      }

      float rightX = box.getWidth() - margin - 140f;
      String deeplink = buildDeepLink(intervention);
      PDImageXObject qr = qrImage(doc, deeplink, 128);
      stream.drawImage(qr, rightX, box.getHeight() - margin - 140f, 128f, 128f);
      stream.setFont(PDType1Font.HELVETICA, 9);
      stream.beginText();
      stream.newLineAtOffset(rightX, box.getHeight() - margin - 150f);
      stream.showText("Scanner pour ouvrir la fiche");
      stream.endText();

      byte[] signature = tryGetSignature(intervention);
      if (signature != null && signature.length > 0){
        try {
          PDImageXObject image = PDImageXObject.createFromByteArray(doc, signature, "signature");
          stream.drawImage(image, rightX, y - 80f, 140f, 60f);
          y -= 90f;
          stream.setFont(PDType1Font.HELVETICA_BOLD, 9);
          stream.beginText();
          stream.newLineAtOffset(x, y);
          stream.showText("Signature client (le cas échéant) :");
          stream.endText();
          y -= 10f;
        } catch (Exception ignore){
          // Ignorer une signature invalide : on ne bloque pas l'export.
        }
      }

      if (intervention.getQuoteReference() != null){
        stream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
        stream.beginText();
        stream.newLineAtOffset(x, margin - 6f);
        stream.showText("Réf. devis : " + intervention.getQuoteReference());
        stream.endText();
      }
    }
  }

  /* ---------- Helpers ---------- */
  private static String buildDeepLink(Intervention intervention){
    if (intervention.getId() != null){
      return "gm://interventions/" + intervention.getId();
    }
    return "gm://interventions/unknown";
  }

  private static PDImageXObject qrImage(PDDocument document, String text, int size) throws IOException {
    try {
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.MARGIN, 1);
      BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
      BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
      return PDImageXObject.createFromByteArray(document, toPNG(image), "qr");
    } catch (Exception ex){
      throw new IOException("QR encode failed: " + ex.getMessage(), ex);
    }
  }

  private static byte[] toPNG(BufferedImage image) throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", baos);
    return baos.toByteArray();
  }

  private static String safe(String text, String defaultValue){
    if (text == null){
      return defaultValue;
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()){
      return defaultValue;
    }
    return trimmed.replace('\r', ' ').replace('\n', ' ');
  }

  private static String safe(String text){
    return safe(text, "");
  }

  private static String sanitizeFileName(String name){
    String value = safe(name, "Ressource");
    String cleaned = value.replaceAll("[^a-zA-Z0-9-_\\.]+", "_");
    return cleaned.isBlank() ? "Ressource" : cleaned;
  }

  private static float text(PDPageContentStream stream, float x, float y, String text, float vspace, boolean bold) throws IOException {
    stream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 12);
    stream.beginText();
    stream.newLineAtOffset(x, y);
    stream.showText(text == null ? "" : text);
    stream.endText();
    return y - vspace;
  }

  private static float bullet(PDPageContentStream stream, float x, float y, String text) throws IOException {
    String value = text == null ? "" : text;
    stream.beginText();
    stream.newLineAtOffset(x + 10f, y);
    stream.showText("• " + value);
    stream.endText();
    return y - 14f;
  }

  private static float paragraph(PDPageContentStream stream, float x, float y, String text, float width, float leading, int maxLines) throws IOException {
    List<String> lines = wrap(text, width, PDType1Font.HELVETICA, 12f, maxLines);
    for (String line : lines){
      stream.beginText();
      stream.newLineAtOffset(x, y);
      stream.showText(line);
      stream.endText();
      y -= leading;
    }
    return y - 2f;
  }

  private static List<String> wrap(String text, float maxWidth, PDType1Font font, float size, int maxLines) throws IOException {
    List<String> lines = new ArrayList<>();
    if (text == null){
      return lines;
    }
    String normalized = text.replace('\r', ' ').replace('\n', ' ').trim();
    if (normalized.isEmpty()){
      return lines;
    }
    String[] words = normalized.split("\\s+");
    StringBuilder current = new StringBuilder();
    for (String word : words){
      String candidate = current.length() == 0 ? word : current + " " + word;
      float width = font.getStringWidth(candidate) / 1000f * size;
      if (width > maxWidth){
        if (current.length() > 0){
          lines.add(current.toString());
          current = new StringBuilder(word);
        } else {
          lines.add(candidate);
          current = new StringBuilder();
        }
        if (maxLines > 0 && lines.size() >= maxLines){
          lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " …");
          return lines;
        }
      } else {
        current = new StringBuilder(candidate);
      }
    }
    if (current.length() > 0){
      lines.add(current.toString());
    }
    return lines;
  }

  private static String formatDateTime(LocalDateTime dateTime){
    if (dateTime == null){
      return "—";
    }
    try {
      return DATE_TIME.format(dateTime);
    } catch (Exception ex){
      return "—";
    }
  }

  /** Lecture optionnelle d'une signature PNG via réflexion ou Base64. */
  private static byte[] tryGetSignature(Intervention intervention){
    try {
      var method = intervention.getClass().getMethod("getSignaturePng");
      Object value = method.invoke(intervention);
      if (value instanceof byte[] bytes && bytes.length > 0){
        return bytes;
      }
      if (value instanceof String str){
        byte[] decoded = decodeBase64(str);
        if (decoded != null && decoded.length > 0){
          return decoded;
        }
      }
    } catch (Exception ignore){
      // Fallback sur le champ standard ci-dessous.
    }
    byte[] decoded = decodeBase64(intervention.getSignaturePngBase64());
    if (decoded != null && decoded.length > 0){
      return decoded;
    }
    return null;
  }

  private static byte[] tryGetAgencyLogo(){
    try {
      GeneralSettings settings = ServiceLocator.settings().getGeneral();
      if (settings == null){
        return null;
      }
      return decodeBase64(settings.getAgencyLogoPngBase64());
    } catch (Exception ignore){
      return null;
    }
  }

  private static String tryGetSafety(Intervention intervention){
    if (intervention == null){
      return null;
    }
    String[] methods = {"getSafetyInstructions", "getEpiRequired", "getSafetyNote"};
    for (String name : methods){
      try {
        var method = intervention.getClass().getMethod(name);
        Object value = method.invoke(intervention);
        if (value != null){
          String text = String.valueOf(value);
          if (!text.isBlank()){
            return text;
          }
        }
      } catch (Exception ignore){
      }
    }
    return null;
  }

  private static String tryGetEquipment(Intervention intervention){
    if (intervention == null){
      return null;
    }
    String[] methods = {"getEquipmentList", "getEquipmentNotes", "getGear"};
    for (String name : methods){
      try {
        var method = intervention.getClass().getMethod(name);
        Object value = method.invoke(intervention);
        if (value != null){
          String text = String.valueOf(value);
          if (!text.isBlank()){
            return text;
          }
        }
      } catch (Exception ignore){
      }
    }
    return null;
  }

  private static byte[] decodeBase64(String base64){
    if (base64 == null || base64.isBlank()){
      return null;
    }
    try {
      return Base64.getDecoder().decode(base64.trim());
    } catch (IllegalArgumentException ex){
      return null;
    }
  }
}
