package com.loyalty.dto;

import java.time.LocalDateTime;

public record CustomerResponseDto(
        Long id,
        String name,
        String email,
        String phone,
        Long lifetimePoints,
        Long availablePoints,
        TierDto tier,
        LocalDateTime createdAt
) {}
