package com.loyalty.dto;

import java.math.BigDecimal;

public record CustomerSummaryDto(
        Long customerId,
        String customerName,
        String tier,
        BigDecimal discountPercent,
        Long availablePoints,
        Long lifetimePoints,
        Long pointsToNextTier,
        String nextTierName
) {}
