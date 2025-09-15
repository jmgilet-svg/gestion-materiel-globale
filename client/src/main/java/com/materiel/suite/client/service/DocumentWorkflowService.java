package com.materiel.suite.client.service;

import java.util.UUID;

/**
 * Service unique pour piloter les transitions de workflow côté client.
 * Implémentations : ApiWorkflowService (backend) et MockWorkflowService (hors-ligne).
 */
public interface DocumentWorkflowService {
  // Orders
  void orderConfirm(UUID orderId, long version) throws Exception;
  void orderLock(UUID orderId, long version) throws Exception;
  void orderCancel(UUID orderId, long version) throws Exception;

  // Delivery notes
  void deliveryDeliver(UUID deliveryId, long version) throws Exception;
  void deliveryLock(UUID deliveryId, long version) throws Exception;
  void deliveryCancel(UUID deliveryId, long version) throws Exception;

  // Invoices
  void invoiceIssue(UUID invoiceId, long version) throws Exception;
  void invoicePay(UUID invoiceId, long version) throws Exception;
  void invoiceCancel(UUID invoiceId, long version) throws Exception;
}

