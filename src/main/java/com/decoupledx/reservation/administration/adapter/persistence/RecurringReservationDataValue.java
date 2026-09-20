package com.decoupledx.reservation.administration.adapter.persistence;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record RecurringReservationDataValue(
        UUID id,
        UUID resourceId,
        CustomerId customerId,
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        int windowMonths,
        RecurringReservationStatus status,
        LocalDate nextOccurrence,
        Instant createdAt,
        Instant cancelledAt,
        CustomerId cancelledBy) {

}

