package com.decoupledx.reservation.venue.adapter.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record DailyOpeningHoursRequest(
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt) {
}
