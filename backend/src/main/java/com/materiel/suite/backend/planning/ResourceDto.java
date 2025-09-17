package com.materiel.suite.backend.planning;

import java.math.BigDecimal;
import java.util.UUID;

public record ResourceDto(
    UUID id,
    String name,
    String icon,
    BigDecimal unitPriceHt
) {}

