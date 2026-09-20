package com.decoupledx.reservation.availability.adapter.api;

import com.decoupledx.reservation.availability.domain.port.AvailabilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AvailabilityApiConfig {

    @Bean
    AvailabilityApi availabilityApi(AvailabilityService availabilityService) {
        return new AvailabilityApiImpl(availabilityService);
    }
}
