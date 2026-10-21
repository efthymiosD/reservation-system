package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import java.time.Instant;
import java.util.List;

/**
 * Combined constraints of the administrative reservation listing. Every
 * component is optional; the given ones AND together so filters work in
 * conjunction. A date range matches a reservation when its START falls
 * inside the window: {@code startTimeFrom} inclusive,
 * {@code startTimeToExclusive} exclusive — callers expressing an inclusive
 * end day pass that day's start plus one day.
 */
public record AdminReservationFilter(
        ReservationStatus status,
        List<CustomerId> customers,
        Instant startTimeFrom,
        Instant startTimeToExclusive) {

    public AdminReservationFilter {
        if (customers == null) {
            customers = List.of();
        }
    }

    /** Only a status constraint (e.g. the dashboard's active count). */
    public static AdminReservationFilter byStatus(ReservationStatus status) {
        return new AdminReservationFilter(status, List.of(), null, null);
    }
}
