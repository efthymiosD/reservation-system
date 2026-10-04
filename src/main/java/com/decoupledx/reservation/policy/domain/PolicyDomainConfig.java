package com.decoupledx.reservation.policy.domain;

import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.PolicyService;
import com.decoupledx.reservation.shared.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class PolicyDomainConfig {

    @Bean
    PolicyService policyService(BookingPolicyRepository bookingPolicies,
                                CancellationPolicyRepository cancellationPolicies, TransactionRunner tx) {
        return new PolicyServiceImpl(bookingPolicies, cancellationPolicies, tx);
    }
}
