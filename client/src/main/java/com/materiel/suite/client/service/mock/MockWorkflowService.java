package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.*;
import com.materiel.suite.client.service.DocumentWorkflowService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock local : applique les transitions en mémoire.
 */
public class MockWorkflowService implements DocumentWorkflowService {
  // Notre mock déjà existant stocke les objets dans les Mock*Service correspondants.
  // Ici on garde juste un cache "version" pour simuler If-Match basiquement.
  private final Map<UUID, Long> versions = new ConcurrentHashMap<>();
  private long bump(UUID id, long v){ long nv = Math.max(versions.getOrDefault(id, v), v) + 1; versions.put(id, nv); return nv; }

  @Override public void orderConfirm(UUID id, long v){ /* no-op: UI rafraîchira depuis service mock */ bump(id,v); }
  @Override public void orderLock(UUID id, long v){ bump(id,v); }
  @Override public void orderCancel(UUID id, long v){ bump(id,v); }
  @Override public void deliveryDeliver(UUID id, long v){ bump(id,v); }
  @Override public void deliveryLock(UUID id, long v){ bump(id,v); }
  @Override public void deliveryCancel(UUID id, long v){ bump(id,v); }
  @Override public void invoiceIssue(UUID id, long v){ bump(id,v); }
  @Override public void invoicePay(UUID id, long v){ bump(id,v); }
  @Override public void invoiceCancel(UUID id, long v){ bump(id,v); }
}

