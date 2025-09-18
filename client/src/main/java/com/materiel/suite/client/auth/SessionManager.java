package com.materiel.suite.client.auth;

import com.materiel.suite.client.service.ServiceLocator;
import com.materiel.suite.client.ui.auth.LoginDialog;
import com.materiel.suite.client.ui.common.Toasts;
import com.materiel.suite.client.util.Prefs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.util.Timer;
import java.util.TimerTask;

/** Gère l'expiration automatique de la session après inactivité. */
public final class SessionManager {
  private static final Object LOCK = new Object();
  private static Timer timer;
  private static long timeoutMs = 30 * 60 * 1000L;
  private static Window attachedWindow;
  private static AWTEventListener listener;

  private SessionManager(){
  }

  public interface SessionAware {
    void onSessionRefreshed();
  }

  public static void install(JFrame frame){
    if (frame == null){
      return;
    }
    synchronized (LOCK){
      attachedWindow = frame;
      timeoutMs = Math.max(1, Prefs.getSessionTimeoutMinutes()) * 60L * 1000L;
      ensureListener();
      restartTimer();
    }
  }

  public static void setTimeoutMinutes(int minutes){
    synchronized (LOCK){
      timeoutMs = Math.max(1, minutes) * 60L * 1000L;
      restartTimer();
    }
  }

  private static void ensureListener(){
    if (listener != null){
      return;
    }
    listener = event -> {
      if (event == null){
        return;
      }
      if (AuthContext.isLogged()){
        restartTimer();
      }
    };
    long mask = AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK;
    Toolkit.getDefaultToolkit().addAWTEventListener(listener, mask);
  }

  private static void restartTimer(){
    synchronized (LOCK){
      cancelTimer();
      if (attachedWindow == null){
        return;
      }
      timer = new Timer("session-timeout", true);
      timer.schedule(new TimerTask() {
        @Override
        public void run(){
          handleTimeout();
        }
      }, timeoutMs);
    }
  }

  private static void cancelTimer(){
    if (timer != null){
      timer.cancel();
      timer = null;
    }
  }

  private static void handleTimeout(){
    Window window;
    synchronized (LOCK){
      window = attachedWindow;
      cancelTimer();
    }
    if (window == null || !AuthContext.isLogged()){
      restartTimer();
      return;
    }
    SwingUtilities.invokeLater(() -> {
      try {
        AuthService auth = ServiceLocator.auth();
        if (auth != null){
          auth.logout();
        }
      } catch (Exception ignore){
      }
      Toasts.info(window, "Session expirée, veuillez vous reconnecter.");
      LoginDialog.require(window);
      if (AuthContext.isLogged() && window instanceof SessionAware aware){
        aware.onSessionRefreshed();
      }
      restartTimer();
    });
  }
}
