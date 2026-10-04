package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class ReservationDomainConfig {

    @Bean
    CreateReservationService createReservationService(ResourceApi resourceService, VenueApi venueService,
                                                      PolicyApi policyService, PricingApi pricingService,
                                                      ReservationRepository reservations, CurrentCustomerApi currentCustomers,
                                                      Clock clock, TransactionRunner tx) {
        return new CreateReservationServiceImpl(resourceService, venueService, policyService, pricingService,
                reservations, currentCustomers, clock, tx);
    }

    @Bean
    CancelReservationService cancelReservationService(ReservationRepository reservations,
                                                      ResourceApi resourceService, PolicyApi policyService,
                                                      CurrentCustomerApi currentCustomers,
                                                      Clock clock, TransactionRunner tx) {
        return new CancelReservationServiceImpl(reservations, currentCustomers, resourceService, policyService, clock, tx);
    }

    @Bean
    ReservationQueryService reservationQueryService(ReservationRepository reservations, CurrentCustomerApi currentCustomers) {
        return new ReservationQueryServiceImpl(reservations, currentCustomers);
    }
}
