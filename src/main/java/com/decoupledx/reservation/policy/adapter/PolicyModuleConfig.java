package com.decoupledx.reservation.policy.adapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.shared.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
class PolicyModuleConfig {

    @Bean
    PolicyService policyService(BookingPolicyRepository bookingPolicies,
            CancellationPolicyRepository cancellationPolicies, TransactionRunner tx) {
        return new PolicyService(bookingPolicies, cancellationPolicies, tx);
    }
}
