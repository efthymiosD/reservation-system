package com.decoupledx.reservation.webui.admin;

import java.time.Clock;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;

/**
 * Assembles the admin reservations listing from module APIs: page query, field
 * names resolved per distinct resource, and customer display names from the
 * identity directory (fallback: internal reference prefix).
 */
@Component
class AdminReservationsFactory {

    private final ReservationApi reservationApi;
    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final CustomerDirectoryApi customerDirectory;
    private final Clock clock;

    AdminReservationsFactory(ReservationApi reservationApi, ResourceApi resourceService,
            VenueApi venueService, CustomerDirectoryApi customerDirectory, Clock clock) {
        this.reservationApi = reservationApi;
        this.resourceService = resourceService;
        this.venueService = venueService;
        this.customerDirectory = customerDirectory;
        this.clock = clock;
    }

    AdminReservationsModel build(ReservationStatus status, int page, int size) {
        ReservationPage result = reservationApi.listAllForAdmin(status, page, size);
        ZoneId zone = venueService.getVenue(venueService.singleVenueId()).timezone();
        java.time.Instant now = clock.instant();
        Map<UUID, String> fieldNames = new LinkedHashMap<>();
        Map<UUID, String> customerNames =
                customerDirectory.displayNames(customerIds(result));
        List<AdminReservationsModel.Row> rows = result.items().stream()
                .map(reservation -> toRow(reservation, zone, fieldNames, customerNames, now))
                .toList();
        return new AdminReservationsModel(status, rows, result.total(), result.page(), result.size());
    }

    private List<CustomerId> customerIds(ReservationPage result) {
        return result.items().stream()
                .map(ReservationInfo::customerId)
                .distinct()
                .toList();
    }

    private AdminReservationsModel.Row toRow(ReservationInfo reservation, ZoneId zone,
            Map<UUID, String> fieldNames, Map<UUID, String> customerNames, java.time.Instant now) {
        return new AdminReservationsModel.Row(
                reservation.id().value(),
                customerName(reservation, customerNames),
                reservation.customerId().value(),
                fieldName(reservation.resourceId(), fieldNames),
                reservation.start().atZone(zone).toLocalDate(),
                reservation.start().atZone(zone).toLocalTime(),
                reservation.end().atZone(zone).toLocalTime(),
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode(),
                displayStatus(reservation),
                reservation.isActive() && reservation.end().isAfter(now));
    }

    private String displayStatus(ReservationInfo reservation) {
        // Past-active slots have fully elapsed: show COMPLETED instead of the raw
        // ACTIVE status (same UX as the customer-facing page).
        if (reservation.isActive()
                && reservation.end().isBefore(java.time.Instant.now())) {
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
        String customerId = reservation.customerId().value();
        String storedName = null;
        try {
            storedName = customerNames.get(UUID.fromString(customerId));
        } catch (IllegalArgumentException notAnId) {
            // SQL-seeded rows may carry free-form ids: no display name exists.
        }
        return storedName;
    }
}
