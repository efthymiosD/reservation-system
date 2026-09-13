package com.decoupledx.reservation.webui.reserve;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.availability.api.ResourceAvailability;
import com.decoupledx.reservation.availability.api.ResourceAvailabilityStatus;
import com.decoupledx.reservation.availability.api.AvailabilityApi;
import com.decoupledx.reservation.identity.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.policy.api.BookingPolicy;
import com.decoupledx.reservation.policy.api.PolicyApi;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.resource.api.ResourceId;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.venue.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.api.VenueInfo;
import com.decoupledx.reservation.venue.api.VenueApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Assembles the reservation page view model from backend state: duration and
 * time options are shaped from the booking policy and opening hours, availability
 * and price come from the availability service, and the visual arrangement comes
 * from the venue layout configuration. The page itself is authenticated-only, so
 * the current customer is always resolvable. All booking rules stay in the
 * domain; the factory only shapes what the UI may offer and degrades gracefully
 * for unbookable requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ReservationPageModelFactory {

    private final PolicyApi policyService;
    private final AvailabilityApi availabilityService;
    private final VenueApi venueService;
    private final VenueLayoutLoader venueLayout;
    private final ReservationApi reservationQueries;
    private final ResourceApi resourceService;
    private final CurrentCustomerApi currentCustomer;
    private final Clock clock;

    ReservationPageModel build(LocalDate requestedDate, LocalTime requestedStart, Integer requestedDuration) {
        VenueInfo venue = venueService.getVenue(venueService.singleVenueId());
        ZoneId zone = venue.timezone();
        BookingPolicy policy = policyService.bookingPolicyFor(venue.id().value());

        LocalDate today = clock.instant().atZone(zone).toLocalDate();
        LocalDate maxDate = today.plus(policy.maxAdvanceBooking());
        List<Integer> durationOptions = durationOptions(policy);

        SlotSelection selection = resolveSelection(requestedDate, requestedStart, requestedDuration,
                today, maxDate, durationOptions, policy.startTimeStep(), venue);

        return selection == null
                ? unavailableModel(today, maxDate, durationOptions)
                : availabilityModel(selection, venue, zone, today, maxDate, durationOptions);
    }

    private record SlotSelection(LocalDate date, LocalTime start, int duration,
            List<LocalTime> timeOptions) {
    }

    private SlotSelection resolveSelection(LocalDate requestedDate, LocalTime requestedStart,
            Integer requestedDuration, LocalDate today, LocalDate maxDate,
            List<Integer> durationOptions, Duration startStep, VenueInfo venue) {

        LocalDate date = requestedDate == null || !validDate(requestedDate, today, maxDate).isPresent()
                ? today
                : requestedDate;
        int duration = requestedDuration == null || !validDuration(requestedDuration, durationOptions).isPresent()
                ? durationOptions.get(0)
                : requestedDuration;

        SlotSelection direct = forDate(date, requestedStart, duration, startStep, venue, zoneOf(venue));
        if (direct != null) {
            return direct;
        }
        // The day has no bookable start times (closed, or every start has passed or
        // does not fit the duration): silently fall forward to the next bookable day.
        for (LocalDate candidate = date.plusDays(1); !candidate.isAfter(maxDate); candidate = candidate.plusDays(1)) {
            SlotSelection next = forDate(candidate, null, duration, startStep, venue, zoneOf(venue));
            if (next != null) {
                return next;
            }
        }
        return null;
    }

    private ZoneId zoneOf(VenueInfo venue) {
        return venue.timezone();
    }

    /** Fits one concrete day; null when closed or when no start time fits. */
    private SlotSelection forDate(LocalDate date, LocalTime requestedStart, int duration,
            Duration startStep, VenueInfo venue, ZoneId zone) {
        DailyOpeningHours hours = venue.openingHours().on(date.getDayOfWeek()).orElse(null);
        if (hours == null) {
            return null;
        }
        List<LocalTime> timeOptions = timeOptions(hours, duration, date, zone, startStep);
        if (timeOptions.isEmpty()) {
            return null;
        }
        LocalTime start = requestedStart != null && timeOptions.contains(requestedStart)
                ? requestedStart
                : timeOptions.get(0);
        return new SlotSelection(date, start, duration, timeOptions);
    }

    private ReservationPageModel availabilityModel(SlotSelection selection, VenueInfo venue, ZoneId zone,
            LocalDate today, LocalDate maxDate, List<Integer> durationOptions) {

        try {
            List<ResourceAvailability> resources = availabilityService.resourceAvailability(
                    selection.date(), selection.start(), selection.duration());
            return new ReservationPageModel(
                    selection.date(),
                    selection.start(),
                    selection.duration(),
                    today,
                    maxDate,
                    durationOptions,
                    selection.timeOptions(),
                    priceOf(resources),
                    mapFields(resources),
                    heldReservation(selection, resources),
                    anyAvailable(resources));
        } catch (BusinessException exception) {
            log.info("Reservation page slot not bookable: {}", exception.getMessage());
            return new ReservationPageModel(
                    selection.date(),
                    selection.start(),
                    selection.duration(),
                    today,
                    maxDate,
                    durationOptions,
                    List.of(),
                    null,
                    List.of(),
                    null,
                    false);
        }
    }

    private ReservationPageModel unavailableModel(LocalDate today, LocalDate maxDate, List<Integer> durationOptions) {
        return new ReservationPageModel(
                today,
                null,
                durationOptions.get(0),
                today,
                maxDate,
                durationOptions,
                List.of(),
                null,
                List.of(),
                null,
                false);
    }

    /**
     * UX hint for the overlapping-own-reservation business rule: when the current
     * customer already holds an active reservation overlapping the selected slot,
     * the page says so before they try to reserve. Anonymous visitors skip the
     * check entirely.
     */
    private ReservationPageModel.HeldReservation heldReservation(SlotSelection selection,
            List<ResourceAvailability> resources) {
        CustomerId customer = currentCustomer.currentCustomerId();
        if (resources.isEmpty()) {
            return null;
        }
        ReservationPeriod period = ReservationPeriod.ofStartAndDuration(
                selection.date().atTime(selection.start()).atZone(zone()).toInstant(),
                Duration.ofMinutes(selection.duration()));
        return reservationQueries.findActiveOverlappingCustomer(customer, period).stream()
                .findFirst()
                .map(held -> new ReservationPageModel.HeldReservation(
                        held.id().value(),
                        fieldName(held.resourceId()),
                        held.start().atZone(zone()).toLocalTime(),
                        held.end().atZone(zone()).toLocalTime()))
                .orElse(null);
    }

    private ZoneId zone() {
        return venueService.getVenue(venueService.singleVenueId()).timezone();
    }

    private String fieldName(ResourceId resourceId) {
        try {
            return resourceService.getResource(resourceId.value()).name();
        } catch (BusinessException gone) {
            return "the selected field";
        }
    }

    private List<Integer> durationOptions(BookingPolicy policy) {
        List<Integer> options = new ArrayList<>();
        for (Duration d = policy.minDuration(); d.compareTo(policy.maxDuration()) <= 0;
                d = d.plus(policy.durationStep())) {
            options.add((int) d.toMinutes());
        }
        return options;
    }

    private List<LocalTime> timeOptions(DailyOpeningHours hours, int durationMinutes, LocalDate date,
            ZoneId zone, Duration startStep) {
        List<LocalTime> options = new ArrayList<>();
        LocalTime close = hours.closesAt();
        for (LocalTime candidate = hours.opensAt(); !candidate.plusMinutes(durationMinutes).isAfter(close);
                candidate = candidate.plus(startStep)) {
            if (isNotPast(candidate, date, zone)) {
                options.add(candidate);
            }
        }
        return options;
    }

    private boolean isNotPast(LocalTime candidate, LocalDate date, ZoneId zone) {
        return !date.atTime(candidate).atZone(zone).toInstant().isBefore(clock.instant());
    }

    private Optional<Integer> validDuration(Integer requested, List<Integer> options) {
        return requested != null && options.contains(requested)
                ? java.util.Optional.of(requested)
                : java.util.Optional.empty();
    }

    private Optional<LocalDate> validDate(LocalDate requested, LocalDate today, LocalDate maxDate) {
        return requested != null && !requested.isBefore(today) && !requested.isAfter(maxDate)
                ? java.util.Optional.of(requested)
                : java.util.Optional.empty();
    }

    private boolean anyAvailable(List<ResourceAvailability> resources) {
        return resources.stream()
                .anyMatch(resource -> resource.status() == ResourceAvailabilityStatus.AVAILABLE);
    }

    private ReservationPageModel.MoneyView priceOf(List<ResourceAvailability> resources) {
        return resources.isEmpty() ? null
                : new ReservationPageModel.MoneyView(
                        resources.get(0).priceAmount(), resources.get(0).priceCurrency());
    }

    private List<ReservationPageModel.MapField> mapFields(List<ResourceAvailability> resources) {
        VenueLayout layout = venueLayout.get();
        return resources.stream()
                .flatMap(resource -> placementOf(layout, resource).stream()
                        .map(placement -> mapField(resource, placement)))
                .toList();
    }

    private java.util.Optional<VenueLayout.Placement> placementOf(VenueLayout layout, ResourceAvailability resource) {
        return layout.placements().stream()
                .filter(placement -> placement.resourceId().equals(resource.resourceId()))
                .findFirst();
    }

    private ReservationPageModel.MapField mapField(ResourceAvailability resource, VenueLayout.Placement placement) {
        ResourceAvailabilityStatus status = resource.status();
        String statusClass = status.name().toLowerCase();
        String aria = switch (status) {
            case AVAILABLE -> resource.name() + " — available — select field";
            case RESERVED -> resource.name() + " — reserved — unavailable";
        };
        return new ReservationPageModel.MapField(
                resource.resourceId(),
                resource.name(),
                placement.x(),
                placement.y(),
                placement.width(),
                placement.height(),
                status.name(),
                statusClass,
                aria);
    }
}
