package com.loyalty.controller;

import com.loyalty.dto.*;
import com.loyalty.service.CustomerService;
import com.loyalty.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management and loyalty operations")
public class CustomerController {

    private final CustomerService customerService;
    private final PurchaseService purchaseService;

    @PostMapping
    @Operation(summary = "Register a new customer with Base tier")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerRequestDto dto) {
        CustomerResponseDto created = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all registered customers")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping("/{id}/purchases")
    @Operation(summary = "Process a new retail purchase with tier discount and points redemption")
    public ResponseEntity<PurchaseResponseDto> processPurchase(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestDto dto) {
        PurchaseResponseDto response = purchaseService.processPurchase(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/points-history")
    @Operation(summary = "Get paginated points ledger history for customer (newest first)")
    public ResponseEntity<Page<PointsLedgerDto>> getPointsHistory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(customerService.getPointsHistory(id, pageable));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Get customer loyalty summary with next tier progression")
    public ResponseEntity<CustomerSummaryDto> getCustomerSummary(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerSummary(id));
    }
}
