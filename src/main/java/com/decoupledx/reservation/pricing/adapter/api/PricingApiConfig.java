package com.decoupledx.reservation.pricing.adapter.api;

import com.decoupledx.reservation.pricing.domain.port.PricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PricingApiConfig {

    @Bean
    PricingApi pricingApi(PricingService pricingService) {
        return new PricingApiImpl(pricingService);
    }
}
