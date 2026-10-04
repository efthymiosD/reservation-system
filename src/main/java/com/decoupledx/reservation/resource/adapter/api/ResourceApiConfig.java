package com.decoupledx.reservation.resource.adapter.api;

import com.decoupledx.reservation.resource.domain.port.ResourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ResourceApiConfig {

    @Bean
    ResourceApi resourceApi(ResourceService resourceService) {
        return new ResourceApiImpl(resourceService);
    }
}
