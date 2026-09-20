package com.decoupledx.reservation.availability.adapter;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.availability.domain.service.AvailabilityService;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
class AvailabilityModuleConfig {

    @Bean
    AvailabilityService availabilityService(VenueApi venueService, ResourceApi resourceService,
            ReservationApi reservationQueries, PolicyApi policyService, PricingApi pricingService,
            Clock clock) {
        return new AvailabilityService(venueService, resourceService, reservationQueries, policyService,
                pricingService, clock);
    }
}
