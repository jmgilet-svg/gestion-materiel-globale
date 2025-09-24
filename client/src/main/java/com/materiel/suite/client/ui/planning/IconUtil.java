package com.materiel.suite.client.ui.planning;

import com.materiel.suite.client.ui.icons.IconRegistry;

import javax.swing.*;
import java.util.Locale;

/**
 * Small helper used by planning tiles to retrieve icons based on loose keys.
 * It normalizes common resource labels (ex: "grue", "truck") to the
 * {@link IconRegistry} vocabulary while keeping the dependency optional.
 */
final class IconUtil {
  private IconUtil(){
  }

  static Icon colored(String key, int size){
    if (key == null || key.isBlank() || size <= 0){
      return null;
    }
    String normalized = normalize(key);
    if (normalized == null || normalized.isBlank()){
      return null;
    }
    Icon icon = IconRegistry.load(normalized, size);
    if (icon != null){
      return icon;
    }
    String fallback = key.trim().toLowerCase(Locale.ROOT);
    if (!fallback.equals(normalized)){
      icon = IconRegistry.load(fallback, size);
    }
    return icon;
  }

  private static String normalize(String key){
    String lower = key.trim().toLowerCase(Locale.ROOT);
    if (lower.isEmpty()){
      return "";
    }
    if (lower.contains("grue") || lower.contains("crane")){
      return "crane";
    }
    if (lower.contains("camion") || lower.contains("truck")){
      return "truck";
    }
    if (lower.contains("chariot") || lower.contains("forklift")){
      return "forklift";
    }
    if (lower.contains("pelle") || lower.contains("excavator")){
      return "excavator";
    }
    if (lower.contains("generator") || lower.contains("générateur") || lower.contains("generateur")){
      return "generator";
    }
    if (lower.contains("hook") || lower.contains("crochet")){
      return "hook";
    }
    if (lower.contains("casque") || lower.contains("helmet") || lower.contains("ouvrier")
        || lower.contains("worker") || lower.contains("tech") || lower.contains("chauffeur")){
      return "helmet";
    }
    if (lower.contains("pallet") || lower.contains("palette")){
      return "pallet";
    }
    if (lower.contains("badge")){
      return "badge";
    }
    if (lower.contains("wrench") || lower.contains("outil") || lower.contains("maintenance")){
      return "wrench";
    }
    if (lower.contains("container")){
      return "container";
    }
    if (lower.contains("info")){
      return "info";
    }
    if (lower.contains("file")){
      return "file";
    }
    return lower.replaceAll("[^a-z0-9_-]", "");
  }
}
