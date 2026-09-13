package com.decoupledx.reservation.webui.admin;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * View model of the admin recurring-reservations page: the recurring-reservation
 * rows plus the active resources and provisioned customers usable as picker
 * options, the predefined booking windows (1/3/6 months) and the same start-time
 * and duration choices as the customer reserve page.
 */
public record AdminRecurringReservationsModel(
        List<ResourceOption> resources,
        List<CustomerOption> customers,
        List<WindowOption> windows,
        List<LocalTime> startTimeOptions,
        List<Integer> durationOptions,
        List<RecurringReservationRow> reservations) {

    public record ResourceOption(UUID resourceId, String name) {
    }

    public record CustomerOption(UUID customerId, String displayName) {
    }

    public record WindowOption(int months, String label) {
    }

    /** Times are venue-local; the venue's timezone is not re-rendered per row. */
    public record RecurringReservationRow(
            UUID reference,
            String fieldName,
            String customerName,
            String weekday,
            String startTime,
            String endTime,
            String windowLabel,
            String status,
            boolean active) {
    }
}