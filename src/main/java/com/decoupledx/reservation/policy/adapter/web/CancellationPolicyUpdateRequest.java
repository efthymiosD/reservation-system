package com.decoupledx.reservation.policy.adapter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CancellationPolicyUpdateRequest(
        @NotNull @PositiveOrZero Integer deadlineBeforeStartMinutes) {
}
