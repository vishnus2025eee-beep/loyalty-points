package com.loyalty.controller;

import com.loyalty.config.LoyaltyProperties;
import com.loyalty.dto.LoyaltyConfigDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Configuration", description = "Current loyalty conversion rates")
public class ConfigController {

    private final LoyaltyProperties loyaltyProperties;

    @GetMapping
    @Operation(summary = "Get current loyalty conversion rates for UI live calculations")
    public ResponseEntity<LoyaltyConfigDto> getConfig() {
        return ResponseEntity.ok(new LoyaltyConfigDto(
                loyaltyProperties.getPointsPerRupee(),
                loyaltyProperties.getRupeesPerPoint()
        ));
    }
}
