package com.decoupledx.reservation.reservation.adapter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID resourceId,
        @NotNull LocalDateTime startTime,
        @Positive int durationMinutes) {
}
