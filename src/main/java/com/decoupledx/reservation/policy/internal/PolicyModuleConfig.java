package com.decoupledx.reservation.policy.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class PolicyModuleConfig {

    @Bean
    PolicyService policyService(BookingPolicyRepository bookingPolicies,
            CancellationPolicyRepository cancellationPolicies, TransactionRunner tx) {
        return new PolicyService(bookingPolicies, cancellationPolicies, tx);
    }
}
