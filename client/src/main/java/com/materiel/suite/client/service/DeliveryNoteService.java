package com.materiel.suite.client.service;

import com.materiel.suite.client.model.DeliveryNote;

import java.util.List;
import java.util.UUID;

public interface DeliveryNoteService {
  List<DeliveryNote> list();
  DeliveryNote get(UUID id);
  DeliveryNote save(DeliveryNote dn);
  void delete(UUID id);
  DeliveryNote createFromOrder(UUID orderId);
}
