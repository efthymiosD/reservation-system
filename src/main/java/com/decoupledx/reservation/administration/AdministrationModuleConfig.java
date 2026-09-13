package com.decoupledx.reservation.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.administration.api.AdministrationApi;
import com.decoupledx.reservation.administration.domain.RecurringReservationManagementFacade;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import com.decoupledx.reservation.administration.domain.service.RecurringReservationMaterializationService;
import com.decoupledx.reservation.administration.domain.service.RecurringReservationService;
import com.decoupledx.reservation.policy.api.PolicyApi;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.api.VenueApi;

import java.time.Clock;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class AdministrationModuleConfig {

    @Bean
    RecurringReservationMaterializationService recurringReservationMaterializationService(
            RecurringReservationRepository recurringReservations,
            ReservationApi reservationApi, VenueApi venueService,
            Clock clock, TransactionRunner tx) {
        return new RecurringReservationMaterializationService(
                recurringReservations, reservationApi, venueService, clock, tx);
    }

    @Bean
    RecurringReservationService recurringReservationService(
            RecurringReservationRepository recurringReservations, ResourceApi resourceService,
            VenueApi venueService, PolicyApi policyService, ReservationApi reservationApi,
            RecurringReservationMaterializationService materializer, Clock clock, TransactionRunner tx) {
        return new RecurringReservationService(
                recurringReservations, resourceService, venueService, policyService, reservationApi,
                materializer, clock, tx);
    }

    @Bean
    AdministrationApi recurringReservationManagementFacade(RecurringReservationService recurringReservationService,
            RecurringReservationMaterializationService materializationService) {
        return new RecurringReservationManagementFacade(recurringReservationService, materializationService);
    }
}