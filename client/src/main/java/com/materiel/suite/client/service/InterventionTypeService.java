package com.materiel.suite.client.service;

import com.materiel.suite.client.model.InterventionType;

import java.util.List;

/** Service catalogue des types d'intervention. */
public interface InterventionTypeService {
  /** Retourne la liste des types disponibles (peut être vide). */
  List<InterventionType> list();

  /** Crée ou met à jour un type d'intervention. */
  InterventionType save(InterventionType type);

  /** Supprime le type correspondant au code fourni. */
  void delete(String code);
}
