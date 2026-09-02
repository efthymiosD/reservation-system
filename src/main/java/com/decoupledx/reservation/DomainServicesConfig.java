package com.decoupledx.reservation;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.administration.domain.service.BlockResourceService;
import com.decoupledx.reservation.administration.domain.service.OverrideResourceBlockService;
import com.decoupledx.reservation.availability.domain.service.AvailabilityService;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;
import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.pricing.domain.service.PricingService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.reservation.domain.service.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.resource.domain.port.ResourceBlockRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;
import com.decoupledx.reservation.venue.domain.service.VenueService;

@Configuration
class DomainServicesConfig {

    @Bean
    VenueService venueService(VenueRepository venueRepository, TransactionRunner tx) {
        return new VenueService(venueRepository, tx);
    }

    @Bean
    ResourceService resourceService(ResourceRepository resources, ResourceGroupRepository groups,
                                    ResourceBlockRepository blocks, TransactionRunner tx) {
        return new ResourceService(resources, groups, blocks, tx);
    }

    @Bean
    PolicyService policyService(BookingPolicyRepository bookingPolicies,
                                CancellationPolicyRepository cancellationPolicies,
                                TransactionRunner tx) {
        return new PolicyService(bookingPolicies, cancellationPolicies, tx);
    }

    @Bean
    PricingService pricingService(PricingPolicyRepository pricingPolicies, TransactionRunner tx) {
        return new PricingService(pricingPolicies, tx);
    }

    @Bean
    ReservationQueryService reservationQueryService(ReservationRepository reservations) {
        return new ReservationQueryService(reservations);
    }

    @Bean
    CreateReservationService createReservationService(ResourceService resourceService,
                                                       VenueService venueService,
                                                       PolicyService policyService,
                                                       PricingService pricingService,
                                                       ReservationRepository reservations,
                                                       Clock clock, TransactionRunner tx) {
        return new CreateReservationService(resourceService, venueService, policyService,
                pricingService, reservations, clock, tx);
    }

    @Bean
    CancelReservationService cancelReservationService(ReservationRepository reservations,
                                                       ResourceService resourceService,
                                                       PolicyService policyService,
                                                       Clock clock, TransactionRunner tx) {
        return new CancelReservationService(reservations, resourceService, policyService, clock, tx);
    }

    @Bean
    AvailabilityService availabilityService(VenueService venueService,
                                             ResourceService resourceService,
                                             ReservationQueryService reservationQueries,
                                             PolicyService policyService,
                                             PricingService pricingService,
                                             Clock clock) {
        return new AvailabilityService(venueService, resourceService, reservationQueries,
                policyService, pricingService, clock);
    }

    @Bean
    BlockResourceService blockResourceService(ResourceService resourceService,
                                               ReservationQueryService reservationQueries,
                                               TransactionRunner tx) {
        return new BlockResourceService(resourceService, reservationQueries, tx);
    }

    @Bean
    OverrideResourceBlockService overrideResourceBlockService(ResourceService resourceService,
                                                               ReservationQueryService reservationQueries,
                                                               CancelReservationService cancelReservation,
                                                               TransactionRunner tx) {
        return new OverrideResourceBlockService(resourceService, reservationQueries, cancelReservation, tx);
    }

    @Bean
    CustomerAccountService customerAccountService(CustomerAccountRepository customerAccounts,
                                                   TransactionRunner tx) {
        return new CustomerAccountService(customerAccounts, tx);
    }
}
