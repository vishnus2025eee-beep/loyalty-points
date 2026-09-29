package com.loyalty.controller;

import com.loyalty.dto.TierDto;
import com.loyalty.service.TierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
@Tag(name = "Tiers", description = "Endpoints for loyalty tier tiers metadata")
public class TierController {

    private final TierService tierService;

    @GetMapping
    @Operation(summary = "Get all loyalty tiers (Cached)")
    public ResponseEntity<List<TierDto>> getAllTiers() {
        return ResponseEntity.ok(tierService.getAllTiers());
    }
}
