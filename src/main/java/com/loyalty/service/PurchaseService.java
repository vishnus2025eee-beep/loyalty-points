package com.loyalty.service;

import com.loyalty.config.LoyaltyProperties;
import com.loyalty.dto.PurchaseRequestDto;
import com.loyalty.dto.PurchaseResponseDto;
import com.loyalty.entity.*;
import com.loyalty.exception.BusinessRuleException;
import com.loyalty.repository.AuditLogRepository;
import com.loyalty.repository.CustomerRepository;
import com.loyalty.repository.PointsLedgerRepository;
import com.loyalty.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final TierService tierService;
    private final PurchaseRepository purchaseRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final AuditLogRepository auditLogRepository;
    private final LoyaltyProperties loyaltyProperties;

    @Transactional
    public PurchaseResponseDto processPurchase(Long customerId, PurchaseRequestDto dto) {
        Customer customer = customerService.getCustomerEntity(customerId);

        // 1. Validation BEFORE saving anything
        BigDecimal grossAmount = dto.amount();
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Purchase amount must be strictly greater than zero");
        }

        Long redeemPoints = (dto.redeemPoints() == null) ? 0L : dto.redeemPoints();
        if (redeemPoints < 0) {
            throw new BusinessRuleException("Points to redeem cannot be negative");
        }

        Tier currentTier = customer.getTier();
        BigDecimal tierDiscount = grossAmount.multiply(currentTier.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal remainingAfterTierDiscount = grossAmount.subtract(tierDiscount);

        if (redeemPoints > customer.getAvailablePoints()) {
            throw new BusinessRuleException(
                    "Insufficient points: Customer has " + customer.getAvailablePoints() +
                    " points available, but attempted to redeem " + redeemPoints + " points."
            );
        }

        BigDecimal rupeesPerPoint = loyaltyProperties.getRupeesPerPoint();
        BigDecimal redeemDiscount = BigDecimal.valueOf(redeemPoints)
                .multiply(rupeesPerPoint)
                .setScale(2, RoundingMode.HALF_UP);

        if (redeemDiscount.compareTo(remainingAfterTierDiscount) > 0) {
            long maxAllowedPoints = remainingAfterTierDiscount.divide(rupeesPerPoint, 0, RoundingMode.FLOOR).longValue();
            throw new BusinessRuleException(
                    "Redemption discount (Rs. " + redeemDiscount + ") exceeds payable amount after tier discount (Rs. " +
                    remainingAfterTierDiscount + "). Maximum redeemable points is " + maxAllowedPoints + " points."
            );
        }

        // 2. Calculations
        BigDecimal netAmount = remainingAfterTierDiscount.subtract(redeemDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pointsPerRupee = loyaltyProperties.getPointsPerRupee();
        long pointsEarned = netAmount.multiply(pointsPerRupee).setScale(0, RoundingMode.FLOOR).longValue();

        // 3. Save Purchase
        Purchase purchase = Purchase.builder()
                .customer(customer)
                .grossAmount(grossAmount)
                .tierDiscount(tierDiscount)
                .pointsRedeemed(redeemPoints)
                .redeemDiscount(redeemDiscount)
                .netAmount(netAmount)
                .pointsEarned(pointsEarned)
                .build();
        Purchase savedPurchase = purchaseRepository.save(purchase);

        // 4. Update balances and write ledger entries
        // Redeeming lowers only availablePoints
        if (redeemPoints > 0) {
            customer.setAvailablePoints(customer.getAvailablePoints() - redeemPoints);
            PointsLedger redeemLedger = PointsLedger.builder()
                    .customer(customer)
                    .purchase(savedPurchase)
                    .type(LedgerType.REDEEM)
                    .points(redeemPoints)
                    .balanceAfter(customer.getAvailablePoints())
                    .description("Redeemed " + redeemPoints + " points for Rs. " + redeemDiscount + " discount on Purchase #" + savedPurchase.getId())
                    .build();
            pointsLedgerRepository.save(redeemLedger);
        }

        // Earning raises both availablePoints and lifetimePoints
        customer.setAvailablePoints(customer.getAvailablePoints() + pointsEarned);
        customer.setLifetimePoints(customer.getLifetimePoints() + pointsEarned);

        PointsLedger earnLedger = PointsLedger.builder()
                .customer(customer)
                .purchase(savedPurchase)
                .type(LedgerType.EARN)
                .points(pointsEarned)
                .balanceAfter(customer.getAvailablePoints())
                .description("Earned " + pointsEarned + " points from Rs. " + netAmount + " net spend on Purchase #" + savedPurchase.getId())
                .build();
        pointsLedgerRepository.save(earnLedger);

        // 5. Auto-upgrade tier if lifetimePoints crosses higher threshold (NEVER downgrade)
        Tier oldTier = customer.getTier();
        Tier potentialTier = tierService.getTierForPoints(customer.getLifetimePoints());
        boolean upgraded = false;

        if (potentialTier.getMinLifetimePoints() > oldTier.getMinLifetimePoints()) {
            customer.setTier(potentialTier);
            upgraded = true;

            auditLogRepository.save(AuditLog.builder()
                    .action("TIER_UPGRADE")
                    .entity("Customer")
                    .entityId(customer.getId())
                    .details("Upgraded from " + oldTier.getName() + " to " + potentialTier.getName() +
                            " at " + customer.getLifetimePoints() + " lifetime points")
                    .build());
        }

        customerRepository.save(customer);

        // 6. Write AuditLog for Purchase
        auditLogRepository.save(AuditLog.builder()
                .action("PURCHASE")
                .entity("Purchase")
                .entityId(savedPurchase.getId())
                .details(String.format("Customer #%d spent Gross Rs. %s (Tier Disc: Rs. %s, Redeem Disc: Rs. %s, Net: Rs. %s). Points: Redeemed %d, Earned %d.",
                        customer.getId(), grossAmount, tierDiscount, redeemDiscount, netAmount, redeemPoints, pointsEarned))
                .build());

        return new PurchaseResponseDto(
                savedPurchase.getId(),
                customer.getId(),
                customer.getName(),
                grossAmount,
                tierDiscount,
                redeemPoints,
                redeemDiscount,
                netAmount,
                pointsEarned,
                customer.getAvailablePoints(),
                customer.getLifetimePoints(),
                oldTier.getName(),
                customer.getTier().getName(),
                upgraded,
                savedPurchase.getCreatedAt()
        );
    }
}
