package com.decoupledx.reservation.venue.adapter.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.Map;

public record OpeningHoursUpdateRequest(
        @NotEmpty Map<DayOfWeek, @NotNull DailyOpeningHoursRequest> days) {
}
