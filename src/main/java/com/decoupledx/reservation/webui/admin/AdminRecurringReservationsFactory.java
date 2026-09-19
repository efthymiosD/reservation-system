package com.decoupledx.reservation.webui.admin;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.administration.adapter.api.AdministrationApi;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerEntry;
import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

import lombok.RequiredArgsConstructor;

/**
 * Assembles the admin recurring-reservations page from module APIs: the
 * recurring-reservation listing and the active resources + provisioned customers
 * usable as picker options, plus the same start-time and duration choices as the
 * customer reserve page (booking-policy grid fitted into the venue's opening
 * hours).
 */
@Component
@RequiredArgsConstructor
class AdminRecurringReservationsFactory {

    private final AdministrationApi administrationApi;
    private final CustomerDirectoryApi customerDirectory;
    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final PolicyApi policyService;

    private static final java.util.List<Integer> WINDOW_MONTHS = java.util.List.of(1, 3, 6);

    AdminRecurringReservationsModel build() {
        List<ResourceInfo> resources = venueResources();
        List<CustomerEntry> customers = customerDirectory.findAll();
        Map<UUID, String> resourceNames = resources.stream()
                .collect(Collectors.toMap(resource -> resource.id().value(), ResourceInfo::name, (a, _) -> a));
        Map<UUID, String> customerNames = customers.stream()
                .collect(Collectors.toMap(CustomerEntry::customerId, this::displayName, (a, _) -> a));
        List<AdminRecurringReservationsModel.RecurringReservationRow> rows = administrationApi.findRecurringReservations().stream()
                .map(recurringReservation -> toRow(recurringReservation, resourceNames, customerNames))
                .toList();
        List<AdminRecurringReservationsModel.ResourceOption> resourceOptions = resources.stream()
                .map(resource -> new AdminRecurringReservationsModel.ResourceOption(
                        resource.id().value(), resource.name()))
                .toList();
        List<AdminRecurringReservationsModel.CustomerOption> customerOptions = customers.stream()
                .map(customer -> new AdminRecurringReservationsModel.CustomerOption(
                        customer.customerId(), displayName(customer)))
                .toList();
        List<AdminRecurringReservationsModel.WindowOption> windowOptions = WINDOW_MONTHS.stream()
                .map(months -> new AdminRecurringReservationsModel.WindowOption(months, windowLabel(months)))
                .toList();
        VenueInfo venue = venueService.getVenue(venueService.singleVenueId());
        BookingPolicy policy = policyService.bookingPolicyFor(venue.id().value());
        List<Integer> durationOptions = durationOptions(policy);
        List<LocalTime> startTimeOptions = startTimeOptions(venue, policy.minDuration(), policy.startTimeStep());
        return new AdminRecurringReservationsModel(resourceOptions, customerOptions, windowOptions,
                startTimeOptions, durationOptions, rows);
    }

    private AdminRecurringReservationsModel.RecurringReservationRow toRow(RecurringReservationInfo recurringReservation,
            Map<UUID, String> resourceNames, Map<UUID, String> customerNames) {
        return new AdminRecurringReservationsModel.RecurringReservationRow(
                recurringReservation.id(),
                resourceNames.getOrDefault(recurringReservation.resourceId(), "field " + shortId(recurringReservation.resourceId())),
                customerNames.getOrDefault(recurringReservation.customerId(), "customer " + shortId(recurringReservation.customerId())),
                weekdayLabel(recurringReservation.weekday()),
                recurringReservation.startTime().toString(),
                recurringReservation.endTime().toString(),
                windowLabel(recurringReservation.windowMonths()),
                recurringReservation.status().name(),
                recurringReservation.isActive());
    }

    /** Same start-time grid as the customer reserve page: opens→closes−minDuration, unioned across weekdays. */
    private List<LocalTime> startTimeOptions(VenueInfo venue, Duration minDuration, Duration startStep) {
        var options = new java.util.TreeSet<LocalTime>();
        for (DayOfWeek day : DayOfWeek.values()) {
            venue.openingHours().on(day).ifPresent(hours -> {
                for (LocalTime candidate = hours.opensAt();
                        !candidate.plus(minDuration).isAfter(hours.closesAt());
                        candidate = candidate.plus(startStep)) {
                    options.add(candidate);
                }
            });
        }
        return List.copyOf(options);
    }

    private List<Integer> durationOptions(BookingPolicy policy) {
        List<Integer> options = new ArrayList<>();
        for (Duration d = policy.minDuration(); d.compareTo(policy.maxDuration()) <= 0;
                d = d.plus(policy.durationStep())) {
            options.add((int) d.toMinutes());
        }
        return options;
    }

    private String windowLabel(int months) {
        return months == 1 ? "1 month" : months + " months";
    }

    private String weekdayLabel(DayOfWeek weekday) {
        return switch (weekday) {
            case MONDAY -> "Monday";
            case TUESDAY -> "Tuesday";
            case WEDNESDAY -> "Wednesday";
            case THURSDAY -> "Thursday";
            case FRIDAY -> "Friday";
            case SATURDAY -> "Saturday";
            case SUNDAY -> "Sunday";
        };
    }

    private String displayName(CustomerEntry customer) {
        return customer.displayName() == null || customer.displayName().isBlank()
                ? "customer " + shortId(customer.customerId())
                : customer.displayName();
    }

    private List<ResourceInfo> venueResources() {
        return resourceService.findActiveResources(venueService.singleVenueId());
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}