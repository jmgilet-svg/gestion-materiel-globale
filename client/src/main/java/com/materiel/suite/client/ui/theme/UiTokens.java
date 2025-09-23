package com.materiel.suite.client.ui.theme;

import java.awt.Color;

/**
 * Tokens UI centralisés (fallbacks si absence de configuration agence).
 * Toutes les couleurs passent par {@link ThemeManager} lorsque possible.
 */
public final class UiTokens {
  private UiTokens(){
  }

  public static Color brandPrimary(){
    return ThemeManager.parseColorSafe(ThemeManager.brandPrimaryHex(), new Color(0x0F62FE));
  }

  public static Color brandSecondary(){
    return ThemeManager.parseColorSafe(ThemeManager.brandSecondaryHex(), new Color(0xF4511E));
  }

  public static Color textPrimary(){
    return new Color(0x0F172A);
  }

  public static Color textMuted(){
    return new Color(0x475569);
  }

  public static Color line(){
    return new Color(0xE5E7EB);
  }

  public static Color bgSoft(){
    return new Color(0xF8FAFF);
  }

  public static Color bgAlt(){
    return new Color(0xF7F7F7);
  }

  public static Color ok(){
    return new Color(0x22C55E);
  }

  public static Color warn(){
    return new Color(0xF59E0B);
  }

  public static Color err(){
    return new Color(0xEF4444);
  }

  public static Color info(){
    return new Color(0x3B82F6);
  }
}
