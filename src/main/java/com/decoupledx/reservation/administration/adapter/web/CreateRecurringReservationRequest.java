package com.decoupledx.reservation.administration.adapter.web;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

record CreateRecurringReservationRequest(
        @NotNull UUID resourceId,
        @NotNull UUID customerId,
        @NotNull String weekday,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Integer windowMonths) {
}
