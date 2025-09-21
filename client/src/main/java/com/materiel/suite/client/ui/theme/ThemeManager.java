package com.materiel.suite.client.ui.theme;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.GeneralSettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge FlatLaf si disponible, sinon laisse le LAF système, puis applique les options UI locales
 * (contraste, échelle, délais tooltips…).
 */
public final class ThemeManager {
  private static boolean dark = false;
  private static final Map<Object, Font> BASE_FONTS = new HashMap<>();
  private static final Map<String, Object> BASE_OVERRIDES = new HashMap<>();
  private static Font DYSLEXIA_BASE_FONT;
  private static boolean DYSLEXIA_LOAD_FAILED;

  private ThemeManager(){}

  public static void applyInitial(){
    try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore){}
    tryLoad(false);
    applyGeneralSettings(loadSettingsSafely());
  }

  public static void toggleDark(){
    dark = !dark;
    tryLoad(dark);
    applyGeneralSettings();
    refreshAllFrames();
  }

  private static void tryLoad(boolean darkMode){
    try {
      String cls = darkMode ? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf";
      Class<?> laf = Class.forName(cls);
      BASE_FONTS.clear();
      BASE_OVERRIDES.clear();
      laf.getMethod("setup").invoke(null);
    } catch(Throwable ignore){}
  }

  public static void applyGeneralSettings(){
    applyGeneralSettings(loadSettingsSafely());
  }

  public static void applyGeneralSettings(GeneralSettings settings){
    GeneralSettings safe = settings != null ? settings : new GeneralSettings();
    applyFontSettings(safe);
    applyBranding(safe.getBrandPrimaryHex());
    applyHighContrast(safe.isHighContrast());
    configureTooltips();
  }

  public static void refreshAllFrames(){
    for (Frame frame : Frame.getFrames()){
      SwingUtilities.updateComponentTreeUI(frame);
      frame.invalidate();
      frame.validate();
      frame.repaint();
    }
  }

  private static GeneralSettings loadSettingsSafely(){
    try {
      GeneralSettings settings = ServiceLocator.settings().getGeneral();
      return settings != null ? settings : new GeneralSettings();
    } catch (RuntimeException ex){
      return new GeneralSettings();
    }
  }

  private static float clampScale(int percent){
    int sanitized = Math.max(80, Math.min(130, percent));
    return sanitized / 100f;
  }

  private static void applyFontSettings(GeneralSettings settings){
    float scale = clampScale(settings.getUiScalePercent());
    ensureBaseFonts();
    Font dyslexia = settings.isDyslexiaMode() ? loadDyslexiaFont(defaultFontSize() * scale) : null;
    if (dyslexia != null){
      applyUniformFont(dyslexia);
    } else {
      applyFontScale(scale);
    }
  }

  private static void ensureBaseFonts(){
    if (!BASE_FONTS.isEmpty()){
      return;
    }
    UIDefaults defaults = UIManager.getDefaults();
    Enumeration<Object> keys = defaults.keys();
    while (keys.hasMoreElements()){
      Object key = keys.nextElement();
      Object value = defaults.get(key);
      if (value instanceof Font font){
        BASE_FONTS.put(key, font);
      }
    }
  }

  private static void applyFontScale(float scale){
    ensureBaseFonts();
    UIDefaults defaults = UIManager.getDefaults();
    for (Map.Entry<Object, Font> entry : BASE_FONTS.entrySet()){
      Font base = entry.getValue();
      if (base != null){
        defaults.put(entry.getKey(), new FontUIResource(base.deriveFont(base.getSize2D() * scale)));
      }
    }
  }

  private static void applyUniformFont(Font font){
    if (font == null){
      return;
    }
    FontUIResource resource = new FontUIResource(font);
    UIDefaults defaults = UIManager.getDefaults();
    Enumeration<Object> keys = defaults.keys();
    while (keys.hasMoreElements()){
      Object key = keys.nextElement();
      Object value = defaults.get(key);
      if (value instanceof Font){
        defaults.put(key, resource);
      }
    }
  }

  private static void applyBranding(String hex){
    Color brand = parseColor(hex);
    if (brand == null){
      override("Component.accentColor", null);
      override("Component.linkColor", null);
      override("Component.linkHoverColor", null);
      override("Button.default.background", null);
      override("Button.default.focusColor", null);
      override("Button.default.foreground", null);
      override("ProgressBar.foreground", null);
      override("CheckBox.icon.selectedBackground", null);
      override("RadioButton.icon.selectedBackground", null);
      override("ToggleButton.icon.selectedBackground", null);
      return;
    }
    ColorUIResource accent = new ColorUIResource(brand);
    override("Component.accentColor", accent);
    override("Component.linkColor", accent);
    override("Component.linkHoverColor", new ColorUIResource(brand.brighter()));
    override("Button.default.background", accent);
    override("Button.default.focusColor", new ColorUIResource(brand.darker()));
    override("Button.default.foreground", new ColorUIResource(readableForeground(brand)));
    override("ProgressBar.foreground", accent);
    override("CheckBox.icon.selectedBackground", accent);
    override("RadioButton.icon.selectedBackground", accent);
    override("ToggleButton.icon.selectedBackground", accent);
  }

  private static float defaultFontSize(){
    Font label = BASE_FONTS.get("Label.font");
    if (label != null){
      return label.getSize2D();
    }
    Font fallback = UIManager.getFont("Label.font");
    if (fallback != null){
      return fallback.getSize2D();
    }
    return 14f;
  }

  private static Font loadDyslexiaFont(float size){
    Font base = dyslexiaBaseFont();
    if (base == null){
      return null;
    }
    return base.deriveFont(size);
  }

  private static Font dyslexiaBaseFont(){
    if (DYSLEXIA_BASE_FONT != null){
      return DYSLEXIA_BASE_FONT;
    }
    if (DYSLEXIA_LOAD_FAILED){
      return null;
    }
    try (InputStream is = ThemeManager.class.getResourceAsStream("/fonts/OpenDyslexic3-Regular.otf")){
      if (is == null){
        DYSLEXIA_LOAD_FAILED = true;
        return null;
      }
      DYSLEXIA_BASE_FONT = Font.createFont(Font.TRUETYPE_FONT, is);
      return DYSLEXIA_BASE_FONT;
    } catch (Exception ex){
      DYSLEXIA_LOAD_FAILED = true;
      DYSLEXIA_BASE_FONT = null;
      return null;
    }
  }

  private static Color parseColor(String hex){
    if (hex == null){
      return null;
    }
    String trimmed = hex.trim();
    if (trimmed.isEmpty()){
      return null;
    }
    String normalized = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    try {
      if (normalized.length() == 6){
        int rgb = Integer.parseInt(normalized, 16);
        return new Color(rgb);
      }
      if (normalized.length() == 8){
        long rgba = Long.parseLong(normalized, 16);
        int alpha = (int) ((rgba >> 24) & 0xFF);
        int red = (int) ((rgba >> 16) & 0xFF);
        int green = (int) ((rgba >> 8) & 0xFF);
        int blue = (int) (rgba & 0xFF);
        return new Color(red, green, blue, alpha);
      }
    } catch (NumberFormatException ignore){
      return null;
    }
    return null;
  }

  private static Color readableForeground(Color color){
    double r = srgb(color.getRed() / 255.0);
    double g = srgb(color.getGreen() / 255.0);
    double b = srgb(color.getBlue() / 255.0);
    double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    return luminance < 0.5 ? Color.WHITE : Color.BLACK;
  }

  private static double srgb(double channel){
    if (channel <= 0.03928){
      return channel / 12.92;
    }
    return Math.pow((channel + 0.055) / 1.055, 2.4);
  }

  private static void applyHighContrast(boolean enabled){
    Color focusColor = enabled ? (dark ? new Color(0x90CAF9) : new Color(0x0B0B0B)) : null;
    Color selectionBg = enabled ? (dark ? new Color(0x264F78) : new Color(0xCCE5FF)) : null;
    Color selectionFg = enabled ? (dark ? Color.WHITE : Color.BLACK) : null;
    Color labelColor = enabled ? (dark ? new Color(0xFAFAFA) : new Color(0x0B0B0B)) : null;
    Border focusBorder = enabled ? new LineBorder(focusColor != null ? focusColor : Color.BLACK, 2, true) : null;

    override("Component.focusWidth", enabled ? 2.0f : null);
    override("Component.innerFocusWidth", enabled ? 1.0f : null);
    override("Component.focusColor", focusColor);
    override("Component.innerFocusColor", focusColor);
    override("Button.focusedBorderColor", focusColor);
    override("ToggleButton.focusedBorderColor", focusColor);
    override("TabbedPane.focusColor", focusColor);
    override("Table.focusCellHighlightBorder", focusBorder);
    override("Table.showHorizontalLines", enabled ? Boolean.TRUE : null);
    override("Table.showVerticalLines", enabled ? Boolean.TRUE : null);
    override("Table.gridColor", enabled ? new Color(0xBDBDBD) : null);
    override("List.selectionBackground", selectionBg);
    override("Tree.selectionBackground", selectionBg);
    override("Table.selectionBackground", selectionBg);
    override("List.selectionForeground", selectionFg);
    override("Tree.selectionForeground", selectionFg);
    override("Table.selectionForeground", selectionFg);
    override("Label.foreground", labelColor);
  }

  private static void configureTooltips(){
    ToolTipManager.sharedInstance().setInitialDelay(350);
    ToolTipManager.sharedInstance().setDismissDelay(12_000);
  }

  private static void override(String key, Object value){
    if (key == null){
      return;
    }
    UIDefaults defaults = UIManager.getDefaults();
    if (!BASE_OVERRIDES.containsKey(key)){
      BASE_OVERRIDES.put(key, defaults.get(key));
    }
    if (value != null){
      UIManager.put(key, value);
    } else {
      Object base = BASE_OVERRIDES.get(key);
      if (base != null){
        UIManager.put(key, base);
      } else {
        UIManager.getDefaults().remove(key);
      }
    }
  }
}
