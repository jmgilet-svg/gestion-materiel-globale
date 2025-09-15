package com.materiel.suite.client.ui.planning;

/** Simple UI density helper for planning views. */
public enum UiDensity {
  COMPACT(18), NORMAL(26), SPACIOUS(34);

  /** Base tile height in pixels for this density. */
  public final int baseTileHeight;

  UiDensity(int h){ this.baseTileHeight = h; }

  /** Parse from name, defaulting to NORMAL when unknown. */
  public static UiDensity fromString(String s){
    try {
      return UiDensity.valueOf(String.valueOf(s).toUpperCase());
    } catch(Exception e){
      return NORMAL;
    }
  }
}
