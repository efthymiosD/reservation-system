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
        VenueInfo venue = venueService.getVenue(venueService.singleVenueId());
        ZoneId zone = venue.timezone();
        Instant start = date.atTime(startTime).atZone(zone).toInstant();
        ReservationPeriod period = ReservationPeriod.ofStartAndDuration(start, Duration.ofMinutes(durationMinutes));

        BookingPolicy bookingPolicy = policyService.getBookingPolicy(venue.id());
        bookingPolicy.validateDuration(period.duration());
        if (!venue.openingHours().fits(period, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = venue.openingHours()
                .opensAt(date.getDayOfWeek())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        bookingPolicy.validateStartTime(start.atZone(zone), opensAt);
        bookingPolicy.validateAdvanceBooking(clock.instant(), start, zone);

        List<ResourceInfo> activeResources = resourceService.findActiveResources(venue.id());
        if (activeResources.isEmpty()) {
            return List.of();
        }
        List<ResourceId> resourceIds = activeResources.stream().map(ResourceInfo::id).toList();

        Set<ResourceId> blockedResourceIds = resourceService
                .findActiveBlocksOverlapping(resourceIds, period).stream()
                .map(ResourceBlockInfo::resourceId)
                .collect(Collectors.toSet());

        PricingPolicy pricingPolicy = pricingService.getPricingPolicy(venue.id());
        Money price = pricingPolicy.calculatePrice(period);

        return activeResources.stream()
                .filter(resource -> !blockedResourceIds.contains(resource.id()))
                .filter(resource -> isFree(resource.id(), period))
                .map(resource -> new AvailableResource(
                        resource.id().value(),
                        resource.name(),
                        resource.code(),
                        resource.type().name(),
                        price.amount(),
                        price.currency().getCurrencyCode()))
                .toList();
    }

    private boolean isFree(ResourceId resourceId, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingResource(resourceId, period).isEmpty();
    }
}
