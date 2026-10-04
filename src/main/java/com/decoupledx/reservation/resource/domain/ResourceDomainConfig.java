package com.decoupledx.reservation.resource.domain;

import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceService;
import com.decoupledx.reservation.shared.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class ResourceDomainConfig {

    @Bean
    ResourceService resourceService(ResourceRepository resources, ResourceGroupRepository groups,
                                    TransactionRunner tx) {
        return new ResourceServiceImpl(resources, groups, tx);
    }
}
