package com.loyalty.dto;

import java.math.BigDecimal;

public record TierDto(
        Long id,
        String name,
        Long minLifetimePoints,
        BigDecimal discountPercent
) {}
