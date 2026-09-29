package com.loyalty.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record PurchaseRequestDto(
        @NotNull(message = "Gross amount is required")
        @Positive(message = "Gross amount must be greater than zero")
        BigDecimal amount,

        @PositiveOrZero(message = "Redeem points cannot be negative")
        Long redeemPoints
) {}
