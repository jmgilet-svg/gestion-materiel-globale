package com.materiel.suite.backend.v1.domain;

public enum DocumentStatus {
  DRAFT,
  SENT,
  ACCEPTED,
  REFUSED,
  CONFIRMED,
  DELIVERED,
  ISSUED,
  PAID,
  CANCELED,
  LOCKED,
  ARCHIVED;

  public boolean canTransitionTo(DocumentStatus target){
    return switch (this){
      case DRAFT -> target==SENT || target==ARCHIVED;
      case SENT -> target==ACCEPTED || target==REFUSED || target==ARCHIVED;
      case ACCEPTED -> target==LOCKED || target==ARCHIVED;
      case REFUSED -> target==ARCHIVED;
      case LOCKED -> target==ARCHIVED;
      case ARCHIVED -> false;
      default -> false; // For other statuses, transitions handled elsewhere
    };
  }
}
