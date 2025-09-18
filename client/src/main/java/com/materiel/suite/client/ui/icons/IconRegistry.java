package com.materiel.suite.client.ui.icons;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registre centralisant les icônes SVG intégrées à l'application. */
public final class IconRegistry {
  private static final List<String> ICON_KEYS = List.of(
      "search", "success", "error", "info", "settings", "signature", "task", "invoice",
      "maximize", "minimize", "plus", "edit", "trash", "refresh", "image", "cube", "file",
      "calendar", "user", "wrench", "lock", "building",
      "crane", "truck", "forklift", "container", "excavator", "generator", "hook", "helmet", "pallet",
      "badge"
  );
  private static final Set<String> ICON_SET = Set.copyOf(ICON_KEYS);
  private static final Map<Integer, Icon> PLACEHOLDER_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Icon> FALLBACK_CACHE = new ConcurrentHashMap<>();
  private static final Color[] FALLBACK_COLORS = {
      new Color(0x2962FF), new Color(0x00B8D4), new Color(0x00C853),
      new Color(0xFF8F00), new Color(0xD81B60), new Color(0x8E24AA)
  };

  private IconRegistry(){}

  /** Retourne la liste complète des clés disponibles. */
  public static List<String> listKeys(){
    return new ArrayList<>(ICON_KEYS);
  }

  /** Indique si la clé correspond à une icône connue. */
  public static boolean isKnownKey(String key){
    if (key == null){
      return false;
    }
    String normalized = key.trim();
    if (normalized.isEmpty()){
      return false;
    }
    return ICON_SET.contains(normalized);
  }

  /** Charge une icône SVG avec la taille souhaitée, ou {@code null} si absente. */
  public static Icon load(String key, int size){
    if (size <= 0){
      return null;
    }
    if (!isKnownKey(key)){
      return null;
    }
    try {
      return new FlatSVGIcon("icons/" + key.trim() + ".svg", size, size);
    } catch (Exception ex){
      return null;
    }
  }

  public static Icon small(String key){
    Icon icon = load(key, 16);
    return icon != null ? icon : fallback(key, 16);
  }

  public static Icon medium(String key){
    Icon icon = load(key, 20);
    return icon != null ? icon : fallback(key, 20);
  }

  public static Icon large(String key){
    Icon icon = load(key, 28);
    return icon != null ? icon : fallback(key, 28);
  }

  /** Icône par défaut lorsqu'aucune n'est définie. */
  public static Icon placeholder(int size){
    if (size <= 0){
      return null;
    }
    return PLACEHOLDER_CACHE.computeIfAbsent(size, s -> new FlatSVGIcon("icons/user.svg", s, s));
  }

  /**
   * Charge une icône SVG si disponible, sinon renvoie une icône générique.
   * Utile pour fournir un aperçu même lorsque la valeur est manquante.
   */
  public static Icon loadOrPlaceholder(String key, int size){
    Icon icon = load(key, size);
    if (icon != null){
      return icon;
    }
    Icon generated = fallback(key, size);
    return generated != null ? generated : placeholder(size);
  }

  private static Icon fallback(String key, int size){
    if (size <= 0){
      return null;
    }
    String normalized = key == null ? "" : key.trim();
    String cacheKey = normalized.toLowerCase(Locale.ROOT) + "|" + size;
    return FALLBACK_CACHE.computeIfAbsent(cacheKey, k -> createFallbackIcon(normalized, size));
  }

  private static Icon createFallbackIcon(String key, int size){
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      int diameter = size - 2;
      Color base = fallbackColor(key);
      g.setColor(base);
      g.fillOval(1, 1, diameter, diameter);

      g.setColor(Color.WHITE);
      String letter = fallbackLetter(key);
      Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, Math.round(size * 0.6f)));
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();
      int textWidth = fm.stringWidth(letter);
      int textX = (size - textWidth) / 2;
      int textY = (size + fm.getAscent() - fm.getDescent()) / 2 - 1;
      g.drawString(letter, Math.max(1, textX), Math.max(fm.getAscent(), textY));
    } finally {
      g.dispose();
    }
    return new ImageIcon(image);
  }

  private static Color fallbackColor(String key){
    if (FALLBACK_COLORS.length == 0){
      return new Color(0x546E7A);
    }
    String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
    int idx = Math.abs(normalized.hashCode()) % FALLBACK_COLORS.length;
    return FALLBACK_COLORS[idx];
  }

  private static String fallbackLetter(String key){
    if (key == null || key.isBlank()){
      return "?";
    }
    for (int i = 0; i < key.length(); i++){
      char c = key.charAt(i);
      if (Character.isLetterOrDigit(c)){
        return String.valueOf(Character.toUpperCase(c));
      }
    }
    return "?";
  }
}

