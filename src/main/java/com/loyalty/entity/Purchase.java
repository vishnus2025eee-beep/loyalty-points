package com.loyalty.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tierDiscount;

    @Column(nullable = false)
    @Builder.Default
    private Long pointsRedeemed = 0L;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal redeemDiscount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false)
    @Builder.Default
    private Long pointsEarned = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (pointsRedeemed == null) {
            pointsRedeemed = 0L;
        }
        if (pointsEarned == null) {
            pointsEarned = 0L;
        }
    }
}
