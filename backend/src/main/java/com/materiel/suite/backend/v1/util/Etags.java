package com.materiel.suite.backend.v1.util;

public final class Etags {
  private Etags(){}
  public static String weakOfVersion(long version){
    return "W/\"" + version + "\"";
  }
  public static boolean matches(String ifMatchHeader, String current){
    if (ifMatchHeader==null) return false;
    // gestion If-Match avec éventuelle liste
    String[] parts = ifMatchHeader.split(",");
    for (String p : parts){
      if (p.trim().equals(current)) return true;
    }
    return false;
  }
}
