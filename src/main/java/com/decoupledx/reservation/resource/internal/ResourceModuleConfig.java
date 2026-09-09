package com.decoupledx.reservation.resource.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.resource.domain.port.ResourceBlockRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class ResourceModuleConfig {

    @Bean
    ResourceService resourceService(ResourceRepository resources, ResourceGroupRepository groups,
            ResourceBlockRepository blocks, TransactionRunner tx) {
        return new ResourceService(resources, groups, blocks, tx);
    }
}
