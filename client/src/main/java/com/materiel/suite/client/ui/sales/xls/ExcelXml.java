package com.materiel.suite.client.ui.sales.xls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Générateur très simple de fichier Excel XML (SpreadsheetML 2003). */
public class ExcelXml {
  private final String sheetName;
  private final List<List<String>> rows = new ArrayList<>();

  public ExcelXml(String sheetName){
    if (sheetName == null || sheetName.isBlank()){
      this.sheetName = "Feuille1";
    } else {
      this.sheetName = sheetName;
    }
  }

  public void addRow(List<String> row){
    if (row == null){
      rows.add(Collections.emptyList());
    } else {
      rows.add(new ArrayList<>(row));
    }
  }

  public void save(File file) throws IOException {
    if (file == null){
      return;
    }
    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)){
      writer.write("<?xml version=\"1.0\"?>\n");
      writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n");
      writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
      writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
      writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
      writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
      writer.write("<Worksheet ss:Name=\"" + escape(sheetName) + "\"><Table>\n");
      for (List<String> row : rows){
        writer.write("<Row>");
        for (String cell : row){
          writer.write("<Cell><Data ss:Type=\"String\">");
          writer.write(escape(cell));
          writer.write("</Data></Cell>");
        }
        writer.write("</Row>\n");
      }
      writer.write("</Table></Worksheet></Workbook>");
    }
  }

  private static String escape(String value){
    if (value == null){
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
