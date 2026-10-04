package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.shared.*;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class CreateReservationServiceImpl implements CreateReservationService {

    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final PolicyApi policyService;
    private final PricingApi pricingService;
    private final ReservationRepository reservations;
    private final CurrentCustomerApi currentCustomers;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes) {
        return create(resourceId, startTime, durationMinutes, currentCustomers.currentCustomerId(), null);
    }

    @Override
    public ReservationInfo create(UUID resourceId, LocalDateTime startTime,
                                  int durationMinutes, CustomerId customerId) {
        return create(resourceId, startTime, durationMinutes, customerId, null);
    }

    @Override
    public void createForRecurringReservation(UUID resourceId, LocalDateTime startTime,
                                              int durationMinutes, CustomerId customerId, UUID recurringReservationId) {
        create(resourceId, startTime, durationMinutes, customerId, recurringReservationId);
    }

    private ReservationInfo create(UUID resourceId, LocalDateTime startTime,
                                   int durationMinutes, CustomerId customerId, UUID recurringReservationId) {
        Instant start = startTime.atZone(venueZone()).toInstant();
        Instant end = start.plus(Duration.ofMinutes(durationMinutes));
        return create(new CreateReservationCommand(ResourceId.of(resourceId), start, end), customerId, recurringReservationId);
    }

    private ZoneId venueZone() {
        return venueService.getVenue(venueService.singleVenueId()).timezone();
    }

    private ReservationInfo create(CreateReservationCommand command, CustomerId customerId,
                                   UUID recurringReservationId) {
        return tx.run(() -> doCreate(command, customerId, recurringReservationId));
    }

    private ReservationInfo doCreate(CreateReservationCommand command, CustomerId customerId,
                                     UUID recurringReservationId) {
        ResourceInfo resource = resourceService.lockResource(command.resourceId().value());
        if (!resource.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_INACTIVE);
        }

        VenueInfo venue = venueService.getVenue(resource.venueId().value());
        Instant now = clock.instant();
        ReservationPeriod period = ReservationPeriod.of(command.start(), command.end());

        validatePeriod(command, period, venue, now, recurringReservationId != null);
        requireNoCustomerOverlap(customerId, period);

        Money price = pricingService.pricingPolicyFor(resource.venueId().value()).calculatePrice(period);
        Reservation reservation = Reservation.create(command.resourceId(), customerId, period, price, now, recurringReservationId);
        ReservationInfo saved = ReservationMappings.toInfo(reservations.save(reservation.toDataValue()));
        log.info("Reservation created id={} resourceId={} customerId={} price={} recurringReservationId={}",
                saved.id(), saved.resourceId(), saved.customerId(), saved.price(), saved.recurringReservationId());
        return saved;
    }

    private void validatePeriod(CreateReservationCommand command, ReservationPeriod period,
                                VenueInfo venue, Instant now, boolean forRecurringReservation) {
        ZoneId zone = venue.timezone();
        BookingPolicy bookingPolicy = policyService.bookingPolicyFor(venue.id().value());
        bookingPolicy.validateDuration(period.duration());

        OpeningHours openingHours = venue.openingHours();
        if (!openingHours.fits(period, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = openingHours
                .opensAt(command.start().atZone(zone).getDayOfWeek())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        bookingPolicy.validateStartTime(command.start().atZone(zone), opensAt);
        bookingPolicy.validateNotInPast(now, command.start());
        if (!forRecurringReservation) {
            // Recurring-reservation-created bookings are governed by the recurring reservation's own
            // booking window (1/3/6 months), not the venue's public advance cap.
            bookingPolicy.validateAdvanceBooking(now, command.start(), zone);
        }
    }

    private void requireNoCustomerOverlap(CustomerId customerId, ReservationPeriod period) {
        if (reservations.existsActiveOverlappingCustomer(customerId, period)) {
            throw new BusinessException(ErrorCode.CUSTOMER_HAS_OVERLAPPING_RESERVATION);
        }
    }
}
