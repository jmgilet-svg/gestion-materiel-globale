package com.materiel.suite.client.ui.sales.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Générateur PDF ultra-léger (texte uniquement) sans dépendances externes.
 * Permet d'exporter des listes simples sur une ou plusieurs pages.
 */
public class PdfMini {
  private static final int LINES_PER_PAGE = 45;

  private static class Line {
    final String text;
    final boolean title;

    Line(String text, boolean title){
      this.text = text == null ? "" : text;
      this.title = title;
    }
  }

  private final List<Line> lines = new ArrayList<>();

  public void addTitle(String text){
    lines.add(new Line(text, true));
  }

  public void addParagraph(String text){
    lines.add(new Line(text, false));
  }

  public void save(File file) throws IOException {
    if (file == null){
      throw new IllegalArgumentException("file == null");
    }
    File parent = file.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()){
      throw new IOException("Impossible de créer le dossier " + parent);
    }
    List<List<Line>> pages = paginate();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    List<Integer> offsets = new ArrayList<>();
    offsets.add(0); // objet 0 réservé
    out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

    registerOffset(offsets, 1, out.size());
    out.write(buildObject(1, "<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));

    String kids = buildKids(pages.size());
    registerOffset(offsets, 2, out.size());
    out.write(buildObject(2, ("<< /Type /Pages /Count " + pages.size() + " /Kids [" + kids + "] >>\nendobj\n").getBytes(StandardCharsets.US_ASCII)));

    registerOffset(offsets, 3, out.size());
    out.write(buildObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"));

    int objectNumber = 4;
    for (List<Line> page : pages){
      byte[] content = buildPageContent(page);

      String pageDict = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
          + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + (objectNumber + 1) + " 0 R >>\nendobj\n";
      registerOffset(offsets, objectNumber, out.size());
      out.write(buildObject(objectNumber, pageDict.getBytes(StandardCharsets.US_ASCII)));

      registerOffset(offsets, objectNumber + 1, out.size());
      out.write(buildObject(objectNumber + 1, ("<< /Length " + content.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII)));
      out.write(content);
      out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

      objectNumber += 2;
    }

    int xrefStart = out.size();
    out.write(("xref\n0 " + objectNumber + "\n").getBytes(StandardCharsets.US_ASCII));
    out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
    ensureSize(offsets, objectNumber);
    for (int i = 1; i < objectNumber; i++){
      int offset = offsets.get(i);
      out.write(String.format(Locale.ROOT, "%010d 00000 n \n", offset).getBytes(StandardCharsets.US_ASCII));
    }
    out.write(("trailer << /Size " + objectNumber + " /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));

    try (FileOutputStream fos = new FileOutputStream(file)){
      out.writeTo(fos);
    }
  }

  private List<List<Line>> paginate(){
    List<List<Line>> pages = new ArrayList<>();
    if (lines.isEmpty()){
      pages.add(List.of());
      return pages;
    }
    for (int i = 0; i < lines.size(); i += LINES_PER_PAGE){
      int end = Math.min(i + LINES_PER_PAGE, lines.size());
      pages.add(new ArrayList<>(lines.subList(i, end)));
    }
    return pages;
  }

  private static String buildKids(int pageCount){
    StringBuilder kids = new StringBuilder();
    for (int i = 0; i < pageCount; i++){
      if (i > 0){
        kids.append(' ');
      }
      kids.append(4 + (i * 2)).append(" 0 R");
    }
    return kids.toString();
  }

  private static void registerOffset(List<Integer> offsets, int objectNumber, int value){
    ensureSize(offsets, objectNumber + 1);
    offsets.set(objectNumber, value);
  }

  private static void ensureSize(List<Integer> offsets, int size){
    while (offsets.size() < size){
      offsets.add(0);
    }
  }

  private static byte[] buildObject(int id, byte[] body){
    byte[] header = (id + " 0 obj\n").getBytes(StandardCharsets.US_ASCII);
    byte[] result = new byte[header.length + body.length];
    System.arraycopy(header, 0, result, 0, header.length);
    System.arraycopy(body, 0, result, header.length, body.length);
    return result;
  }

  private static byte[] buildObject(int id, String body){
    return buildObject(id, body.getBytes(StandardCharsets.US_ASCII));
  }

  private static byte[] buildPageContent(List<Line> page) throws IOException {
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    content.write("BT\n/F1 12 Tf\n1 0 0 1 50 780 Tm\n".getBytes(StandardCharsets.US_ASCII));
    for (Line line : page){
      if (line.title){
        content.write("/F1 16 Tf\n".getBytes(StandardCharsets.US_ASCII));
        content.write(("(" + escapePdf(line.text) + ") Tj\n").getBytes(StandardCharsets.US_ASCII));
        content.write("/F1 12 Tf\n0 -28 Td\n".getBytes(StandardCharsets.US_ASCII));
      } else {
        content.write(("(" + escapePdf(line.text) + ") Tj\n").getBytes(StandardCharsets.US_ASCII));
        content.write("0 -16 Td\n".getBytes(StandardCharsets.US_ASCII));
      }
    }
    content.write("ET\n".getBytes(StandardCharsets.US_ASCII));
    return content.toByteArray();
  }

  private static String escapePdf(String value){
    if (value == null || value.isEmpty()){
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace('\r', ' ')
        .replace('\n', ' ');
  }
}
