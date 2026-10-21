package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.AdminReservationFilter;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Assembles the admin reservations listing from module APIs: a combined
 * filter (status × customer picker × capped venue-local date window, all in
 * conjunction), field names resolved per distinct resource, and customer
 * display names from the identity directory (fallback: internal reference
 * prefix).
 */
@Component
class AdminReservationsFactory {

    private final ReservationApi reservationApi;
    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final CustomerDirectoryApi customerDirectory;
    private final AdminFilterSettings filterSettings;
    private final Clock clock;

    AdminReservationsFactory(ReservationApi reservationApi, ResourceApi resourceService,
                             VenueApi venueService, CustomerDirectoryApi customerDirectory,
                             AdminFilterSettings filterSettings, Clock clock) {
        this.reservationApi = reservationApi;
        this.resourceService = resourceService;
        this.venueService = venueService;
        this.customerDirectory = customerDirectory;
        this.filterSettings = filterSettings;
        this.clock = clock;
    }

    AdminReservationsModel build(ReservationStatus status, String customer,
                                 LocalDate from, LocalDate to, int page, int size) {
        CustomerSelection selection = CustomerSelection.resolve(customerDirectory, customer);
        if (selection.isNoMatch()) {
            return explicitEmptyPage(status, selection, from, to, page, size);
        }
        VenueWindow window = VenueWindow.forRange(venueZone(), from, to,
                filterSettings.maxRangeDays());
        AdminReservationFilter filter = new AdminReservationFilter(
                status, selection.asFilterConstraint(),
                window.startInclusiveInstant(), window.endExclusiveInstant());

        ReservationPage result = reservationApi.listAllForAdmin(filter, page, size);
        Map<UUID, String> fieldNames = new LinkedHashMap<>();
        Map<UUID, String> customerNames =
                customerDirectory.displayNames(customerIds(result));
        List<Row> rows = result.items().stream()
                .map(reservation -> toRow(reservation, window.zone(), fieldNames, customerNames))
                .toList();
        return new AdminReservationsModel(status, selection.selectedName(), from, to,
                nameOptions(), window.clamped(), rows, result.total(),
                Math.max(page, 0), size);
    }

    private AdminReservationsModel explicitEmptyPage(ReservationStatus status, CustomerSelection selection,
                                                     LocalDate from, LocalDate to, int page, int size) {
        // The customer picker matched nothing: an explicitly empty table, not
        // a silently unfiltered one.
        return new AdminReservationsModel(status, selection.selectedName(), from, to,
                nameOptions(), false, List.of(), 0, Math.max(page, 0), size);
    }

    private ZoneId venueZone() {
        return venueService.getVenue(venueService.singleVenueId()).timezone();
    }

    /** Distinct stored display names of the directory — the picker options. */
    private List<String> nameOptions() {
        return customerDirectory.findAll().stream()
                .map(entry -> entry.displayName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<CustomerId> customerIds(ReservationPage result) {
        return result.items().stream()
                .map(ReservationInfo::customerId)
                .distinct()
                .toList();
    }

    private Row toRow(ReservationInfo reservation, ZoneId zone,
                      Map<UUID, String> fieldNames, Map<UUID, String> customerNames) {
        Instant now = clock.instant();
        return new Row(
                reservation.id().value(),
                customerName(reservation, customerNames),
                reservation.customerId().value(),
                fieldName(reservation.resourceId(), fieldNames),
                reservation.start().atZone(zone).toLocalDate(),
                reservation.start().atZone(zone).toLocalTime(),
                reservation.end().atZone(zone).toLocalTime(),
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode(),
                displayStatus(reservation, now),
                reservation.isActive() && reservation.end().isAfter(now));
    }

    private String displayStatus(ReservationInfo reservation, Instant now) {
        // Past-active slots have fully elapsed: show COMPLETED instead of the raw
        // ACTIVE status (same UX as the customer-facing page).
        if (reservation.isActive() && reservation.end().isBefore(now)) {
            return "COMPLETED";
        }
        return reservation.status().name();
    }

    private String fieldName(ResourceId resourceId,
                             Map<UUID, String> fieldNames) {
        return fieldNames.computeIfAbsent(resourceId.value(), id -> {
            try {
                return resourceService.getResource(id).name();
            } catch (BusinessException gone) {
                return "field " + id.toString().substring(0, 8);
            }
        });
    }

    private String customerName(ReservationInfo reservation, Map<UUID, String> customerNames) {
        try {
            String internalCustomerId = reservation.customerId().value();
            return customerNames.get(UUID.fromString(internalCustomerId));
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
    }
}
