package com.decoupledx.reservation.policy.adapter.api;

import com.decoupledx.reservation.policy.domain.port.PolicyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PolicyApiConfig {

    @Bean
    PolicyApi policyApi(PolicyService policyService) {
        return new PolicyApiImpl(policyService);
    }
}
