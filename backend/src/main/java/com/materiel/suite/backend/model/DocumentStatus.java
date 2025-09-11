package com.materiel.suite.backend.model;

public enum DocumentStatus {
    // Quote
    QUOTE_DRAFT,
    QUOTE_SENT,
    QUOTE_ACCEPTED,
    QUOTE_REFUSED,
    QUOTE_EXPIRED,

    // Order
    ORDER_DRAFT,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,

    // Delivery note
    DELIVERY_NOTE_DRAFT,
    DELIVERY_NOTE_SIGNED,
    DELIVERY_NOTE_LOCKED,

    // Invoice
    INVOICE_DRAFT,
    INVOICE_SENT,
    INVOICE_PARTIALLY_PAID,
    INVOICE_PAID,
    INVOICE_CANCELLED
}
