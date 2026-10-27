package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.domain.port.*;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.shared.DomainEventPublisher;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class ReservationDomainConfig {

    @Bean
    SlotPeriodRules slotPeriodRules(PolicyApi policyService, Clock clock) {
        return new SlotPeriodRules(policyService, clock);
    }

    @Bean
    CreateReservationService createReservationService(ResourceApi resourceService, VenueApi venueService, PricingApi pricingService,
                                                      ReservationRepository reservations, CurrentCustomerApi currentCustomers,
                                                      SlotPeriodRules slotPeriodRules,
                                                      Clock clock, TransactionRunner tx, DomainEventPublisher events) {
        return new CreateReservationServiceImpl(resourceService, venueService, pricingService,
                reservations, currentCustomers, slotPeriodRules, clock, tx, events);
    }

    @Bean
    CancelReservationService cancelReservationService(ReservationRepository reservations,
                                                      ResourceApi resourceService, PolicyApi policyService,
                                                      CurrentCustomerApi currentCustomers,
                                                      Clock clock, TransactionRunner tx,
                                                      DomainEventPublisher events) {
        return new CancelReservationServiceImpl(reservations, currentCustomers, resourceService, policyService,
                clock, tx, events);
    }

    @Bean
    ReservationQueryService reservationQueryService(ReservationRepository reservations, CurrentCustomerApi currentCustomers) {
        return new ReservationQueryServiceImpl(reservations, currentCustomers);
    }

    @Bean
    SubmitBookingRequestService submitBookingRequestService(BookingRequestRepository bookingRequests,
                                                            BookingRequestLinkRepository links,
                                                            ResourceApi resourceService, VenueApi venueService,
                                                            SlotPeriodRules slotPeriodRules,
                                                            ReservationQueryService reservationQueries,
                                                            Clock clock, TransactionRunner tx) {
        return new SubmitBookingRequestServiceImpl(bookingRequests, links, resourceService, venueService,
                slotPeriodRules, reservationQueries, clock, tx);
    }

    @Bean
    ManageBookingRequestsService manageBookingRequestsService(BookingRequestRepository bookingRequests,
                                                              CreateReservationService createReservation,
                                                              CurrentCustomerApi currentCustomers,
                                                              CustomerDirectoryApi customerDirectory,
                                                              Clock clock, TransactionRunner tx) {
        return new ManageBookingRequestsServiceImpl(bookingRequests, createReservation, currentCustomers,
                customerDirectory, clock, tx);
    }

    @Bean
    BookingRequestLinkService bookingRequestLinkService(BookingRequestLinkRepository links,
                                                        Clock clock, TransactionRunner tx) {
        return new BookingRequestLinkServiceImpl(links, clock, tx);
    }
}
