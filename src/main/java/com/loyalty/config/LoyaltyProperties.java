package com.loyalty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "loyalty")
@Getter
@Setter
public class LoyaltyProperties {
    private BigDecimal pointsPerRupee = new BigDecimal("0.1");
    private BigDecimal rupeesPerPoint = new BigDecimal("1.0");
}
