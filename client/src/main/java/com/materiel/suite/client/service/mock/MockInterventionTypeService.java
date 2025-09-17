package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.service.InterventionTypeService;

import java.util.ArrayList;
import java.util.List;

/** Fournit un catalogue statique pour l'utilisation hors-ligne. */
public class MockInterventionTypeService implements InterventionTypeService {
  private static final List<InterventionType> DEFAULTS = List.of(
      new InterventionType("LIFT", "Levage", "crane"),
      new InterventionType("TRANSPORT", "Transport", "truck"),
      new InterventionType("MANUT", "Manutention", "forklift")
  );

  @Override
  public List<InterventionType> list(){
    return new ArrayList<>(DEFAULTS);
  }
}
