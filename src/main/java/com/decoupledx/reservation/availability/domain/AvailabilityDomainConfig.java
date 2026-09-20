package com.decoupledx.reservation.availability.domain;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.availability.domain.port.AvailabilityService;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;

/** Domain-internal wiring; nothing here is visible to other modules. */
@Configuration
class AvailabilityDomainConfig {

    @Bean
    AvailabilityService availabilityService(VenueApi venueService, ResourceApi resourceService,
            ReservationApi reservationQueries, PolicyApi policyService, PricingApi pricingService,
            Clock clock) {
        return new AvailabilityServiceImpl(venueService, resourceService, reservationQueries,
                policyService, pricingService, clock);
    }
}
