package com.materiel.suite.client.ui.icons;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registre centralisant les icônes SVG intégrées à l'application. */
public final class IconRegistry {
  private static final List<String> ICON_KEYS = List.of(
      "crane", "truck", "forklift", "excavator", "generator", "container",
      "hook", "helmet", "wrench", "pallet", "calendar", "user",
      "file", "invoice", "search", "success", "error", "info"
  );
  private static final Set<String> ICON_SET = Set.copyOf(ICON_KEYS);
  private static final Map<Integer, Icon> PLACEHOLDER_CACHE = new ConcurrentHashMap<>();

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
    return load(key, 16);
  }

  public static Icon medium(String key){
    return load(key, 20);
  }

  public static Icon large(String key){
    return load(key, 28);
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
    return icon != null ? icon : placeholder(size);
  }
}

