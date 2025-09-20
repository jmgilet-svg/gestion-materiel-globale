package com.materiel.suite.client.util;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.GeneralSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Utilitaires centralisés pour les montants monétaires (arrondis, TVA, formatage). */
public final class Money {
  private Money(){
  }

  /** Retourne la valeur arrondie selon la configuration en vigueur. */
  public static BigDecimal round(BigDecimal value){
    if (value == null){
      value = BigDecimal.ZERO;
    }
    SettingsSnapshot snapshot = snapshot();
    return value.setScale(snapshot.scale(), snapshot.mode());
  }

  /** Retourne la valeur arrondie sous forme de {@code double}. */
  public static double round(double value){
    return round(BigDecimal.valueOf(value)).doubleValue();
  }

  /** Fournit un formateur monétaire configuré avec la précision actuelle. */
  public static NumberFormat currencyFormat(){
    SettingsSnapshot snapshot = snapshot();
    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    format.setMinimumFractionDigits(snapshot.scale());
    format.setMaximumFractionDigits(snapshot.scale());
    return format;
  }

  /** Nombre de décimales à utiliser pour les montants. */
  public static int scale(){
    return snapshot().scale();
  }

  /** Mode d'arrondi configuré. */
  public static RoundingMode roundingMode(){
    return snapshot().mode();
  }

  /** Pourcentage de TVA configuré (0-100). */
  public static BigDecimal vatPercent(){
    return snapshot().vatPercent();
  }

  /** Taux de TVA (fraction) équivalent au pourcentage configuré. */
  public static BigDecimal vatRate(){
    SettingsSnapshot snapshot = snapshot();
    return snapshot.vatPercent().divide(BigDecimal.valueOf(100), snapshot.scale() + 4, snapshot.mode());
  }

  private static SettingsSnapshot snapshot(){
    GeneralSettings settings = ServiceLocator.settings().getGeneral();
    int scale = Math.max(0, settings.getRoundingScale());
    RoundingMode mode = toRoundingMode(settings.getRoundingMode());
    Double pct = settings.getDefaultVatPercent();
    BigDecimal vatPercent = BigDecimal.valueOf(pct != null ? pct : 20.0);
    return new SettingsSnapshot(scale, mode, vatPercent);
  }

  private static RoundingMode toRoundingMode(String mode){
    if (mode == null){
      return RoundingMode.HALF_UP;
    }
    String normalized = mode.trim().toUpperCase(Locale.ROOT);
    return switch (normalized){
      case "HALF_DOWN" -> RoundingMode.HALF_DOWN;
      case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
      case "HALF_UP" -> RoundingMode.HALF_UP;
      default -> RoundingMode.HALF_UP;
    };
  }

  private record SettingsSnapshot(int scale, RoundingMode mode, BigDecimal vatPercent){}
}
