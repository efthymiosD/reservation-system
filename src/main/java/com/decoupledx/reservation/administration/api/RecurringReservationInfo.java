package com.decoupledx.reservation.administration.api;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A recurring reservation as seen by admin-facing views: a resource assigned to
 * a customer on a weekly weekday at venue-local times, plus the materializer
 * cursor ({@code nextOccurrence}) and lifecycle audit fields.
 */
public record RecurringReservationInfo(
        UUID id,
        UUID resourceId,
        UUID customerId,
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        int windowMonths,
        RecurringReservationStatus status,
        LocalDate nextOccurrence,
        Instant createdAt,
        Instant cancelledAt,
        UUID cancelledBy) {

    public boolean isActive() {
        return status == RecurringReservationStatus.ACTIVE;
    }
}