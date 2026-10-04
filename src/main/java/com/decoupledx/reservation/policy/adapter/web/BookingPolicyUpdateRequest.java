package com.decoupledx.reservation.policy.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BookingPolicyUpdateRequest(
        @Positive int minDurationMinutes,
        @Positive int maxDurationMinutes,
        @Positive int durationStepMinutes,
        @Positive int startTimeStepMinutes,
        @NotBlank String maxAdvanceBooking) {
}
