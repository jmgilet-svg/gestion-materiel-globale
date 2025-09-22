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
      "maximize", "minimize", "plus", "edit", "trash", "refresh", "image", "cube", "file", "file-plus",
      "calendar", "user", "wrench", "lock", "building", "filter",
      "crane", "truck", "forklift", "container", "excavator", "generator", "hook", "helmet", "pallet",
      "badge"
  );
  private static final Set<String> ICON_SET = Set.copyOf(ICON_KEYS);
  private static final Map<Integer, Icon> PLACEHOLDER_CACHE = new ConcurrentHashMap<>();
  private static final Map<Integer, Icon> FALLBACK_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, String> ALIASES = Map.ofEntries(
      Map.entry("ajouter", "plus"),
      Map.entry("add", "plus"),
      Map.entry("modifier", "edit"),
      Map.entry("edit-outline", "edit"),
      Map.entry("supprimer", "trash"),
      Map.entry("delete", "trash"),
      Map.entry("reload", "refresh"),
      Map.entry("recharger", "refresh"),
      Map.entry("file-add", "file-plus"),
      Map.entry("calendar-outline", "calendar"),
      Map.entry("user-outline", "user"),
      Map.entry("search-outline", "search")
  );

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
    String canonical = canonicalKey(key);
    if (canonical == null || canonical.isEmpty()){
      return false;
    }
    return ICON_SET.contains(canonical);
  }

  /** Charge une icône SVG avec la taille souhaitée, ou {@code null} si absente. */
  public static Icon load(String key, int size){
    if (size <= 0){
      return null;
    }
    String canonical = canonicalKey(key);
    if (canonical == null || canonical.isEmpty() || !ICON_SET.contains(canonical)){
      return null;
    }
    try {
      return new FlatSVGIcon("icons/" + canonical + ".svg", size, size);
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

  /** Variante légèrement plus grande, utile pour mettre en avant certaines actions. */
  public static Icon colored(String key){
    return medium(key);
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
    int normalizedSize = Math.max(12, size);
    return FALLBACK_CACHE.computeIfAbsent(normalizedSize, IconRegistry::createFallbackIcon);
  }

  private static Icon createFallbackIcon(int size){
    int effective = Math.max(12, size);
    BufferedImage image = new BufferedImage(effective, effective, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(0xECEFF1));
      g.fillRoundRect(0, 0, effective - 1, effective - 1, 4, 4);
      g.setColor(new Color(0x90A4AE));
      g.drawRoundRect(0, 0, effective - 1, effective - 1, 4, 4);
      g.fillOval(effective / 2 - 1, effective / 2 - 1, 2, 2);
    } finally {
      g.dispose();
    }
    return new ImageIcon(image);
  }

  private static String canonicalKey(String key){
    if (key == null){
      return null;
    }
    String trimmed = key.trim();
    if (trimmed.isEmpty()){
      return "";
    }
    String lower = trimmed.toLowerCase(Locale.ROOT);
    String alias = ALIASES.get(lower);
    return alias != null ? alias : lower;
  }
}

