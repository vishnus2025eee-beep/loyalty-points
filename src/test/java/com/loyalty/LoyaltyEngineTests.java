package com.loyalty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.dto.CustomerRequestDto;
import com.loyalty.dto.PurchaseRequestDto;
import com.loyalty.entity.Customer;
import com.loyalty.entity.LedgerType;
import com.loyalty.entity.PointsLedger;
import com.loyalty.entity.Tier;
import com.loyalty.repository.CustomerRepository;
import com.loyalty.repository.PointsLedgerRepository;
import com.loyalty.repository.TierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoyaltyEngineTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TierRepository tierRepository;

    @Autowired
    private PointsLedgerRepository pointsLedgerRepository;

    @Test
    @DisplayName("Tiers are correctly seeded on startup")
    void testTiersSeeded() throws Exception {
        mockMvc.perform(get("/api/tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].name", is("Base")))
                .andExpect(jsonPath("$[0].minLifetimePoints", is(0)))
                .andExpect(jsonPath("$[1].name", is("Silver")))
                .andExpect(jsonPath("$[1].minLifetimePoints", is(500)))
                .andExpect(jsonPath("$[2].name", is("Gold")))
                .andExpect(jsonPath("$[2].minLifetimePoints", is(2000)))
                .andExpect(jsonPath("$[3].name", is("Platinum")))
                .andExpect(jsonPath("$[3].minLifetimePoints", is(5000)));
    }

    @Test
    @DisplayName("Create customer returns 201 with Base tier")
    void testCreateCustomerSuccess() throws Exception {
        CustomerRequestDto req = new CustomerRequestDto("Rohan Gupta", "rohan.gupta@test.com", "+91 99999 88888");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Rohan Gupta")))
                .andExpect(jsonPath("$.email", is("rohan.gupta@test.com")))
                .andExpect(jsonPath("$.availablePoints", is(0)))
                .andExpect(jsonPath("$.lifetimePoints", is(0)))
                .andExpect(jsonPath("$.tier.name", is("Base")));
    }

    @Test
    @DisplayName("Duplicate email returns 409 Conflict")
    void testDuplicateEmailReturns409() throws Exception {
        CustomerRequestDto req1 = new CustomerRequestDto("Sameer Sen", "duplicate@test.com", null);
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CustomerRequestDto req2 = new CustomerRequestDto("Another User", "duplicate@test.com", null);
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Missing customer returns 404 Not Found")
    void testMissingCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/customers/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Customer not found")));
    }

    @Test
    @DisplayName("Normal purchase earns points and updates ledger")
    void testNormalPurchase() throws Exception {
        Tier baseTier = tierRepository.findByName("Base").orElseThrow();
        Customer customer = customerRepository.save(Customer.builder()
                .name("Karan Verma")
                .email("karan@test.com")
                .lifetimePoints(0L)
                .availablePoints(0L)
                .tier(baseTier)
                .build());

        // Purchase Rs. 1000 -> 0% Base discount -> net 1000 -> earns 100 pts
        PurchaseRequestDto purchaseReq = new PurchaseRequestDto(new BigDecimal("1000.00"), 0L);

        mockMvc.perform(post("/api/customers/" + customer.getId() + "/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grossAmount", is(1000.0)))
                .andExpect(jsonPath("$.tierDiscount", is(0.0)))
                .andExpect(jsonPath("$.pointsRedeemed", is(0)))
                .andExpect(jsonPath("$.netAmount", is(1000.0)))
                .andExpect(jsonPath("$.pointsEarned", is(100)))
                .andExpect(jsonPath("$.availablePointsAfter", is(100)))
                .andExpect(jsonPath("$.lifetimePointsAfter", is(100)))
                .andExpect(jsonPath("$.oldTier", is("Base")))
                .andExpect(jsonPath("$.newTier", is("Base")))
                .andExpect(jsonPath("$.upgraded", is(false)));

        List<PointsLedger> ledger = pointsLedgerRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        assertEquals(1, ledger.size());
        assertEquals(LedgerType.EARN, ledger.get(0).getType());
        assertEquals(100L, ledger.get(0).getPoints());
        assertEquals(100L, ledger.get(0).getBalanceAfter());
    }

    @Test
    @DisplayName("Tier auto-upgrades when threshold crossed")
    void testTierAutoUpgrade() throws Exception {
        Tier baseTier = tierRepository.findByName("Base").orElseThrow();
        // Aarav has 480 lifetime points, near Silver (500)
        Customer customer = customerRepository.save(Customer.builder()
                .name("Near Silver User")
                .email("nearsilver@test.com")
                .lifetimePoints(480L)
                .availablePoints(480L)
                .tier(baseTier)
                .build());

        // Purchase Rs. 200 -> Base 0% discount -> net 200 -> earns floor(200 * 0.1) = 20 pts
        // 480 + 20 = 500 lifetime points -> threshold for Silver reached!
        PurchaseRequestDto purchaseReq = new PurchaseRequestDto(new BigDecimal("200.00"), 0L);

        mockMvc.perform(post("/api/customers/" + customer.getId() + "/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointsEarned", is(20)))
                .andExpect(jsonPath("$.lifetimePointsAfter", is(500)))
                .andExpect(jsonPath("$.oldTier", is("Base")))
                .andExpect(jsonPath("$.newTier", is("Silver")))
                .andExpect(jsonPath("$.upgraded", is(true)));
    }

    @Test
    @DisplayName("Over-redeem points rejected with 400 Bad Request")
    void testOverRedeemRejected() throws Exception {
        Tier baseTier = tierRepository.findByName("Base").orElseThrow();
        Customer customer = customerRepository.save(Customer.builder()
                .name("Balance User")
                .email("balance@test.com")
                .lifetimePoints(100L)
                .availablePoints(50L) // only 50 points available
                .tier(baseTier)
                .build());

        // Try to redeem 100 points
        PurchaseRequestDto purchaseReq = new PurchaseRequestDto(new BigDecimal("500.00"), 100L);

        mockMvc.perform(post("/api/customers/" + customer.getId() + "/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Insufficient points")));
    }

    @Test
    @DisplayName("Redemption exceeding payable net amount rejected with 400 Bad Request")
    void testRedemptionExceedingBillRejected() throws Exception {
        Tier baseTier = tierRepository.findByName("Base").orElseThrow();
        Customer customer = customerRepository.save(Customer.builder()
                .name("High Balance User")
                .email("highbalance@test.com")
                .lifetimePoints(2000L)
                .availablePoints(1000L)
                .tier(baseTier)
                .build());

        // Bill is Rs. 100, but user tries to redeem 200 points (Rs. 200 discount)
        PurchaseRequestDto purchaseReq = new PurchaseRequestDto(new BigDecimal("100.00"), 200L);

        mockMvc.perform(post("/api/customers/" + customer.getId() + "/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("exceeds payable amount")));
    }
}
