package com.decoupledx.reservation.administration.api;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Request to assign a customer a recurring weekly slot on a resource. Times are
 * venue-local wall-clock. {@code windowMonths} selects the predefined booking
 * window (1/3/6 months) the materializer books occurrences into. Validation
 * (active resource, opening-hours fit, half-hour grid, duration policy,
 * window) happens in the domain at creation time.
 */
public record CreateRecurringReservationCommand(
        UUID resourceId,
        UUID customerId,
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        Integer windowMonths) {

    public CreateRecurringReservationCommand {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(weekday, "weekday must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        Objects.requireNonNull(windowMonths, "windowMonths must not be null");
    }
}