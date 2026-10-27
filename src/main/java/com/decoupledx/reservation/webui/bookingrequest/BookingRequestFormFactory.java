package com.decoupledx.reservation.webui.bookingrequest;

import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Advisory OPTIONS of the public booking-request form, shaped from the very
 * same module machinery the reserve page uses: policy-bounded dates and
 * durations, a weekly union of the opening-hours start grid, active
 * resources. ADVISORY ONLY — a displayed option is not a promise: the coach's
 * approval revalidates availability authoritatively, and the module validates
 * the exact submitted slot server-side.
 */
@Component
class BookingRequestFormFactory {

    private final VenueApi venueService;
    private final PolicyApi policyService;
    private final ResourceApi resourceService;
    private final Clock clock;

    BookingRequestFormFactory(VenueApi venueService, PolicyApi policyService,
                              ResourceApi resourceService, Clock clock) {
        this.venueService = venueService;
        this.policyService = policyService;
        this.resourceService = resourceService;
        this.clock = clock;
    }

    BookingRequestFormOptions options() {
        VenueInfo venue = venueService.getVenue(venueService.singleVenueId());
        ZoneId zone = venue.timezone();
        BookingPolicy policy = policyService.bookingPolicyFor(venue.id().value());

        LocalDate today = clock.instant().atZone(zone).toLocalDate();
        LocalDate maxDate = today.plus(policy.maxAdvanceBooking());
        List<Integer> durations = supportedDurations(policy);
        List<LocalTime> startOptions = weeklyUnionStartGrid(venue);
        List<ResourceOption> resources = activeResources();
        return new BookingRequestFormOptions(
                today.toString(), maxDate.toString(), durations, startOptions, resources);
    }

    private List<Integer> supportedDurations(BookingPolicy policy) {
        List<Integer> options = new ArrayList<>();
        for (Duration d = policy.minDuration(); d.compareTo(policy.maxDuration()) <= 0;
             d = d.plus(policy.durationStep())) {
            options.add((int) d.toMinutes());
        }
        return options;
    }

    /**
     * The union of all weekday opening-hours start candidates, in the start
     * step. Overnight weekday shapes (closes after midnight) contribute their
     * pre-midnight part only for this advisory union; the exact submitted
     * slot is validated per its own weekday (including overnight) module-side.
     */
    private List<LocalTime> weeklyUnionStartGrid(VenueInfo venue) {
        Duration startStep = policyService.bookingPolicyFor(venue.id().value()).startTimeStep();
        int step = (int) startStep.toMinutes();
        int dayMinutes = 24 * 60;
        TreeSet<LocalTime> union = new TreeSet<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            Optional<DailyOpeningHours> hours = venue.openingHours().on(day);
            if (hours.isEmpty()) {
                continue;
            }
            int opens = minutesOfDay(hours.get().opensAt());
            int closes = Math.min(minutesOfDay(hours.get().closesAt())
                    + (hours.get().overnight() ? dayMinutes : 0), dayMinutes);
            for (int start = opens; start + step <= closes; start += step) {
                union.add(LocalTime.of(start / 60, start % 60));
            }
        }
        return union.stream().toList();
    }

    private List<ResourceOption> activeResources() {
        return resourceService.findResources(venueService.singleVenueId()).stream()
                .filter(resource -> resource.isActive())
                .map(resource -> new ResourceOption(
                        resource.id().value(), resource.name()))
                .toList();
    }

    private int minutesOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
