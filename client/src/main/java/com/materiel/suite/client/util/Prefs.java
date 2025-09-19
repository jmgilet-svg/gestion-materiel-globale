package com.materiel.suite.client.util;

import java.util.prefs.Preferences;

/** Accès simplifié aux préférences utilisateur côté client. */
public final class Prefs {
  private static final Preferences PREFS = Preferences.userRoot().node("gestion-materiel");

  private Prefs(){
  }

  public static int getSessionTimeoutMinutes(){
    return Math.max(1, PREFS.getInt("session.timeout.minutes", 30));
  }

  public static void setSessionTimeoutMinutes(int minutes){
    PREFS.putInt("session.timeout.minutes", Math.max(1, minutes));
  }

  public static int getAutosaveIntervalSeconds(){
    return Math.max(5, PREFS.getInt("autosave.interval.seconds", 30));
  }

  public static void setAutosaveIntervalSeconds(int seconds){
    PREFS.putInt("autosave.interval.seconds", Math.max(5, seconds));
  }
}
