package com.loyalty.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseResponseDto(
        Long purchaseId,
        Long customerId,
        String customerName,
        BigDecimal grossAmount,
        BigDecimal tierDiscount,
        Long pointsRedeemed,
        BigDecimal redeemDiscount,
        BigDecimal netAmount,
        Long pointsEarned,
        Long availablePointsAfter,
        Long lifetimePointsAfter,
        String oldTier,
        String newTier,
        boolean upgraded,
        LocalDateTime createdAt
) {}
