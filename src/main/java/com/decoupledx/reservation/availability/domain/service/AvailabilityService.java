package com.decoupledx.reservation.availability.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.decoupledx.reservation.availability.adapter.api.AvailabilityApi;
import com.decoupledx.reservation.availability.adapter.api.AvailableResource;
import com.decoupledx.reservation.availability.adapter.api.ResourceAvailability;
import com.decoupledx.reservation.availability.adapter.api.ResourceAvailabilityStatus;
import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingPolicy;
import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AvailabilityService implements AvailabilityApi {

    private final VenueApi venueService;
    private final ResourceApi resourceService;
    private final ReservationApi reservationQueries;
    private final PolicyApi policyService;
    private final PricingApi pricingService;
    private final Clock clock;

    @Override
    public List<AvailableResource> findAvailable(LocalDate date, LocalTime startTime, int durationMinutes) {
        Slot slot = validatedSlot(date, startTime, durationMinutes);
        List<ResourceInfo> activeResources = activeResources(slot.venue());
        if (activeResources.isEmpty()) {
            return List.of();
        }
        return activeResources.stream()
                .filter(resource -> isFree(resource.id(), slot.period()))
                .map(resource -> toAvailableResource(resource, slot.price()))
                .toList();
    }

    /**
     * View-oriented availability of every active resource for one requested slot:
     * each resource is AVAILABLE or RESERVED (overlapping active reservation).
     * Includes the backend-computed slot price so clients never calculate prices
     * themselves.
     */
    @Override
    public List<ResourceAvailability> resourceAvailability(LocalDate date, LocalTime startTime, int durationMinutes) {
        Slot slot = validatedSlot(date, startTime, durationMinutes);
        List<ResourceInfo> activeResources = activeResources(slot.venue());
        if (activeResources.isEmpty()) {
            return List.of();
        }
        return activeResources.stream()
                .map(resource -> toResourceAvailability(resource, slot))
                .toList();
    }

    private AvailableResource toAvailableResource(ResourceInfo resource, Money price) {
        return new AvailableResource(
                resource.id().value(),
                resource.name(),
                resource.code(),
                resource.type().name(),
                price.amount(),
                price.currency().getCurrencyCode());
    }

    private ResourceAvailability toResourceAvailability(ResourceInfo resource, Slot slot) {
        return new ResourceAvailability(
                resource.id().value(),
                resource.name(),
                resource.code(),
                resource.type().name(),
                statusOf(resource.id(), slot.period()),
                slot.price().amount(),
                slot.price().currency().getCurrencyCode());
    }

    private ResourceAvailabilityStatus statusOf(ResourceId resourceId, ReservationPeriod period) {
        return isFree(resourceId, period)
                ? ResourceAvailabilityStatus.AVAILABLE
                : ResourceAvailabilityStatus.RESERVED;
    }

    private Slot validatedSlot(LocalDate date, LocalTime startTime, int durationMinutes) {
        VenueInfo venue = venueService.getVenue(venueService.singleVenueId());
        ReservationPeriod period = requestedPeriod(date, startTime, venue.timezone(), durationMinutes);
        validateSlotRequest(venue, date, period);
        return new Slot(venue, period, priceFor(venue.id(), period));
    }

    private ReservationPeriod requestedPeriod(LocalDate date, LocalTime startTime, ZoneId zone, int durationMinutes) {
        Instant start = date.atTime(startTime).atZone(zone).toInstant();
        return ReservationPeriod.ofStartAndDuration(start, Duration.ofMinutes(durationMinutes));
    }

    private void validateSlotRequest(VenueInfo venue, LocalDate date, ReservationPeriod period) {
        ZoneId zone = venue.timezone();
        BookingPolicy bookingPolicy = policyService.bookingPolicyFor(venue.id().value());
        bookingPolicy.validateDuration(period.duration());
        if (!venue.openingHours().fits(period, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = venue.openingHours()
                .opensAt(date.getDayOfWeek())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        bookingPolicy.validateStartTime(period.start().atZone(zone), opensAt);
        bookingPolicy.validateNotInPast(clock.instant(), period.start());
        bookingPolicy.validateAdvanceBooking(clock.instant(), period.start(), zone);
    }

    private Money priceFor(VenueId venueId, ReservationPeriod period) {
        PricingPolicy pricingPolicy = pricingService.pricingPolicyFor(venueId.value());
        return pricingPolicy.calculatePrice(period);
    }

    private List<ResourceInfo> activeResources(VenueInfo venue) {
        return resourceService.findActiveResources(venue.id().value());
    }

    private boolean isFree(ResourceId resourceId, ReservationPeriod period) {
        return reservationQueries.isSlotFree(resourceId.value(), period);
    }

    private record Slot(VenueInfo venue, ReservationPeriod period, Money price) {
    }
}
