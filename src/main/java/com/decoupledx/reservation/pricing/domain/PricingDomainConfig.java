package com.decoupledx.reservation.pricing.domain;

import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.pricing.domain.port.PricingService;
import com.decoupledx.reservation.shared.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class PricingDomainConfig {

    @Bean
    PricingService pricingService(PricingPolicyRepository pricingPolicies, TransactionRunner tx) {
        return new PricingServiceImpl(pricingPolicies, tx);
    }
}
