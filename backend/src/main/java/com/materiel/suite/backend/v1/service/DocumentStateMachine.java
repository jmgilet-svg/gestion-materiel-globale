package com.materiel.suite.backend.v1.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.materiel.suite.backend.v1.domain.DocumentStatus;

/**
 * Machine à états simple et déterministe pour les documents.
 * - ORDER : DRAFT -> CONFIRMED -> LOCKED ; DRAFT -> CANCELED
 * - DELIVERY NOTE : DRAFT -> DELIVERED -> LOCKED ; DRAFT -> CANCELED
 * - INVOICE : DRAFT -> ISSUED -> PAID ; DRAFT -> CANCELED ; ISSUED -> CANCELED
 *
 * Remarque : les états sont représentés par DocumentStatus pour homogénéiser.
 */
public class DocumentStateMachine {
  public enum Action {
    SEND,          // e.g. envoyer un devis (optionnel ici)
    CONFIRM,       // Order
    DELIVER,       // DeliveryNote
    ISSUE,         // Invoice
    PAY,           // Invoice
    ACCEPT,        // (support générique)
    REFUSE,        // (support générique)
    CANCEL,        // all
    LOCK           // verrouiller (final)
  }

  private final Map<DocumentStatus, Set<Action>> allowed = new EnumMap<>(DocumentStatus.class);
  private final Map<TransitionKey, DocumentStatus> transitions = new HashMap<TransitionKey, DocumentStatus>();

  public DocumentStateMachine(){
    // Par défaut, rien n'est autorisé -> on remplit explicitement
    for (DocumentStatus s : DocumentStatus.values()) {
      allowed.put(s, EnumSet.noneOf(Action.class));
    }
    // Génériques
    allow(DocumentStatus.DRAFT, Action.CANCEL, DocumentStatus.CANCELED);
    allow(DocumentStatus.LOCKED, Action.LOCK, DocumentStatus.LOCKED); // idempotent

    // Orders
    allow(DocumentStatus.DRAFT, Action.CONFIRM, DocumentStatus.CONFIRMED);
    allow(DocumentStatus.CONFIRMED, Action.LOCK, DocumentStatus.LOCKED);

    // DeliveryNotes
    allow(DocumentStatus.DRAFT, Action.DELIVER, DocumentStatus.DELIVERED);
    allow(DocumentStatus.DELIVERED, Action.LOCK, DocumentStatus.LOCKED);

    // Invoices
    allow(DocumentStatus.DRAFT, Action.ISSUE, DocumentStatus.ISSUED);
    allow(DocumentStatus.ISSUED, Action.PAY, DocumentStatus.PAID);
    allow(DocumentStatus.DRAFT, Action.CANCEL, DocumentStatus.CANCELED);
    allow(DocumentStatus.ISSUED, Action.CANCEL, DocumentStatus.CANCELED);
  }

  public boolean isAllowed(DocumentStatus from, Action action){
    return allowed.getOrDefault(from, Set.of()).contains(action);
  }
  public DocumentStatus next(DocumentStatus from, Action action){
    if (!isAllowed(from, action)) throw new IllegalStateException("Transition non autorisée: "+from+" -> "+action);
    return transitions.get(new TransitionKey(from, action));
  }

  private void allow(DocumentStatus from, Action action, DocumentStatus to){
    allowed.computeIfAbsent(from, k -> EnumSet.noneOf(Action.class)).add(action);
    transitions.put(new TransitionKey(from, action), to);
  }

  public record TransitionKey(DocumentStatus from, Action action) {}
}

