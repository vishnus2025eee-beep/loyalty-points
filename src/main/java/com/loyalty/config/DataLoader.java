package com.loyalty.config;

import com.loyalty.entity.*;
import com.loyalty.repository.AuditLogRepository;
import com.loyalty.repository.CustomerRepository;
import com.loyalty.repository.PointsLedgerRepository;
import com.loyalty.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final TierRepository tierRepository;
    private final CustomerRepository customerRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedTiers();
        seedDemoCustomers();
    }

    private void seedTiers() {
        if (tierRepository.count() == 0) {
            log.info("Seeding loyalty tiers: Base, Silver, Gold, Platinum...");
            tierRepository.save(Tier.builder()
                    .name("Base")
                    .minLifetimePoints(0L)
                    .discountPercent(new BigDecimal("0.00"))
                    .createdAt(LocalDateTime.now())
                    .build());

            tierRepository.save(Tier.builder()
                    .name("Silver")
                    .minLifetimePoints(500L)
                    .discountPercent(new BigDecimal("5.00"))
                    .createdAt(LocalDateTime.now())
                    .build());

            tierRepository.save(Tier.builder()
                    .name("Gold")
                    .minLifetimePoints(2000L)
                    .discountPercent(new BigDecimal("10.00"))
                    .createdAt(LocalDateTime.now())
                    .build());

            tierRepository.save(Tier.builder()
                    .name("Platinum")
                    .minLifetimePoints(5000L)
                    .discountPercent(new BigDecimal("15.00"))
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("Loyalty tiers seeded successfully.");
        }
    }

    private void seedDemoCustomers() {
        if (customerRepository.count() == 0) {
            log.info("Seeding 3 demo customers with realistic points and tiers...");
            Tier baseTier = tierRepository.findByName("Base").orElseThrow();
            Tier silverTier = tierRepository.findByName("Silver").orElseThrow();
            Tier goldTier = tierRepository.findByName("Gold").orElseThrow();

            // Demo Customer 1: Near Silver threshold (480 / 500 points)
            Customer aarav = customerRepository.save(Customer.builder()
                    .name("Aarav Sharma")
                    .email("aarav.sharma@example.com")
                    .phone("+91 98765 43210")
                    .lifetimePoints(480L)
                    .availablePoints(480L)
                    .tier(baseTier)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .build());

            pointsLedgerRepository.save(PointsLedger.builder()
                    .customer(aarav)
                    .type(LedgerType.EARN)
                    .points(480L)
                    .balanceAfter(480L)
                    .description("Initial sign-up and retail purchases")
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .action("CUSTOMER_CREATED")
                    .entity("Customer")
                    .entityId(aarav.getId())
                    .details("Seeded demo customer near Silver threshold (480 pts)")
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .build());

            // Demo Customer 2: Silver tier member (1250 lifetime, 850 available)
            Customer priya = customerRepository.save(Customer.builder()
                    .name("Priya Patel")
                    .email("priya.patel@example.com")
                    .phone("+91 98123 45678")
                    .lifetimePoints(1250L)
                    .availablePoints(850L)
                    .tier(silverTier)
                    .createdAt(LocalDateTime.now().minusDays(25))
                    .build());

            pointsLedgerRepository.save(PointsLedger.builder()
                    .customer(priya)
                    .type(LedgerType.EARN)
                    .points(1250L)
                    .balanceAfter(1250L)
                    .description("Store opening promotion and purchases")
                    .createdAt(LocalDateTime.now().minusDays(25))
                    .build());

            pointsLedgerRepository.save(PointsLedger.builder()
                    .customer(priya)
                    .type(LedgerType.REDEEM)
                    .points(400L)
                    .balanceAfter(850L)
                    .description("Redeemed 400 points for Rs. 400 discount")
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .action("CUSTOMER_CREATED")
                    .entity("Customer")
                    .entityId(priya.getId())
                    .details("Seeded demo customer Silver tier (1250 lifetime pts)")
                    .createdAt(LocalDateTime.now().minusDays(25))
                    .build());

            // Demo Customer 3: Gold tier member (3400 lifetime, 1800 available)
            Customer vikram = customerRepository.save(Customer.builder()
                    .name("Vikram Malhotra")
                    .email("vikram.malhotra@example.com")
                    .phone("+91 97654 32109")
                    .lifetimePoints(3400L)
                    .availablePoints(1800L)
                    .tier(goldTier)
                    .createdAt(LocalDateTime.now().minusDays(40))
                    .build());

            pointsLedgerRepository.save(PointsLedger.builder()
                    .customer(vikram)
                    .type(LedgerType.EARN)
                    .points(3400L)
                    .balanceAfter(3400L)
                    .description("Seasonal shopping campaign")
                    .createdAt(LocalDateTime.now().minusDays(40))
                    .build());

            pointsLedgerRepository.save(PointsLedger.builder()
                    .customer(vikram)
                    .type(LedgerType.REDEEM)
                    .points(1600L)
                    .balanceAfter(1800L)
                    .description("Redeemed 1600 points on electronics purchase")
                    .createdAt(LocalDateTime.now().minusDays(12))
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .action("CUSTOMER_CREATED")
                    .entity("Customer")
                    .entityId(vikram.getId())
                    .details("Seeded demo customer Gold tier (3400 lifetime pts)")
                    .createdAt(LocalDateTime.now().minusDays(40))
                    .build());

            log.info("Demo customers seeded successfully.");
        }
    }
}
