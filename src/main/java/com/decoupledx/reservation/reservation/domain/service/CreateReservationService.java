package com.decoupledx.reservation.reservation.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import com.decoupledx.reservation.reservation.domain.model.CreateReservationCommand;
import com.decoupledx.reservation.reservation.domain.model.ReservationInfo;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.policy.domain.model.BookingPolicy;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.pricing.domain.service.PricingService;
import com.decoupledx.reservation.reservation.domain.model.Reservation;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceInfo;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.domain.model.OpeningHours;
import com.decoupledx.reservation.venue.domain.model.VenueInfo;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateReservationService {

    private final ResourceService resourceService;
    private final VenueService venueService;
    private final PolicyService policyService;
    private final PricingService pricingService;
    private final ReservationRepository reservations;
    private final Clock clock;
    private final TransactionRunner tx;

    public ReservationInfo create(CreateReservationCommand command, CustomerId customerId) {
        return tx.run(() -> doCreate(command, customerId));
    }

    private ReservationInfo doCreate(CreateReservationCommand command, CustomerId customerId) {
        ResourceInfo resource = resourceService.lockResource(command.resourceId());
        if (!resource.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_INACTIVE);
        }

        VenueInfo venue = venueService.getVenue(resource.venueId());
        Instant now = clock.instant();
        ReservationPeriod period = ReservationPeriod.of(command.start(), command.end());

        validatePeriod(command, period, venue, now);
        requireNoBlockConflict(command.resourceId(), period);
        requireNoCustomerOverlap(customerId, period);

        Money price = pricingService.getPricingPolicy(resource.venueId()).calculatePrice(period);
        Reservation reservation = Reservation.create(command.resourceId(), customerId, period, price, now);
        ReservationInfo saved = toInfo(reservations.save(reservation));
        log.info("Reservation created id={} resourceId={} customerId={} price={}",
                saved.id(), saved.resourceId(), saved.customerId(), saved.price());
        return saved;
    }

    private void validatePeriod(CreateReservationCommand command, ReservationPeriod period,
                                VenueInfo venue, Instant now) {
        ZoneId zone = venue.timezone();
        BookingPolicy bookingPolicy = policyService.getBookingPolicy(venue.id());
        bookingPolicy.validateDuration(period.duration());

        OpeningHours openingHours = venue.openingHours();
        if (!openingHours.fits(period, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = openingHours
                .opensAt(command.start().atZone(zone).getDayOfWeek())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        bookingPolicy.validateStartTime(command.start().atZone(zone), opensAt);
        bookingPolicy.validateAdvanceBooking(now, command.start(), zone);
    }

    private void requireNoBlockConflict(ResourceId resourceId, ReservationPeriod period) {
        boolean blocked = !resourceService
                .findActiveBlocksOverlapping(List.of(resourceId), period)
                .isEmpty();
        if (blocked) {
            throw new BusinessException(ErrorCode.RESOURCE_NO_LONGER_AVAILABLE);
        }
    }

    private void requireNoCustomerOverlap(CustomerId customerId, ReservationPeriod period) {
        if (reservations.existsActiveOverlappingCustomer(customerId, period)) {
            throw new BusinessException(ErrorCode.CUSTOMER_HAS_OVERLAPPING_RESERVATION);
        }
    }

    static ReservationInfo toInfo(Reservation reservation) {
        return new ReservationInfo(
                reservation.getId(),
                reservation.getResourceId(),
                reservation.getCustomerId(),
                reservation.getPeriod().start(),
                reservation.getPeriod().end(),
                reservation.getStatus(),
                reservation.getPrice(),
                reservation.getCreatedAt(),
                reservation.getCancelledAt());
    }
}
