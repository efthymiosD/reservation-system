package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationCreated;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.shared.*;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
class CreateReservationServiceImpl implements CreateReservationService {

    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final PricingApi pricingService;
    private final ReservationRepository reservations;
    private final CurrentCustomerApi currentCustomers;
    private final SlotPeriodRules slotPeriodRules;
    private final Clock clock;
    private final TransactionRunner tx;
    private final DomainEventPublisher events;

    @Override
    public ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes) {
        // An administrator booking for themselves (the public reserve page) books
        // like a venue-staff placement: admins may hold several bookings for
        // the same hour. Customers keep the strict rule.
        // admins may hold several bookings for the same hour (the pattern the
        // on-behalf use case supports). Customers keep the strict rule.
        boolean adminPlacement = currentCustomers.isAdministrator();
        CustomerId customer = currentCustomers.currentCustomerId();
        Instant start = startTime.atZone(venueZone()).toInstant();
        Instant end = start.plus(Duration.ofMinutes(durationMinutes));
        return tx.run(() -> doCreate(ResourceId.of(resourceId), start, end, customer, null, adminPlacement));
    }

    @Override
    public ReservationInfo createOnBehalfOfCustomer(UUID resourceId, LocalDateTime startTime,
                                                    int durationMinutes, CustomerId customerId) {
        Instant start = startTime.atZone(venueZone()).toInstant();
        Instant end = start.plus(Duration.ofMinutes(durationMinutes));
        return tx.run(() -> doCreate(ResourceId.of(resourceId), start, end, customerId, null, true));
    }

    @Override
    public void createForRecurringReservation(UUID resourceId, LocalDateTime startTime,
                                              int durationMinutes, CustomerId customerId, UUID recurringReservationId) {
        create(resourceId, startTime, durationMinutes, customerId, recurringReservationId);
    }

    private void create(UUID resourceId, LocalDateTime startTime,
                        int durationMinutes, CustomerId customerId, UUID recurringReservationId) {
        Instant start = startTime.atZone(venueZone()).toInstant();
        Instant end = start.plus(Duration.ofMinutes(durationMinutes));
        create(new CreateReservationCommand(ResourceId.of(resourceId), start, end), customerId, recurringReservationId);
    }

    private ZoneId venueZone() {
        return venueService.getVenue(venueService.singleVenueId()).timezone();
    }

    private void create(CreateReservationCommand command, CustomerId customerId,
                        UUID recurringReservationId) {
        tx.run(() -> doCreate(command.resourceId(), command.start(), command.end(),
                customerId, recurringReservationId, false));
    }

    private ReservationInfo doCreate(ResourceId resourceId, Instant start, Instant end,
                                     CustomerId customerId, UUID recurringReservationId,
                                     boolean adminOverride) {
        ResourceInfo resource = resourceService.lockResource(resourceId.value());
        if (!resource.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_INACTIVE);
        }

        VenueInfo venue = venueService.getVenue(resource.venueId().value());
        Instant now = clock.instant();
        ReservationPeriod period = ReservationPeriod.of(start, end);

        slotPeriodRules.check(start, period, venue, recurringReservationId != null);
        if (!adminOverride) {
            requireNoCustomerOverlap(customerId, period);
        }

        Money price = pricingService.pricingPolicyFor(resource.venueId().value()).calculatePrice(period);
        Reservation reservation = Reservation.create(resourceId, customerId, period, price, now,
                recurringReservationId, adminOverride);
        ReservationInfo saved = ReservationMappings.toInfo(reservations.save(reservation.toDataValue()));
        events.publish(new ReservationCreated(saved.id().value(), saved.resourceId().value(),
                customerId.value(), saved.start(), saved.end(),
                saved.price().amount(), saved.price().currency().getCurrencyCode(),
                adminOverride, saved.recurringReservationId()));
        log.info("Reservation created id={} resourceId={} customerId={} price={} recurringReservationId={}",
                saved.id(), saved.resourceId(), saved.customerId(), saved.price(), saved.recurringReservationId());
        return saved;
    }

    private void requireNoCustomerOverlap(CustomerId customerId, ReservationPeriod period) {
        if (reservations.existsActiveOverlappingCustomer(customerId, period)) {
            throw new BusinessException(ErrorCode.CUSTOMER_HAS_OVERLAPPING_RESERVATION);
        }
    }
}
