package com.materiel.suite.backend.planning;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterventionDto(
    UUID id,
    UUID resourceId,
    String clientName,
    String siteLabel,
    String craneName,
    String truckName,
    String driverName,
    String agency,
    String status,
    boolean favorite,
    String quoteNumber,
    String orderNumber,
    String deliveryNumber,
    String invoiceNumber,
    String color,
    boolean locked,
    LocalDateTime dateHeureDebut,
    LocalDateTime dateHeureFin
) {}

