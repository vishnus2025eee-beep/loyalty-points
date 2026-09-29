package com.loyalty.service;

import com.loyalty.dto.CustomerRequestDto;
import com.loyalty.dto.CustomerResponseDto;
import com.loyalty.dto.CustomerSummaryDto;
import com.loyalty.dto.PointsLedgerDto;
import com.loyalty.entity.AuditLog;
import com.loyalty.entity.Customer;
import com.loyalty.entity.PointsLedger;
import com.loyalty.entity.Tier;
import com.loyalty.exception.DuplicateResourceException;
import com.loyalty.exception.ResourceNotFoundException;
import com.loyalty.repository.AuditLogRepository;
import com.loyalty.repository.CustomerRepository;
import com.loyalty.repository.PointsLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TierService tierService;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public CustomerResponseDto createCustomer(CustomerRequestDto dto) {
        if (customerRepository.existsByEmail(dto.email())) {
            throw new DuplicateResourceException("Customer with email '" + dto.email() + "' already exists");
        }

        Tier baseTier = tierService.getBaseTier();

        Customer customer = Customer.builder()
                .name(dto.name().trim())
                .email(dto.email().trim().toLowerCase())
                .phone(dto.phone() != null ? dto.phone().trim() : null)
                .lifetimePoints(0L)
                .availablePoints(0L)
                .tier(baseTier)
                .build();

        Customer saved = customerRepository.save(customer);

        auditLogRepository.save(AuditLog.builder()
                .action("CUSTOMER_CREATED")
                .entity("Customer")
                .entityId(saved.getId())
                .details("Registered customer " + saved.getName() + " (" + saved.getEmail() + ") with Base tier")
                .build());

        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        return toResponseDto(getCustomerEntity(id));
    }

    @Transactional(readOnly = true)
    public CustomerSummaryDto getCustomerSummary(Long id) {
        Customer customer = getCustomerEntity(id);
        List<Tier> allTiers = tierService.getAllTierEntities();

        Tier currentTier = customer.getTier();
        Tier nextTier = null;

        for (Tier tier : allTiers) {
            if (tier.getMinLifetimePoints() > currentTier.getMinLifetimePoints()) {
                if (nextTier == null || tier.getMinLifetimePoints() < nextTier.getMinLifetimePoints()) {
                    nextTier = tier;
                }
            }
        }

        Long pointsToNextTier = 0L;
        String nextTierName = null;
        if (nextTier != null) {
            pointsToNextTier = Math.max(0L, nextTier.getMinLifetimePoints() - customer.getLifetimePoints());
            nextTierName = nextTier.getName();
        }

        return new CustomerSummaryDto(
                customer.getId(),
                customer.getName(),
                currentTier.getName(),
                currentTier.getDiscountPercent(),
                customer.getAvailablePoints(),
                customer.getLifetimePoints(),
                pointsToNextTier,
                nextTierName
        );
    }

    @Transactional(readOnly = true)
    public Page<PointsLedgerDto> getPointsHistory(Long customerId, Pageable pageable) {
        // Ensure customer exists
        getCustomerEntity(customerId);

        // Ensure default sort is createdAt DESC if not provided or unsorted
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }

        return pointsLedgerRepository.findByCustomerId(customerId, effectivePageable)
                .map(this::toLedgerDto);
    }

    public CustomerResponseDto toResponseDto(Customer customer) {
        return new CustomerResponseDto(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getLifetimePoints(),
                customer.getAvailablePoints(),
                tierService.toDto(customer.getTier()),
                customer.getCreatedAt()
        );
    }

    private PointsLedgerDto toLedgerDto(PointsLedger ledger) {
        return new PointsLedgerDto(
                ledger.getId(),
                ledger.getCustomer().getId(),
                ledger.getPurchase() != null ? ledger.getPurchase().getId() : null,
                ledger.getType(),
                ledger.getPoints(),
                ledger.getBalanceAfter(),
                ledger.getDescription(),
                ledger.getCreatedAt()
        );
    }
}
