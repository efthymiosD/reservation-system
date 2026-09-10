package com.decoupledx.reservation.reservation;

import com.decoupledx.reservation.policy.api.PolicyApi;
import com.decoupledx.reservation.pricing.api.PricingApi;
import com.decoupledx.reservation.reservation.domain.ReservationApiFacade;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.reservation.domain.service.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.api.VenueApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReservationModuleConfig {

    @Bean
    CreateReservationService createReservationService(ResourceApi resourceService, VenueApi venueService,
                                                      PolicyApi policyService, PricingApi pricingService, ReservationRepository reservations,
                                                      Clock clock, TransactionRunner tx) {
        return new CreateReservationService(resourceService, venueService, policyService, pricingService,
                reservations, clock, tx);
    }

    @Bean
    CancelReservationService cancelReservationService(ReservationRepository reservations,
                                                      ResourceApi resourceService, PolicyApi policyService, Clock clock, TransactionRunner tx) {
        return new CancelReservationService(reservations, resourceService, policyService, clock, tx);
    }

    @Bean
    ReservationQueryService reservationQueryService(ReservationRepository reservations) {
        return new ReservationQueryService(reservations);
    }

    @Bean
    ReservationApiFacade reservationFacade(
            CreateReservationService createReservationService, CancelReservationService cancelReservationService,
            ReservationQueryService queryService) {
        return new ReservationApiFacade(createReservationService, cancelReservationService, queryService);
    }
}
