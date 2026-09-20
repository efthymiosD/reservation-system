package com.decoupledx.reservation.administration.domain;

import com.decoupledx.reservation.administration.domain.port.RecurringReservationMaterializationService;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationService;
import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class AdministrationDomainConfig {

    @Bean
    RecurringReservationMaterializationService recurringReservationMaterializationService(
            RecurringReservationRepository recurringReservations,
            ReservationApi reservationApi, VenueApi venueService,
            Clock clock, TransactionRunner tx) {
        return new RecurringReservationMaterializationServiceImpl(
                recurringReservations, reservationApi, venueService, clock, tx);
    }

    @Bean
    RecurringReservationService recurringReservationService(
            RecurringReservationRepository recurringReservations, ResourceApi resourceService,
            VenueApi venueService, PolicyApi policyService, ReservationApi reservationApi,
            CurrentCustomerApi currentCustomer, RecurringReservationMaterializationService materializer,
            Clock clock, TransactionRunner tx) {
        return new RecurringReservationServiceImpl(
                recurringReservations, materializer, resourceService, venueService, policyService, reservationApi,
                currentCustomer, clock, tx);
    }

}