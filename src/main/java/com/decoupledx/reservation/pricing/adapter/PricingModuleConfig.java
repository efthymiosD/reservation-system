package com.decoupledx.reservation.pricing.adapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.pricing.domain.service.PricingService;
import com.decoupledx.reservation.shared.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class PricingModuleConfig {

    @Bean
    PricingService pricingService(PricingPolicyRepository pricingPolicies, TransactionRunner tx) {
        return new PricingService(pricingPolicies, tx);
    }
}
