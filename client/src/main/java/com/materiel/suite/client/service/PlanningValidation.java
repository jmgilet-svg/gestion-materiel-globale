package com.materiel.suite.client.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Result of a planning validation.
 * It may contain suggestions in case of conflicts.
 */
public class PlanningValidation {
  public boolean ok;
  public List<Suggestion> suggestions = new ArrayList<>();

  public static PlanningValidation ok(){
    PlanningValidation v = new PlanningValidation();
    v.ok = true;
    return v;
  }

  public static class Suggestion {
    public UUID resourceId;
    public LocalDateTime startDateTime;
    public LocalDateTime endDateTime;
    public String label;
  }
}
