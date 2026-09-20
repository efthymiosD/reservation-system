package com.decoupledx.reservation.administration.adapter.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

record RecurringReservationResponse(
        UUID id,
        UUID resourceId,
        UUID customerId,
        String weekday,
        LocalTime startTime,
        LocalTime endTime,
        int windowMonths,
        String status,
        LocalDate nextOccurrence,
        Instant createdAt,
        Instant cancelledAt,
        UUID cancelledBy) {
}
