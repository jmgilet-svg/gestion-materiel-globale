package com.materiel.suite.client.ui.theme;

import com.materiel.suite.client.settings.GeneralSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Export / import minimaliste (JSON) pour le thème UI. */
public final class ThemeIO {
  private ThemeIO(){
  }

  public static void exportTheme(File file, GeneralSettings settings) throws IOException {
    if (file == null){
      throw new IllegalArgumentException("Fichier de destination manquant");
    }
    GeneralSettings safe = settings != null ? settings : new GeneralSettings();
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("uiScalePercent", safe.getUiScalePercent());
    values.put("highContrast", safe.isHighContrast());
    values.put("dyslexiaMode", safe.isDyslexiaMode());
    values.put("fontExtraPoints", safe.getFontExtraPoints());
    values.put("brandPrimaryHex", safe.getBrandPrimaryHex());
    values.put("brandSecondaryHex", safe.getBrandSecondaryHex());
    values.put("defaultVatPercent", safe.getDefaultVatPercent());
    values.put("roundingMode", safe.getRoundingMode());
    values.put("roundingScale", safe.getRoundingScale());
    String json = toJson(values);
    try (FileOutputStream out = new FileOutputStream(file)){
      out.write(json.getBytes(StandardCharsets.UTF_8));
    }
  }

  public static GeneralSettings importTheme(File file) throws IOException {
    if (file == null){
      throw new IllegalArgumentException("Fichier source manquant");
    }
    String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    Map<String, String> values = parseJson(json);
    GeneralSettings settings = new GeneralSettings();
    settings.setUiScalePercent(parseInt(values.get("uiScalePercent"), settings.getUiScalePercent()));
    settings.setHighContrast(parseBoolean(values.get("highContrast"), settings.isHighContrast()));
    settings.setDyslexiaMode(parseBoolean(values.get("dyslexiaMode"), settings.isDyslexiaMode()));
    settings.setFontExtraPoints(parseInt(values.get("fontExtraPoints"), settings.getFontExtraPoints()));
    settings.setBrandPrimaryHex(defaultString(values.get("brandPrimaryHex"), settings.getBrandPrimaryHex()));
    settings.setBrandSecondaryHex(defaultString(values.get("brandSecondaryHex"), settings.getBrandSecondaryHex()));
    settings.setDefaultVatPercent(parseDouble(values.get("defaultVatPercent"), settings.getDefaultVatPercent()));
    settings.setRoundingMode(defaultString(values.get("roundingMode"), settings.getRoundingMode()));
    settings.setRoundingScale(parseInt(values.get("roundingScale"), settings.getRoundingScale()));
    return settings;
  }

  private static String toJson(Map<String, ?> values){
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    boolean first = true;
    for (Map.Entry<String, ?> entry : values.entrySet()){
      if (!first){
        sb.append(',');
      }
      first = false;
      sb.append('"').append(escape(entry.getKey())).append('"').append(':');
      Object value = entry.getValue();
      if (value == null){
        sb.append("null");
      } else if (value instanceof Number || value instanceof Boolean){
        sb.append(String.valueOf(value));
      } else {
        sb.append('"').append(escape(String.valueOf(value))).append('"');
      }
    }
    sb.append('}');
    return sb.toString();
  }

  private static Map<String, String> parseJson(String json){
    Map<String, String> map = new LinkedHashMap<>();
    if (json == null || json.isBlank()){
      return map;
    }
    String trimmed = json.trim();
    if (trimmed.startsWith("{") && trimmed.endsWith("}")){
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    Pattern pair = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"(?:\\\\.|[^\\"])*\"|[^,]+)(?:,|$)");
    Matcher matcher = pair.matcher(trimmed);
    while (matcher.find()){
      String key = unescape(matcher.group(1));
      String raw = matcher.group(2).trim();
      map.put(key, unquote(raw));
    }
    return map;
  }

  private static String unquote(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    if ("null".equalsIgnoreCase(trimmed)){
      return null;
    }
    if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")){
      String body = trimmed.substring(1, trimmed.length() - 1);
      return unescape(body);
    }
    return trimmed;
  }

  private static String escape(String value){
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"");
  }

  private static String unescape(String value){
    return value
        .replace("\\\"", "\"")
        .replace("\\\\", "\\");
  }

  private static String defaultString(String value, String fallback){
    if (value == null){
      return fallback;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? fallback : trimmed;
  }

  private static int parseInt(String value, int fallback){
    if (value == null || value.isBlank()){
      return fallback;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex){
      return fallback;
    }
  }

  private static Double parseDouble(String value, Double fallback){
    if (value == null || value.isBlank()){
      return fallback;
    }
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException ex){
      return fallback;
    }
  }

  private static boolean parseBoolean(String value, boolean fallback){
    if (value == null){
      return fallback;
    }
    String normalized = value.trim();
    if (normalized.equalsIgnoreCase("true") || normalized.equals("1")){
      return true;
    }
    if (normalized.equalsIgnoreCase("false") || normalized.equals("0")){
      return false;
    }
    return fallback;
  }
}
