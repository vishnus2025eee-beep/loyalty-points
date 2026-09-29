package com.loyalty.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI loyaltyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LoyaltyPoints Engine API")
                        .description("Tier-based retail loyalty engine REST API with real-time tier calculation and points ledger")
                        .version("1.0.0")
                        .contact(new Contact().name("Retail Loyalty Team").email("loyalty@example.com")));
    }
}
