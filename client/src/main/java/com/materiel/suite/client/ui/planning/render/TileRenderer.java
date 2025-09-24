package com.materiel.suite.client.ui.planning.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.materiel.suite.client.model.Intervention;

/** Contrat minimal pour « skinner » une tuile d’intervention. */
public interface TileRenderer {

  /** Contexte optionnel pour piloter le rendu. */
  record State(boolean selected, boolean hovered, boolean hasQuote, String status, String agency,
               String smallIconKey){}

  /** Dessine la tuile dans le rectangle donné (coordonnées pixels du board). */
  void paintTile(Graphics2D g2, Intervention it, Rectangle bounds, State state);

  /* --------- Utilitaires par défaut --------- */
  default State inferState(Intervention it, boolean selected){
    boolean quoted = it != null && it.hasQuote();
    String status = it != null && it.getStatus() != null ? it.getStatus() : "";
    String agency = it != null ? it.getAgency() : null;
    return new State(selected, false, quoted, status, agency, null);
  }
}
