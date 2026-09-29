package com.loyalty.dto;

import java.math.BigDecimal;

public record LoyaltyConfigDto(
        BigDecimal pointsPerRupee,
        BigDecimal rupeesPerPoint
) {}
