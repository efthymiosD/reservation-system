package com.decoupledx.reservation.availability.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.decoupledx.reservation.availability.domain.model.AvailableResource;
import com.decoupledx.reservation.availability.domain.model.ResourceAvailability;
import com.decoupledx.reservation.availability.domain.model.ResourceAvailabilityStatus;
import com.decoupledx.reservation.policy.domain.model.BookingPolicy;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.pricing.domain.model.PricingPolicy;
import com.decoupledx.reservation.pricing.domain.service.PricingService;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockInfo;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceInfo;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.venue.domain.model.VenueInfo;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AvailabilityService {

    private final VenueService venueService;
    private final ResourceService resourceService;
    private final ReservationQueryService reservationQueries;
    private final PolicyService policyService;
    private final PricingService pricingService;
    private final Clock clock;

    public List<AvailableResource> findAvailable(LocalDate date, LocalTime startTime, int durationMinutes) {
        Slot slot = validatedSlot(date, startTime, durationMinutes);
        List<ResourceInfo> activeResources = activeResources(slot.venue());
        if (activeResources.isEmpty()) {
            return List.of();
        }
        Set<ResourceId> blockedIds = blockedResourceIds(activeResources, slot.period());
        return activeResources.stream()
                .filter(resource -> !blockedIds.contains(resource.id()))
                .filter(resource -> isFree(resource.id(), slot.period()))
                .map(resource -> toAvailableResource(resource, slot.price()))
                .toList();
    }

    /**
     * View-oriented availability of every active resource for one requested slot:
     * each resource is AVAILABLE, RESERVED (overlapping active reservation) or
     * BLOCKED (overlapping active resource block). Includes the backend-computed
     * slot price so clients never calculate prices themselves.
     */
    public List<ResourceAvailability> findResourceAvailability(LocalDate date, LocalTime startTime, int durationMinutes) {
        Slot slot = validatedSlot(date, startTime, durationMinutes);
        List<ResourceInfo> activeResources = activeResources(slot.venue());
        if (activeResources.isEmpty()) {
            return List.of();
        }
        Set<ResourceId> blockedIds = blockedResourceIds(activeResources, slot.period());
        return activeResources.stream()
                .map(resource -> toResourceAvailability(resource, slot, blockedIds))
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

    private ResourceAvailability toResourceAvailability(ResourceInfo resource, Slot slot, Set<ResourceId> blockedIds) {
        return new ResourceAvailability(
                resource.id().value(),
                resource.name(),
                resource.code(),
                resource.type().name(),
                statusOf(resource.id(), slot.period(), blockedIds),
                slot.price().amount(),
                slot.price().currency().getCurrencyCode());
    }

    private ResourceAvailabilityStatus statusOf(ResourceId resourceId, ReservationPeriod period,
            Set<ResourceId> blockedIds) {
        if (blockedIds.contains(resourceId)) {
            return ResourceAvailabilityStatus.BLOCKED;
        }
        if (isFree(resourceId, period)) {
            return ResourceAvailabilityStatus.AVAILABLE;
        }
        return ResourceAvailabilityStatus.RESERVED;
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
        BookingPolicy bookingPolicy = policyService.getBookingPolicy(venue.id());
        bookingPolicy.validateDuration(period.duration());
        if (!venue.openingHours().fits(period, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = venue.openingHours()
                .opensAt(date.getDayOfWeek())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        bookingPolicy.validateStartTime(period.start().atZone(zone), opensAt);
        bookingPolicy.validateAdvanceBooking(clock.instant(), period.start(), zone);
    }

    private Money priceFor(VenueId venueId, ReservationPeriod period) {
        PricingPolicy pricingPolicy = pricingService.getPricingPolicy(venueId);
        return pricingPolicy.calculatePrice(period);
    }

    private List<ResourceInfo> activeResources(VenueInfo venue) {
        return resourceService.findActiveResources(venue.id());
    }

    private Set<ResourceId> blockedResourceIds(List<ResourceInfo> resources, ReservationPeriod period) {
        List<ResourceId> resourceIds = resources.stream().map(ResourceInfo::id).toList();
        return resourceService.findActiveBlocksOverlapping(resourceIds, period).stream()
                .map(ResourceBlockInfo::resourceId)
                .collect(Collectors.toSet());
    }

    private boolean isFree(ResourceId resourceId, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingResource(resourceId, period).isEmpty();
    }

    private record Slot(VenueInfo venue, ReservationPeriod period, Money price) {
    }
}
