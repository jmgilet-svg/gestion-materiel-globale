package com.materiel.suite.client.ui.theme;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.settings.GeneralSettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
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
    float scale = clampScale(safe.getUiScalePercent());
    applyFontScale(scale);
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

  private static void applyFontScale(float scale){
    UIDefaults defaults = UIManager.getDefaults();
    if (BASE_FONTS.isEmpty()){
      Enumeration<Object> keys = defaults.keys();
      while (keys.hasMoreElements()){
        Object key = keys.nextElement();
        Object value = defaults.get(key);
        if (value instanceof Font font){
          BASE_FONTS.put(key, font);
        }
      }
    }
    for (Map.Entry<Object, Font> entry : BASE_FONTS.entrySet()){
      Font base = entry.getValue();
      if (base != null){
        defaults.put(entry.getKey(), new FontUIResource(base.deriveFont(base.getSize2D() * scale)));
      }
    }
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
