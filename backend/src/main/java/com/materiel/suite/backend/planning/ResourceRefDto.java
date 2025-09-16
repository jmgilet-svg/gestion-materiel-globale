package com.materiel.suite.backend.planning;

import java.util.UUID;

public record ResourceRefDto(
    UUID id,
    String name,
    String icon
) {}
