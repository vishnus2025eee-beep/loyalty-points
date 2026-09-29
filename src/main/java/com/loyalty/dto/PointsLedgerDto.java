package com.loyalty.dto;

import com.loyalty.entity.LedgerType;

import java.time.LocalDateTime;

public record PointsLedgerDto(
        Long id,
        Long customerId,
        Long purchaseId,
        LedgerType type,
        Long points,
        Long balanceAfter,
        String description,
        LocalDateTime createdAt
) {}
