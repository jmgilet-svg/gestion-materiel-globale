package com.materiel.suite.client.service;

import com.materiel.suite.client.model.TimelineEvent;

import java.util.List;

/** Service simple pour interagir avec l'historique des interventions. */
public interface TimelineService {
  List<TimelineEvent> list(String interventionId);

  TimelineEvent append(String interventionId, TimelineEvent event);
}
